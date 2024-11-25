package com.bytezone.filesystem;

import java.util.BitSet;
import java.util.Optional;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// see https://prodos8.com/docs/techref/file-organization/
// see https://prodos8.com/docs/technote/25/
// -----------------------------------------------------------------------------------//
public class FsProdos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private final BitSet volumeBitMap;
  private DirectoryEntryProdos directoryEntry;
  private boolean isDosMaster;

  // ---------------------------------------------------------------------------------//
  FsProdos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.PRODOS);

    int nextBlockNo = 2;                    // first catalog block
    int prevBlockNo = -1;

    assert totalCatalogBlocks == 0;
    int catalogBlocks = 0;

    while (nextBlockNo != 0)
    {
      AppleBlock vtoc = getBlock (nextBlockNo, BlockType.FS_DATA);
      byte[] buffer = vtoc.read ();

      if (catalogBlocks == 0)
        directoryEntry = new DirectoryEntryProdos (buffer, 4);

      if (directoryEntry.storageType != ProdosConstants.VOLUME_HEADER)
        throw new FileFormatException ("FsProdos: No Volume Header");

      if (directoryEntry.entryLength != ProdosConstants.ENTRY_SIZE
          || directoryEntry.entriesPerBlock != ProdosConstants.ENTRIES_PER_BLOCK)
        throw new FileFormatException ("FsProdos: Invalid entry data");

      // check first bitmap block number (usually 6)
      if (directoryEntry.keyPtr < 3 || directoryEntry.keyPtr > 10)
        throw new FileFormatException (
            "FsProdos: Invalid bitmap block value: " + directoryEntry.keyPtr);

      prevBlockNo = Utility.unsignedShort (buffer, 0);
      nextBlockNo = Utility.unsignedShort (buffer, 2);

      if (!isValidAddress (prevBlockNo))
        throw new FileFormatException (
            "FsProdos: Invalid catalog previous block - " + prevBlockNo);

      if (!isValidAddress (nextBlockNo))
        throw new FileFormatException (
            "FsProdos: Invalid catalog next block - " + nextBlockNo);

      ++catalogBlocks;
    }

    processFolder (this, 2);              // volume directory

    assert directoryEntry.fileCount == getFiles ().size ();
    setTotalCatalogBlocks (catalogBlocks);

    volumeBitMap = createVolumeBitMap (directoryEntry);
    freeBlocks = volumeBitMap.cardinality ();

    if (isDosMaster)
      isDosMaster = checkDosMaster ();
  }

  // ---------------------------------------------------------------------------------//
  private void processFolder (AppleContainer parent, int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    AppleBlock catalogBlock = getBlock (blockNo, BlockType.FS_DATA);
    if (catalogBlock == null)
      throw new FileFormatException ("FsProdos: Invalid catalog");

    byte[] buffer = catalogBlock.read ();
    boolean isFolder = (buffer[4] & 0xF0) == 0xE0;      // subdirectory header

    FileProdos file = null;

    while (true)
    {
      catalogBlock.setBlockSubType (isFolder ? "FOLDER" : "CATALOG");
      if (isFolder)
      {
        catalogBlock.setBlockSubType ("FOLDER");
        ((FolderProdos) parent).dataBlocks.add (catalogBlock);
      }
      else
        catalogBlock.setBlockSubType ("CATALOG");

      int ptr = 4;
      for (int i = 0; i < ProdosConstants.ENTRIES_PER_BLOCK; i++)
      {
        int blockType = (buffer[ptr] & 0xF0) >>> 4;

        switch (blockType)
        {
          case ProdosConstants.SEEDLING:
          case ProdosConstants.SAPLING:
          case ProdosConstants.TREE:
            file = new FileProdos (this, parent, buffer, ptr);
            parent.addFile (file);

            if (file.getFileType () == ProdosConstants.FILE_TYPE_LBR)
              addEmbeddedFileSystem (file, 0);

            if (file.getFileType () == ProdosConstants.FILE_TYPE_SYS
                && file.getFileName ().equals ("DOS.3.3"))
              isDosMaster = true;                             // possibly

            break;

          case ProdosConstants.PASCAL_ON_PROFILE:
            file = new FileProdos (this, parent, buffer, ptr);
            parent.addFile (file);
            addEmbeddedFileSystem (file, 1024);
            break;

          case ProdosConstants.GSOS_EXTENDED_FILE:
            parent.addFile (new FileProdos (this, parent, buffer, ptr));
            break;

          case ProdosConstants.SUBDIRECTORY:
            FolderProdos folder = new FolderProdos (this, parent, buffer, ptr);
            parent.addFile (folder);
            processFolder (folder, folder.fileEntry.keyPtr);        // recursive
            break;

          case ProdosConstants.SUBDIRECTORY_HEADER:
            ((FolderProdos) parent).addDirectoryEntry (buffer, ptr);
            break;

          case ProdosConstants.VOLUME_HEADER:
            break;

          case ProdosConstants.FREE:
            break;

          default:
            System.out.printf ("Unknown Blocktype: %02X%n", blockType);
        }
        ptr += ProdosConstants.ENTRY_SIZE;
      }

      int nextBlockNo = Utility.unsignedShort (buffer, 2);
      if (nextBlockNo == 0)
        break;

      catalogBlock = getBlock (nextBlockNo, BlockType.FS_DATA);

      if (catalogBlock == null)
        throw new FileFormatException ("FsProdos: Invalid catalog");

      buffer = catalogBlock.read ();
    }
  }

  // ---------------------------------------------------------------------------------//
  private BitSet createVolumeBitMap (DirectoryEntryProdos directoryEntry)
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
        AppleBlock block = getBlock (blockNo++, BlockType.FS_DATA);
        block.setBlockSubType ("V-BITMAP");
        buffer = block.read ();
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
  public int getBitmapBlockNo ()
  // ---------------------------------------------------------------------------------//
  {
    return directoryEntry.keyPtr;
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
    BlockReader diskReader = new BlockReader ("Disk", getDataRecord ());

    //    DataRecord dataRecord = appleFile.getDataRecord ();
    //    byte[] fileBuffer = appleFile.read ();
    //    byte[] fileBuffer = dataRecord.data ();
    BlockReader fileReader =
        new BlockReader (appleFile.getFileName (), appleFile.getDataRecord ());

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
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (directoryEntry);

    return Utility.rtrim (text);
  }
}
