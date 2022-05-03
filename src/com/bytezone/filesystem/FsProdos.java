package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.diskbrowser.prodos.ProdosConstants;

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
  public FsProdos (String name, byte[] buffer, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public FsProdos (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);
    setFileSystemName ("Prodos");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    int nextBlockNo = 2;
    int prevBlockNo = 0;

    assert catalogBlocks == 0;

    while (nextBlockNo != 0)
    {
      AppleBlock vtoc = getBlock (nextBlockNo);           // VTOC sector
      byte[] buffer = vtoc.read ();

      if (catalogBlocks == 0)
      {
        int type = (buffer[0x04] & 0xF0) >>> 4;
        if (type != VOLUME_HEADER)
          throw new FileFormatException ("No Volume Header");

        if (buffer[0x23] != ENTRY_SIZE || buffer[0x24] != ENTRIES_PER_BLOCK)
          throw new FileFormatException ("Invalid entry data");

        int bitMapBlock = Utility.unsignedShort (buffer, 0x27);
        if (bitMapBlock < 3 || bitMapBlock > 10)
          throw new FileFormatException ("Invalid bitmap block value: " + bitMapBlock);

        entryLength = buffer[0x23] & 0xFF;                // 39
        entriesPerBlock = buffer[0x24] & 0xFF;            // 13
        fileCount = Utility.unsignedShort (buffer, 0x25);
        bitmapPointer = Utility.unsignedShort (buffer, 0x27);     // 6
        totalBlocks = Utility.unsignedShort (buffer, 0x29);
      }

      prevBlockNo = Utility.unsignedShort (buffer, 0);
      nextBlockNo = Utility.unsignedShort (buffer, 2);

      if (!isValidBlockNo (prevBlockNo))
        throw new FileFormatException ("Invalid catalog previous block");
      if (!isValidBlockNo (nextBlockNo))
        throw new FileFormatException ("Invalid catalog next block");

      ++catalogBlocks;
    }

    processFolder (2, this);
    assert fileCount == files.size ();
  }

  // ---------------------------------------------------------------------------------//
  private void processFolder (int blockNo, AppleFile parent)
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

            if (file.fileType == ProdosConstants.FILE_TYPE_LBR && file.name.endsWith (".SHK"))
            {
              List<AppleFileSystem> fsList =
                  FileSystemFactory.getFileSystems (file.name, file.read ());
              if (fsList.size () > 0)
                for (AppleFileSystem fs : fsList)
                  parent.addFile (fs);
            }
            else
              parent.addFile (file);
            break;

          case PASCAL_ON_PROFILE:
            FileProdos pascal = new FileProdos (this, buffer, ptr);
            byte[] fileBuffer = pascal.read ();
            FsPascal fs = FileSystemFactory.getPascal (pascal.name, fileBuffer, 1024,
                fileBuffer.length - 1024);
            parent.addFile (fs);
            break;

          case GSOS_EXTENDED_FILE:
            // add both forks
            break;

          case SUBDIRECTORY:
            FolderProdos folder = new FolderProdos (this, buffer, ptr);
            parent.addFile (folder);
            processFolder (folder.keyPtr, folder);
            break;

          case SUBDIRECTORY_HEADER:
            //            ((ProdosFolder) parent).addSubdirectoryDetails (buffer, ptr);
            break;

          case VOLUME_HEADER:
            //            folder = new ProdosFolder (this, buffer, ptr);
            //            parent.addFile (folder);
            //            parent = folder;             // replace parameter
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
        throw new FileFormatException ("Invalid catalog");
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toText ());

    text.append (String.format ("Entry length .......... %d%n", entryLength));
    text.append (String.format ("Entries per block ..... %d%n", entriesPerBlock));
    text.append (String.format ("File count ............ %d%n", fileCount));
    text.append (String.format ("Bitmap ptr ............ %d%n", bitmapPointer));

    return text.toString ();
  }
}
