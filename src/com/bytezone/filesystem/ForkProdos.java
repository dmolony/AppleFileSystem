package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.AppleFileSystem.FileSystemType;

// -----------------------------------------------------------------------------------//
public class ForkProdos implements AppleFile
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

  private FileProdos parent;
  private FsProdos fileSystem;
  private String name;                              // DATA, RESOURCE, FILE

  private int storageType;
  private int size;
  private int eof;
  private int keyPtr;

  private AppleBlock masterIndexBlock;
  private final List<AppleBlock> indexBlocks = new ArrayList<> ();
  private final List<AppleBlock> dataBlocks = new ArrayList<> ();

  private byte[] data;

  // ---------------------------------------------------------------------------------//
  ForkProdos (FileProdos parent, String name, int keyPtr, int storageType, int size, int eof)
  // ---------------------------------------------------------------------------------//
  {
    this.parent = parent;
    this.name = name;
    this.fileSystem = (FsProdos) parent.getFileSystem ();

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
          for (int i = keyPtr; i < fileSystem.getTotalBlocks (); i++)
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
  public int getAuxType ()
  // ---------------------------------------------------------------------------------//
  {
    return parent.getAuxType ();
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
      data = fileSystem.readBlocks (dataBlocks);

    return data;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return name;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("cannot addFile() to a fork");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("cannot getFiles() from a fork");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleFileSystem getFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return parent.getFileSystem ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public FileSystemType getFileSystemType ()
  // ---------------------------------------------------------------------------------//
  {
    return parent.getFileSystemType ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()
  // ---------------------------------------------------------------------------------//
  {
    return eof;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return size;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("getBlocks() not implemented");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String catalog ()
  // ---------------------------------------------------------------------------------//
  {
    return "";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return parent.getFileTypeText ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return parent.getFileType ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlockSize ()
  // ---------------------------------------------------------------------------------//
  {
    return parent.getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFile ()
  // ---------------------------------------------------------------------------------//
  {
    return true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFork ()
  // ---------------------------------------------------------------------------------//
  {
    return true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    return name;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-30s %-3s  %04X %4d %,10d", name, parent.getFileTypeText (), keyPtr,
        getTotalBlocks (), getLength ());
  }
}
