package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.diskbrowser.prodos.ProdosConstants;

// -----------------------------------------------------------------------------------//
public class FileProdos extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern ("d-LLL-yy");
  private static final DateTimeFormatter stf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  static final int VOLUME_HEADER = 0x0F;
  static final int SUBDIRECTORY_HEADER = 0x0E;
  static final int SUBDIRECTORY = 0x0D;
  static final int GSOS_EXTENDED_FILE = 0x05;      // tech note #25
  static final int PASCAL_ON_PROFILE = 0x04;       // tech note #25
  static final int TREE = 0x03;
  static final int SAPLING = 0x02;
  static final int SEEDLING = 0x01;
  static final int FREE = 0x00;

  int storageType;
  int fileType;
  int keyPtr;
  int size;
  int eof;
  int auxType;
  int headerPtr;
  LocalDateTime created;
  LocalDateTime modified;
  String dateC, timeC, dateM, timeM;

  private AppleBlock masterIndexBlock;
  private final List<AppleBlock> indexBlocks = new ArrayList<> ();
  private final List<AppleBlock> dataBlocks = new ArrayList<> ();
  private byte[] data;

  // ---------------------------------------------------------------------------------//
  FileProdos (FsProdos fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFile = true;

    storageType = (buffer[ptr] & 0xF0) >> 4;
    int nameLength = buffer[ptr] & 0x0F;
    if (nameLength > 0)
      name = Utility.string (buffer, ptr + 1, nameLength);

    fileType = buffer[ptr + 0x10] & 0xFF;
    keyPtr = Utility.unsignedShort (buffer, ptr + 0x11);
    size = Utility.unsignedShort (buffer, ptr + 0x13);
    eof = Utility.unsignedTriple (buffer, ptr + 0x15);
    auxType = Utility.unsignedShort (buffer, ptr + 0x1F);
    headerPtr = Utility.unsignedShort (buffer, ptr + 0x25);

    created = Utility.getAppleDate (buffer, ptr + 0x18);
    dateC = created == null ? NO_DATE : created.format (sdf);
    timeC = created == null ? "" : created.format (stf);

    modified = Utility.getAppleDate (buffer, ptr + 0x21);
    dateM = modified == null ? NO_DATE : modified.format (sdf);
    timeM = modified == null ? "" : modified.format (stf);

    addDataBlocks ();
  }

  // ---------------------------------------------------------------------------------//
  private void addDataBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    assert dataBlocks.size () == 0;

    List<Integer> blocks = new ArrayList<> ();
    AppleBlock dataBlock = fileSystem.getBlock (keyPtr);

    if (dataBlock.isValid ())
      switch (storageType)
      {
        case SEEDLING:
          blocks.add (keyPtr);
          break;

        case SAPLING:
          blocks.addAll (readIndex (keyPtr));
          break;

        case TREE:
          for (Integer indexBlock : readMasterIndex (keyPtr))
          {
            AppleBlock dataBlock2 = fileSystem.getBlock (indexBlock);
            if (dataBlock2.isValid ())
              blocks.addAll (readIndex (indexBlock));
          }
          break;

        case GSOS_EXTENDED_FILE:
          // read two forks
          break;

        case PASCAL_ON_PROFILE:
          for (int i = keyPtr; i < fileSystem.getSize (); i++)
            blocks.add (i);

          break;
      }

    // remove trailing empty blocks
    while (blocks.size () > 0 && blocks.get (blocks.size () - 1) == 0)
      blocks.remove (blocks.size () - 1);

    for (Integer block : blocks)
    {
      if (block == 0)
        dataBlocks.add (null);
      else
        dataBlocks.add (fileSystem.getBlock (block));
    }
  }

  // ---------------------------------------------------------------------------------//
  private List<Integer> readIndex (int blockPtr)
  // ---------------------------------------------------------------------------------//
  {
    List<Integer> blocks = new ArrayList<> (256);

    if (blockPtr == 0)                    // master index contains a zero
      for (int i = 0; i < 256; i++)
        blocks.add (0);
    else
    {
      indexBlocks.add (fileSystem.getBlock (blockPtr));

      byte[] buffer = fileSystem.getBlock (blockPtr).read ();
      for (int i = 0; i < 256; i++)
      {
        int blockNo = (buffer[i] & 0xFF) | ((buffer[i + 0x100] & 0xFF) << 8);
        AppleBlock dataBlock = fileSystem.getBlock (blockNo);
        blocks.add (dataBlock.isValid () ? blockNo : 0);      // should throw error
      }
    }

    return blocks;
  }

  // ---------------------------------------------------------------------------------//
  private List<Integer> readMasterIndex (int keyPtr)
  // ---------------------------------------------------------------------------------//
  {
    masterIndexBlock = fileSystem.getBlock (keyPtr);
    indexBlocks.add (fileSystem.getBlock (keyPtr));

    byte[] buffer = masterIndexBlock.read ();             // master index

    int highest = 0x80;
    while (highest-- > 0)                                 // decrement after test
      if (buffer[highest] != 0 || buffer[highest + 0x100] != 0)
        break;

    List<Integer> blocks = new ArrayList<> (highest + 1);
    for (int i = 0; i <= highest; i++)
    {
      int blockNo = (buffer[i] & 0xFF) | ((buffer[i + 256] & 0xFF) << 8);
      AppleBlock dataBlock = fileSystem.getBlock (blockNo);
      blocks.add (dataBlock.isValid () ? blockNo : 0);
    }

    return blocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    if (data == null)
    {
      switch (storageType)
      {
        case SEEDLING:
        case SAPLING:
        case TREE:
          return fileSystem.readBlocks (dataBlocks);

        case SUBDIRECTORY:
          return fileSystem.readBlocks (dataBlocks);

        case GSOS_EXTENDED_FILE:
          return fileSystem.readBlocks (dataBlocks);

        case PASCAL_ON_PROFILE:
          return fileSystem.readBlocks (dataBlocks);

        default:
          System.out.println ("Unknown storage type in getBuffer : " + storageType);
          return new byte[512];
      }
    }

    return data;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return eof;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return size;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-30s %-3s  %04X %4d %,8d", name,
        ProdosConstants.fileTypes[fileType], keyPtr, size, eof);
  }
}
