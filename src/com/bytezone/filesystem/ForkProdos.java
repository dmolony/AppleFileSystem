package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------------//
public class ForkProdos
// -----------------------------------------------------------------------------------//
{
  static final int VOLUME_HEADER = 0x0F;
  static final int SUBDIRECTORY_HEADER = 0x0E;
  static final int SUBDIRECTORY = 0x0D;
  static final int GSOS_EXTENDED_FILE = 0x05;      // tech note #25
  static final int PASCAL_ON_PROFILE = 0x04;       // tech note #25
  static final int TREE = 0x03;
  static final int SAPLING = 0x02;
  static final int SEEDLING = 0x01;
  static final int FREE = 0x00;

  private FsProdos fileSystem;
  private int storageType;
  private int size;
  private int eof;
  private int keyPtr;

  private AppleBlock masterIndexBlock;
  private final List<AppleBlock> indexBlocks = new ArrayList<> ();
  private final List<AppleBlock> dataBlocks = new ArrayList<> ();

  private byte[] data;

  // ---------------------------------------------------------------------------------//
  ForkProdos (FsProdos fileSystem, int keyPtr, int storageType, int size, int eof)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystem = fileSystem;
    this.storageType = storageType;
    this.keyPtr = keyPtr;
    this.size = size;
    this.eof = eof;

    List<Integer> blockNos = new ArrayList<> ();
    AppleBlock dataBlock = fileSystem.getBlock (keyPtr);

    if (dataBlock.isValid ())
      switch (storageType)
      {
        case SEEDLING:
          blockNos.add (keyPtr);
          break;

        case SAPLING:
          blockNos.addAll (readIndex (keyPtr));
          break;

        case TREE:
          for (Integer indexBlock : readMasterIndex (keyPtr))
            if (fileSystem.getBlock (indexBlock).isValid ())
              blockNos.addAll (readIndex (indexBlock));
          break;

        case PASCAL_ON_PROFILE:
          for (int i = keyPtr; i < fileSystem.getSize (); i++)
            blockNos.add (i);
          break;

        default:
          System.out.printf ("Impossible %02X%n", storageType);
          break;
      }

    // remove trailing empty blocks
    while (blockNos.size () > 0 && blockNos.get (blockNos.size () - 1) == 0)
      blockNos.remove (blockNos.size () - 1);

    for (Integer blockNo : blockNos)
    {
      if (blockNo == 0)
        dataBlocks.add (null);
      else
        dataBlocks.add (fileSystem.getBlock (blockNo));
    }
  }

  // ---------------------------------------------------------------------------------//
  public int getEof ()
  // ---------------------------------------------------------------------------------//
  {
    return eof;
  }

  // ---------------------------------------------------------------------------------//
  public int getSize ()
  // ---------------------------------------------------------------------------------//
  {
    return size;
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
}
