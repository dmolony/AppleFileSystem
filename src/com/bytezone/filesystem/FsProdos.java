package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// see https://prodos8.com/docs/techref/file-organization/
// see https://prodos8.com/docs/technote/25/
// -----------------------------------------------------------------------------------//
public class FsProdos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final int FIRST_CATALOG_BLOCK = 2;
  private static final int BITS_PER_BLOCK = 0x1000;

  private DirectoryHeaderProdos directoryHeader;
  private boolean isDosMaster;

  // ---------------------------------------------------------------------------------//
  FsProdos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.PRODOS);

    // Create the Volume Directory Header. This is the first entry in the
    // first block of the catalog (block 2).
    directoryHeader = new DirectoryHeaderProdos (this, FIRST_CATALOG_BLOCK);
    setTotalCatalogBlocks (directoryHeader.catalogBlocks.size ());

    // Create a FileProdos or FolderProdos for each catalog entry. Each one creates
    // its own CatalogEntryProdos. When a FolderProdos is created, it reads its
    // own catalog and repeats the process.
    readCatalog (this, directoryHeader.catalogBlocks);

    volumeBitMap = createVolumeBitMap ();
    freeBlocks = volumeBitMap.cardinality ();

    //    if (file.getFileType () == ProdosConstants.FILE_TYPE_SYS
    //        && file.getFileName ().equals ("DOS.3.3"))
    //      isDosMaster = true;                             // possibly
    //    if (isDosMaster)                                    // found DOS.3.3 file
    //      isDosMaster = checkDosMaster ();
  }

  // ---------------------------------------------------------------------------------//
  void readCatalog (AppleContainer container, List<AppleBlock> catalogBlocks)
  // ---------------------------------------------------------------------------------//
  {
    FileProdos file = null;

    for (AppleBlock catalogBlock : catalogBlocks)
    {
      byte[] buffer = catalogBlock.getBuffer ();
      int ptr = 4;

      for (int i = 0; i < ProdosConstants.ENTRIES_PER_BLOCK; i++)
      {
        int blockType = (buffer[ptr] & 0xF0) >>> 4;

        switch (blockType)
        {
          case ProdosConstants.SEEDLING:
          case ProdosConstants.SAPLING:
          case ProdosConstants.TREE:
            file = new FileProdos (this, container, catalogBlock, i);
            container.addFile (file);
            checkEmbeddedFileSystems (file);
            break;

          case ProdosConstants.PASCAL_ON_PROFILE:
            file = new FileProdos (this, container, catalogBlock, i);
            container.addFile (file);
            addEmbeddedFileSystem (file, 1024);       // fs starts 2 blocks in
            break;

          case ProdosConstants.GSOS_EXTENDED_FILE:
            container.addFile (new FileProdos (this, container, catalogBlock, i));
            break;

          case ProdosConstants.SUBDIRECTORY:
            container.addFile (new FolderProdos (this, container, catalogBlock, i));
            break;

          case ProdosConstants.SUBDIRECTORY_HEADER:
          case ProdosConstants.VOLUME_HEADER:
          case ProdosConstants.FREE:
            break;

          default:
            System.out.printf ("Unknown Blocktype: %02X%n", blockType);
        }

        ptr += ProdosConstants.ENTRY_SIZE;
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  private void checkEmbeddedFileSystems (FileProdos file)
  // ---------------------------------------------------------------------------------//
  {
    // $E0/$8000 - Binary II
    // $E0/$8002 - ShrinkIt (NuFX)

    switch (file.getFileType ())
    {
      case ProdosConstants.FILE_TYPE_LBR:
        addEmbeddedFileSystem (file, 0);
        if (file.getAuxType () == 0x0130)
          System.out.println ("Possible 2mg file " + file.getFileName ());
        break;

      case ProdosConstants.FILE_TYPE_NON:
        if (file.getFileName ().endsWith (".SHK"))
          addEmbeddedFileSystem (file, 0);
        break;
    }
  }

  // ---------------------------------------------------------------------------------//
  private BitSet createVolumeBitMap ()
  // ---------------------------------------------------------------------------------//
  {
    int bitPtr = 0;
    int bfrPtr = 0;
    byte[] buffer = null;

    BitSet bitMap = new BitSet (directoryHeader.totalBlocks);
    int bitMapBlockNo = directoryHeader.keyPtr;             // first block of the bitmap

    while (bitPtr < directoryHeader.totalBlocks)
    {
      if (bitPtr % BITS_PER_BLOCK == 0)
      {
        AppleBlock bitmapBlock = getBlock (bitMapBlockNo++, BlockType.FS_DATA);
        bitmapBlock.setBlockSubType ("V-BITMAP");
        buffer = bitmapBlock.getBuffer ();
        bfrPtr = 0;
      }

      byte flags = buffer[bfrPtr++];

      for (int i = 0; i < 8; i++)
      {
        if ((flags & 0x80) != 0)      // on == free
          bitMap.set (bitPtr);

        flags <<= 1;
        bitPtr++;
      }
    }

    return bitMap;
  }

  // ---------------------------------------------------------------------------------//
  private void writeVolumeBitMap ()
  // ---------------------------------------------------------------------------------//
  {
    int bitPtr = 0;
    int bfrPtr = 0;

    byte[] buffer = null;
    int blockNo = directoryHeader.keyPtr;

    while (bitPtr < directoryHeader.totalBlocks)
    {
      if (bitPtr % 0x1000 == 0)                 // get the next block
      {
        AppleBlock bitmapBlock = getBlock (blockNo++);
        bitmapBlock.markDirty ();
        buffer = bitmapBlock.getBuffer ();
        bfrPtr = 0;
      }

      int flags = 0;
      int mask = 0x80;

      for (int i = 0; i < 8; i++)
      {
        if (volumeBitMap.get (bitPtr++))       // on = free
          flags |= mask;
        mask >>>= 1;
      }

      buffer[bfrPtr++] = (byte) (flags & 0xFF);
    }
  }

  // ---------------------------------------------------------------------------------//
  public int getFirstBitmapBlockNo ()
  // ---------------------------------------------------------------------------------//
  {
    return directoryHeader.keyPtr;
  }

  // ---------------------------------------------------------------------------------//
  public static String getFileTypeText (int fileType)
  // ---------------------------------------------------------------------------------//
  {
    return ProdosConstants.fileTypes[fileType];
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("/%s", directoryHeader.fileName);
  }

  // ---------------------------------------------------------------------------------//
  private boolean checkDosMaster ()
  // ---------------------------------------------------------------------------------//
  {
    Optional<AppleFile> opt = getFile ("DOS.3.3");
    if (opt.isEmpty ())
      return false;

    AbstractAppleFile appleFile = (AbstractAppleFile) opt.get ();

    //    byte[] diskBuffer = getDataRecord ().data ();
    BlockReader diskReader = new BlockReader ("Disk", getDiskBuffer ());

    //    DataRecord dataRecord = appleFile.getDataRecord ();
    //    byte[] fileBuffer = appleFile.read ();
    //    byte[] fileBuffer = dataRecord.data ();
    BlockReader fileReader =
        new BlockReader (appleFile.getFileName (), appleFile.getRawFileBuffer ());

    FsDosMaster afs = new FsDosMaster (diskReader, fileReader);
    if (afs != null && afs.getFileSystems ().size () > 0)
    {
      appleFile.embedFileSystem (afs);
      return true;
    }

    return false;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("%s\n\n", getPath ()));

    text.append (" NAME           TYPE  BLOCKS  "
        + "MODIFIED         CREATED          ENDFILE SUBTYPE" + "\n\n");

    for (AppleFile file : getFiles ())
    {
      text.append (file.getCatalogLine ());
      text.append ("\n");
    }

    int totalBlocks = getTotalBlocks ();
    int freeBlocks = getTotalFreeBlocks ();

    text.append (
        String.format ("%nBLOCKS FREE:%5d     BLOCKS USED:%5d     TOTAL BLOCKS:%5d\n",
            freeBlocks, totalBlocks - freeBlocks, totalBlocks));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  void deleteFile (AppleFile appleFile)
  // ---------------------------------------------------------------------------------//
  {
    if (appleFile.getParentFileSystem () != this)
      throw new InvalidParentFileSystemException ("file not part of this File System");

    //    if (appleFile.isFolder ())
    //    {
    //      FolderProdos folder = (FolderProdos) appleFile;
    //      deleteCatalogEntry (folder.parentCatalogBlock, folder.parentCatalogPtr,
    //          folder.fileEntry);
    //    }
    //    else
    //    {
    //      FileProdos file = (FileProdos) appleFile;
    //      deleteCatalogEntry (file.parentCatalogBlock, file.parentCatalogPtr, file.fileEntry);
    //    }

    FileProdos fileProdos = (FileProdos) appleFile;

    fileProdos.catalogEntry.delete ();

    // create list of blocks to free
    List<AppleBlock> freeBlocks = new ArrayList<> (appleFile.getDataBlocks ());
    if (appleFile.isForkedFile ())
      for (AppleFile file : fileProdos.forks)
        freeBlocks.addAll (file.getDataBlocks ());

    // mark blocks as free in the vtoc
    int count = 0;
    for (AppleBlock block : freeBlocks)
    {
      if (block == null)
        continue;

      if (false)
        System.out.printf ("     %03d block : %-10s %,6d  %<04X%n", count,
            block.getBlockSubType (), block.getBlockNo ());

      volumeBitMap.set (block.getBlockNo ());       // mark block free
      count++;
    }

    //    System.out.printf ("Used blocks: %,d%n",
    //        directoryEntry.totalBlocks - volumeBitMap.cardinality ());

    writeVolumeBitMap ();
  }

  // ---------------------------------------------------------------------------------//
  private void deleteCatalogEntry (AppleBlock catalogBlock, int ptr,
      CatalogEntryProdos fileEntry)
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = catalogBlock.getBuffer ();    // catalog block with file's fileEntry
    buffer[ptr] = (byte) 0x00;                    // mark file as deleted
    catalogBlock.markDirty ();

    AppleBlock firstCatalogBlock = getBlock (fileEntry.headerPtr);
    buffer = firstCatalogBlock.getBuffer ();

    int fileCount = Utility.unsignedShort (buffer, 0x25);
    assert fileCount > 0;

    Utility.writeShort (buffer, 0x25, fileCount - 1);
    firstCatalogBlock.markDirty ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (directoryHeader);

    return Utility.rtrim (text);
  }
}
