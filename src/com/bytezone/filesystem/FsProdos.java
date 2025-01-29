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

  private final BitSet volumeBitMap;
  private DirectoryEntryProdos directoryEntry;
  private boolean isDosMaster;

  // ---------------------------------------------------------------------------------//
  FsProdos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.PRODOS);

    // create the Volume Directory Header
    directoryEntry = new DirectoryEntryProdos (this, FIRST_CATALOG_BLOCK);
    setTotalCatalogBlocks (directoryEntry.catalogBlocks.size ());

    readCatalog ();

    volumeBitMap = createVolumeBitMap ();
    freeBlocks = volumeBitMap.cardinality ();

    if (isDosMaster)                                    // found DOS.3.3 file
      isDosMaster = checkDosMaster ();
  }

  // ---------------------------------------------------------------------------------//
  private void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    FileProdos file = null;

    for (AppleBlock catalogBlock : directoryEntry.catalogBlocks)
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
            file = new FileProdos (this, this, catalogBlock, ptr);
            addFile (file);

            if (file.getFileType () == ProdosConstants.FILE_TYPE_LBR)
              addEmbeddedFileSystem (file, 0);

            if (file.getFileType () == ProdosConstants.FILE_TYPE_SYS
                && file.getFileName ().equals ("DOS.3.3"))
              isDosMaster = true;                             // possibly

            break;

          case ProdosConstants.PASCAL_ON_PROFILE:
            file = new FileProdos (this, this, catalogBlock, ptr);
            addFile (file);
            addEmbeddedFileSystem (file, 1024);
            break;

          case ProdosConstants.GSOS_EXTENDED_FILE:
            addFile (new FileProdos (this, this, catalogBlock, ptr));
            break;

          case ProdosConstants.SUBDIRECTORY:
            FolderProdos folder = new FolderProdos (this, this, catalogBlock, ptr);
            addFile (folder);
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
  private BitSet createVolumeBitMap ()
  // ---------------------------------------------------------------------------------//
  {
    int bitPtr = 0;
    int bfrPtr = 0;
    byte[] buffer = null;

    BitSet bitMap = new BitSet (directoryEntry.totalBlocks);
    int blockNo = directoryEntry.keyPtr;

    while (bitPtr < directoryEntry.totalBlocks)
    {
      if (bitPtr % 0x1000 == 0)
      {
        AppleBlock bitmapBlock = getBlock (blockNo++, BlockType.FS_DATA);
        bitmapBlock.setBlockSubType ("V-BITMAP");
        buffer = bitmapBlock.getBuffer ();
        bfrPtr = 0;
      }

      byte flags = buffer[bfrPtr++];

      for (int i = 0; i < 8; i++)
      {
        if ((flags & 0x80) != 0)
          bitMap.set (bitPtr);

        flags <<= 1;
        ++bitPtr;
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
    int blockNo = directoryEntry.keyPtr;

    while (bitPtr < directoryEntry.totalBlocks)
    {
      if (bitPtr % 0x1000 == 0)
      {
        AppleBlock bitmapBlock = getBlock (blockNo++);
        markDirty (bitmapBlock);
        buffer = bitmapBlock.getBuffer ();
        bfrPtr = 0;
      }

      byte flags = 0;
      byte mask = (byte) 0x80;

      for (int i = 0; i < 8; i++)
      {
        if (!volumeBitMap.get (bitPtr++))
          flags |= mask;
        mask >>>= 1;
      }

      buffer[bfrPtr++] = flags;
    }
  }

  // ---------------------------------------------------------------------------------//
  //  public int getBitmapBlockNo ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return directoryEntry.keyPtr;
  //  }

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
    return String.format ("/%s", directoryEntry.fileName);
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
        new BlockReader (appleFile.getFileName (), appleFile.getFileBuffer ());

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

    text.append (String.format ("%s%n%n", getPath ()));

    text.append (" NAME           TYPE  BLOCKS  "
        + "MODIFIED         CREATED          ENDFILE SUBTYPE" + "\n\n");

    for (AppleFile file : getFiles ())
    {
      text.append (file.getCatalogLine ());
      text.append ("\n");
    }

    int totalBlocks = getTotalBlocks ();
    int freeBlocks = getFreeBlocks ();

    text.append (
        String.format ("%nBLOCKS FREE:%5d     BLOCKS USED:%5d     TOTAL BLOCKS:%5d%n",
            freeBlocks, totalBlocks - freeBlocks, totalBlocks));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void deleteFile (AppleFile appleFile)
  // ---------------------------------------------------------------------------------//
  {
    if (appleFile.getParentFileSystem () != this)
      throw new InvalidParentFileSystemException ("file not part of this File System");

    if (appleFile.isFolder ())
    {
      FolderProdos folder = (FolderProdos) appleFile;
      deleteCatalogEntry (folder.parentCatalogBlock, folder.parentCatalogPtr,
          folder.fileEntry);
    }
    else
    {
      FileProdos file = (FileProdos) appleFile;
      deleteCatalogEntry (file.parentCatalogBlock, file.parentCatalogPtr, file.fileEntry);
    }

    // create list of blocks to free
    List<AppleBlock> freeBlocks = new ArrayList<> (appleFile.getBlocks ());
    if (appleFile.isFork ())
      for (AppleFile file : ((FileProdos) appleFile).forks)
        freeBlocks.addAll (file.getBlocks ());

    // mark blocks as free in the vtoc
    int count = 0;
    for (AppleBlock block : freeBlocks)
    {
      if (block == null)
        continue;

      if (false)
        System.out.printf ("     %03d block : %-10s %,6d  %<04X%n", count,
            block.getBlockSubType (), block.getBlockNo ());

      volumeBitMap.set (block.getBlockNo ());
      count++;
    }

    System.out.printf ("Used blocks: %,d%n",
        directoryEntry.totalBlocks - volumeBitMap.cardinality ());

    writeVolumeBitMap ();
  }

  // ---------------------------------------------------------------------------------//
  private void deleteCatalogEntry (AppleBlock catalogBlock, int ptr,
      FileEntryProdos fileEntry)
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = catalogBlock.getBuffer ();    // catalog block with file's fileEntry
    buffer[ptr] = (byte) 0x00;                    // mark file as deleted
    markDirty (catalogBlock);

    AppleBlock firstCatalogBlock = getBlock (fileEntry.headerPtr);
    buffer = firstCatalogBlock.getBuffer ();

    int fileCount = Utility.unsignedShort (buffer, 0x25);
    assert fileCount > 0;

    Utility.writeShort (buffer, 0x25, fileCount - 1);
    markDirty (firstCatalogBlock);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (directoryEntry);

    return Utility.rtrim (text);
  }
}
