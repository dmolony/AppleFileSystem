package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.FileProdos.ForkType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class ForkProdos extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private FileProdos parentFile;
  private FsProdos fileSystem;
  private ForkType forkType;

  private int storageType;
  private int size;
  private int eof;
  private int keyPtr;

  private AppleBlock masterIndexBlock;
  private final List<AppleBlock> indexBlocks = new ArrayList<> ();
  private final List<AppleBlock> dataBlocks = new ArrayList<> ();

  private byte[] data;

  // All ForkProdos files have a single FileProdos parent. Forks are also AppleFiles,
  // but only the DATA and RESOURCE forks are treated as standalone files. Normal
  // prodos files simply use a ForkProdos for their data (as the code to read them 
  // is identical.
  // DATA and RESOURCE forks are stored as children of the FileProdos, normal
  // prodos files keep a private reference to its 'fork' data.
  // ---------------------------------------------------------------------------------//
  ForkProdos (FileProdos parentFile, ForkType forkType, int keyPtr, int storageType,
      int size, int eof)
  // ---------------------------------------------------------------------------------//
  {
    super (parentFile.getFileSystem ());

    isFile = true;
    isFork = forkType != null;

    fileType = parentFile.getFileType ();
    fileTypeText = parentFile.getFileTypeText ();

    this.parentFile = parentFile;
    this.forkType = forkType;
    this.fileName = forkType == ForkType.DATA ? "Data fork"
        : forkType == ForkType.RESOURCE ? "Resource fork" : "Not forked";
    this.fileSystem = (FsProdos) parentFile.getFileSystem ();

    this.storageType = storageType;
    this.keyPtr = keyPtr;
    this.size = size;
    this.eof = eof;

    List<Integer> blockNos = new ArrayList<> ();
    AppleBlock dataBlock = fileSystem.getBlock (keyPtr);

    if (dataBlock.isValid ())
      switch (storageType)
      {
        case FsProdos.SEEDLING:
          blockNos.add (keyPtr);
          break;

        case FsProdos.SAPLING:
          blockNos.addAll (readIndex (keyPtr));
          break;

        case FsProdos.TREE:
          for (Integer indexBlock : readMasterIndex (keyPtr))
            if (fileSystem.getBlock (indexBlock).isValid ())
              blockNos.addAll (readIndex (indexBlock));
          break;

        case FsProdos.PASCAL_ON_PROFILE:
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
  public FileProdos getParentFile ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFile;
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
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    if (data == null)
      data = fileSystem.readBlocks (dataBlocks);

    return data;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()
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
  public ForkType getForkType ()
  // ---------------------------------------------------------------------------------//
  {
    return forkType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("Size (blocks) ......... %04X  %<,7d%n", size));
    text.append (String.format ("Eof ................... %04X  %<,7d%n", eof));
    text.append (String.format ("Key ptr ............... %04X  %<,7d%n%n", keyPtr));
    text.append (String.format ("Parent ................ %s%n", parentFile.fileName));
    text.append (String.format ("File system ........... %s", fileSystem.fileSystemType));

    return Utility.rtrim (text);
  }
}
