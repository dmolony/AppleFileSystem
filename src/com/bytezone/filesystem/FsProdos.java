package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

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

  private int entryLength;
  private int entriesPerBlock;
  private int fileCount;
  private int bitmapPointer;

  // ---------------------------------------------------------------------------------//
  public FsProdos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader);

    readCatalog ();
  }

  // ---------------------------------------------------------------------------------//
  private void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    setFileSystemName ("Prodos");

    int nextBlockNo = 2;                    // first catalog block
    int prevBlockNo = -1;

    assert catalogBlocks == 0;
    int catalogBlocks = 0;

    while (nextBlockNo != 0)
    {
      AppleBlock vtoc = getBlock (nextBlockNo);           // VTOC sector
      byte[] buffer = vtoc.read ();

      if (catalogBlocks == 0)
      {
        int type = (buffer[0x04] & 0xF0) >>> 4;
        if (type != VOLUME_HEADER)
          throw new FileFormatException ("FsProdos: No Volume Header");

        entryLength = buffer[0x23] & 0xFF;                        // 39
        entriesPerBlock = buffer[0x24] & 0xFF;                    // 13
        fileCount = Utility.unsignedShort (buffer, 0x25);
        bitmapPointer = Utility.unsignedShort (buffer, 0x27);     // 6

        if (entryLength != ENTRY_SIZE || entriesPerBlock != ENTRIES_PER_BLOCK)
          throw new FileFormatException ("FsProdos: Invalid entry data");

        if (bitmapPointer < 3 || bitmapPointer > 10)
          throw new FileFormatException ("FsProdos: Invalid bitmap block value: " + bitmapPointer);
      }

      prevBlockNo = Utility.unsignedShort (buffer, 0);
      nextBlockNo = Utility.unsignedShort (buffer, 2);

      if (!isValidBlockNo (prevBlockNo))
        throw new FileFormatException ("FsProdos: Invalid catalog previous block - " + prevBlockNo);
      if (!isValidBlockNo (nextBlockNo))
        throw new FileFormatException ("FsProdos: Invalid catalog next block - " + nextBlockNo);

      ++catalogBlocks;
    }

    processFolder (this, 2);
    assert fileCount == getFiles ().size ();
    setCatalogBlocks (catalogBlocks);
  }

  // ---------------------------------------------------------------------------------//
  private void processFolder (AppleFile parent, int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    AppleBlock catalogBlock = getBlock (blockNo);

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
            FileProdos file = new FileProdos (this, buffer, ptr);
            if (file.getFileType () == ProdosConstants.FILE_TYPE_LBR)
              addFileSystem (parent, file);
            else
              parent.addFile (file);
            break;

          case PASCAL_ON_PROFILE:
            file = new FileProdos (this, buffer, ptr);
            byte[] fileBuffer = file.read ();
            BlockReader pascalBlockReader =
                new BlockReader (fileBuffer, 1024, fileBuffer.length - 1024);
            addFileSystem (parent, pascalBlockReader);
            break;

          case GSOS_EXTENDED_FILE:
            parent.addFile (new FileProdos (this, buffer, ptr));
            break;

          case SUBDIRECTORY:
            FolderProdos folder = new FolderProdos (this, buffer, ptr);
            parent.addFile (folder);
            processFolder (folder, folder.keyPtr);
            break;

          case SUBDIRECTORY_HEADER:
          case VOLUME_HEADER:
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
  @Override
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toText () + "\n\n");

    text.append (String.format ("Entry length .......... %d%n", entryLength));
    text.append (String.format ("Entries per block ..... %d%n", entriesPerBlock));
    text.append (String.format ("File count ............ %d%n", fileCount));
    text.append (String.format ("Bitmap ptr ............ %d", bitmapPointer));

    return text.toString ();
  }
}
