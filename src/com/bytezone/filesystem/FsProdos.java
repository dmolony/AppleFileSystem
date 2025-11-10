package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// see https://prodos8.com/docs/techref/file-organization/
// see https://prodos8.com/docs/technote/25/
// see https://ciderpress2.com/formatdoc/PPM-notes.html
// -----------------------------------------------------------------------------------//
public class FsProdos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final int FIRST_CATALOG_BLOCK = 2;
  private static final int BITS_PER_BLOCK = 0x1000;

  private DirectoryHeaderProdos directoryHeader;

  private static List<FileDetails> dosMasterFiles = Arrays.asList (      //
      new FileDetails ("DOS.3.3", ProdosConstants.FILE_TYPE_SYS, 21),
      new FileDetails ("DOS", ProdosConstants.FILE_TYPE_BINARY, 19),
      new FileDetails ("DOS.MASTER", ProdosConstants.FILE_TYPE_BINARY, 4));

  // ---------------------------------------------------------------------------------//
  FsProdos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.PRODOS);

    // Create the Volume Directory Header. This is the first entry in the
    // first block of the catalog (block 2).
    directoryHeader = new DirectoryHeaderProdos (this, FIRST_CATALOG_BLOCK);
    setTotalCatalogBlocks (directoryHeader.catalogBlocks.size ());

    volumeBitMap = createVolumeBitMap ();
    freeBlocks = volumeBitMap.cardinality ();

    // Create a FileProdos or FolderProdos for each catalog entry. Each one creates
    // its own CatalogEntryProdos. When a FolderProdos is created, it reads its
    // own catalog and repeats the process.
    readCatalog (this, directoryHeader.catalogBlocks);

    if (blockReader.getDiskLength () > 143360 && directoryHeader.fileCount >= 10
        && diskContains (dosMasterFiles))
      checkDosMaster ();
  }

  // Search the top level directory for each of the files listed. All must exist.
  // ---------------------------------------------------------------------------------//
  private boolean diskContains (List<FileDetails> fileDetails)
  // ---------------------------------------------------------------------------------//
  {
    int filesFound = 0;

    loop: for (FileDetails details : fileDetails)
      for (AppleFile file : getFiles ())
        if (file.getFileType () == details.fileType
            && file.getTotalBlocks () == details.totalBlocks
            && file.getFileName ().equals (details.fileName))
        {
          ++filesFound;
          continue loop;
        }

    return filesFound == fileDetails.size ();
  }

  // ---------------------------------------------------------------------------------//
  record FileDetails (String fileName, int fileType, int totalBlocks)
  // ---------------------------------------------------------------------------------//
  {

  }

  // ---------------------------------------------------------------------------------//
  void readCatalog (AppleContainer container, List<AppleBlock> catalogBlocks)
  // ---------------------------------------------------------------------------------//
  {
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
            FileProdos fileProdos = new FileProdos (this, container, catalogBlock, i);
            container.addFile (fileProdos);
            checkEmbeddedFileSystems (fileProdos);
            break;

          case ProdosConstants.PASCAL_ON_PROFILE:
            FilePPM filePPM = new FilePPM (this, container, catalogBlock, i);
            container.addFile (filePPM);
            addPpmVolumes (filePPM);
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
  private void addPpmVolumes (FilePPM file)
  // ---------------------------------------------------------------------------------//
  {
    for (int volume = 1; volume <= file.getTotalVolumes (); volume++)
      addEmbeddedFileSystem (file, file.volumeNames[volume],
          file.getVolumeBuffer (volume));
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
        addEmbeddedFileSystem (file);
        if (file.getAuxType () == 0x0130)
          System.out.println ("Possible 2mg file " + file.getFileName ());
        break;

      case ProdosConstants.FILE_TYPE_NON:
        if (file.getFileLength () == 143360)
        {
          String fileName = file.getFileName ();
          if (fileName.endsWith (".SHK") || fileName.endsWith (".DSK")
              || fileName.endsWith (".DO") || fileName.endsWith (".PO"))
            addEmbeddedFileSystem (file);
        }
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

    // temp
    //    compare ();

    AbstractAppleFile appleFile = (AbstractAppleFile) opt.get ();

    BlockReader diskReader = new BlockReader ("DosMaster", getDiskBuffer ());
    diskReader.setParameters (FileSystemFactory.prodos1);

    FsDosMaster afs = new FsDosMaster (diskReader, appleFile);
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
  private void compare ()
  // ---------------------------------------------------------------------------------//
  {
    Optional<AppleFile> opt1 = getFile ("DOS.MASTER");
    Optional<AppleFile> opt2 = getFile ("DOS.3.3");

    if (opt1.isEmpty () || opt2.isEmpty ())
      return;

    AppleFile dosMaster = opt1.get ();
    AppleFile dos33 = opt2.get ();

    System.out.printf ("Comparing %s to %s%n%n", dosMaster.getFileName (),
        dos33.getFileName ());

    List<AppleBlock> dosMasterBlocks = dosMaster.getDataBlocks ();
    List<AppleBlock> dos33Blocks = dos33.getDataBlocks ();

    byte[] buffer1 = dosMasterBlocks.get (0).getBuffer ();
    byte[] buffer2 = dos33Blocks.get (0).getBuffer ();

    for (int i = 0; i < 512; i++)
    {
      boolean diff = buffer1[i] != buffer2[i];
      if (diff)
        System.out.printf ("%03d  %<02X :  %02X -> %02X  %s%n", i, buffer1[i], buffer2[i],
            diff ? "*" : "");
    }
    System.out.println (Utility.format (buffer2, 0, 256));
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
