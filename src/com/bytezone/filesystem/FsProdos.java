package com.bytezone.filesystem;

import java.util.BitSet;

import com.bytezone.utility.Utility;

// see https://prodos8.com/docs/techref/file-organization/
// see https://prodos8.com/docs/technote/25/
// -----------------------------------------------------------------------------------//
public class FsProdos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final int ENTRY_SIZE = 39;
  static final int ENTRIES_PER_BLOCK = 13;
  static final int BLOCK_ENTRY_SIZE = ENTRY_SIZE * ENTRIES_PER_BLOCK;

  static final int VOLUME_HEADER = 0x0F;
  static final int SUBDIRECTORY_HEADER = 0x0E;
  static final int SUBDIRECTORY = 0x0D;
  static final int GSOS_EXTENDED_FILE = 0x05;      // tech note #25
  static final int PASCAL_ON_PROFILE = 0x04;       // tech note #25
  static final int TREE = 0x03;
  static final int SAPLING = 0x02;
  static final int SEEDLING = 0x01;
  static final int FREE = 0x00;

  private BitSet volumeBitMap;

  private FolderProdos rootFolder;

  // ---------------------------------------------------------------------------------//
  public FsProdos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.PRODOS);

    int nextBlockNo = 2;                    // first catalog block
    int prevBlockNo = -1;

    assert catalogBlocks == 0;
    int catalogBlocks = 0;

    while (nextBlockNo != 0)
    {
      AppleBlock vtoc = getBlock (nextBlockNo);
      byte[] buffer = vtoc.read ();

      if (catalogBlocks == 0)
      {
        int type = (buffer[0x04] & 0xF0) >>> 4;
        if (type != VOLUME_HEADER)
          throw new FileFormatException ("FsProdos: No Volume Header");

        rootFolder = new FolderProdos (this, buffer, 4);
        addFile (rootFolder);

        if (rootFolder.entryLength != ENTRY_SIZE
            || rootFolder.entriesPerBlock != ENTRIES_PER_BLOCK)
          throw new FileFormatException ("FsProdos: Invalid entry data");

        if (rootFolder.keyPtr < 3 || rootFolder.keyPtr > 10)
          throw new FileFormatException (
              "FsProdos: Invalid bitmap block value: " + rootFolder.keyPtr);
      }

      prevBlockNo = Utility.unsignedShort (buffer, 0);
      nextBlockNo = Utility.unsignedShort (buffer, 2);

      if (!isValidBlockNo (prevBlockNo))
        throw new FileFormatException (
            "FsProdos: Invalid catalog previous block - " + prevBlockNo);

      if (!isValidBlockNo (nextBlockNo))
        throw new FileFormatException (
            "FsProdos: Invalid catalog next block - " + nextBlockNo);

      ++catalogBlocks;
    }

    processFolder (rootFolder, 2);
    assert rootFolder.fileCount == getFiles ().size ();
    setCatalogBlocks (catalogBlocks);

    createVolumeBitMap ();
  }

  // ---------------------------------------------------------------------------------//
  private void processFolder (FolderProdos parent, int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    AppleBlock catalogBlock = getBlock (blockNo);
    FileProdos file = null;

    while (catalogBlock.getBlockNo () != 0)
    {
      byte[] buffer = catalogBlock.read ();

      int ptr = 4;
      for (int i = 0; i < ENTRIES_PER_BLOCK; i++)
      {
        int blockType = (buffer[ptr] & 0xF0) >>> 4;

        switch (blockType)
        {
          case SEEDLING:
          case SAPLING:
          case TREE:
            file = new FileProdos (this, buffer, ptr);
            parent.addFile (file);

            if (file.getFileType () == ProdosConstants.FILE_TYPE_LBR)
              checkEmbeddedFileSystem (file, 0);

            break;

          case PASCAL_ON_PROFILE:
            file = new FileProdos (this, buffer, ptr);
            parent.addFile (file);
            checkEmbeddedFileSystem (file, 1024);
            break;

          case GSOS_EXTENDED_FILE:
            parent.addFile (new FileProdos (this, buffer, ptr));
            break;

          case SUBDIRECTORY:
            FolderProdos folder = new FolderProdos (this, buffer, ptr);
            parent.addFile (folder);
            processFolder (folder, folder.fileEntry.keyPtr);        // recursive
            break;

          case SUBDIRECTORY_HEADER:
            parent.addDirectoryHeader (buffer, ptr);
            break;

          case VOLUME_HEADER:
            break;

          case FREE:
            break;

          default:
            System.out.printf ("Unknown Blocktype: %02X%n", blockType);
        }
        ptr += ENTRY_SIZE;
      }

      catalogBlock = getBlock (Utility.unsignedShort (buffer, 2));

      if (!catalogBlock.isValid ())
        throw new FileFormatException ("FsProdos: Invalid catalog");
    }
  }

  // ---------------------------------------------------------------------------------//
  private void createVolumeBitMap ()
  // ---------------------------------------------------------------------------------//
  {
    int bitPtr = 0;
    int bfrPtr = 0;
    int blockNo = rootFolder.keyPtr;
    byte[] buffer = null;

    volumeBitMap = new BitSet (rootFolder.totalBlocks);

    while (bitPtr < rootFolder.totalBlocks)
    {
      if (bitPtr % 0x1000 == 0)
      {
        buffer = getBlock (blockNo++).read ();
        bfrPtr = 0;
      }

      byte flags = buffer[bfrPtr++];

      for (int i = 0; i < 8; i++)
      {
        if ((flags & 0x80) != 0)
          volumeBitMap.set (bitPtr);

        flags <<= 1;
        ++bitPtr;
      }
    }

    freeBlocks = volumeBitMap.cardinality ();
  }

  // ---------------------------------------------------------------------------------//
  public String getVolumeName ()
  // ---------------------------------------------------------------------------------//
  {
    return rootFolder.fileName;
  }

  // ---------------------------------------------------------------------------------//
  public static String getFileTypeText (int fileType)
  // ---------------------------------------------------------------------------------//
  {
    return ProdosConstants.fileTypes[fileType];
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString () + "\n\n");

    text.append (rootFolder);
    //    text.append (String.format ("Volume name ........... %s%n", volumeName));
    //    text.append (
    //        String.format ("Created ............... %s%n", created == null ? "" : created));
    //    text.append (String.format ("Entry length .......... %d%n", entryLength));
    //    text.append (String.format ("Entries per block ..... %d%n", entriesPerBlock));
    //    text.append (String.format ("File count ............ %d%n", fileCount));
    //    text.append (String.format ("Bitmap ptr ............ %d", bitmapPointer));

    return text.toString ();
  }
}
