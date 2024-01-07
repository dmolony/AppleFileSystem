package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.filesystem.FileProdos.ForkType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class ForkProdos extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private static Locale US = Locale.US;          // to force 3 character months
  protected static final DateTimeFormatter sdf =
      DateTimeFormatter.ofPattern ("d-LLL-yy", US);
  protected static final DateTimeFormatter stf = DateTimeFormatter.ofPattern ("H:mm");
  protected static final String NO_DATE = "<NO DATE>";

  final FileProdos parentFile;
  final FsProdos fileSystem;
  final ForkType forkType;

  final int storageType;
  final String storageTypeText;
  final int size;
  final int eof;
  final int keyPtr;

  private AppleBlock masterIndexBlock;
  private final List<AppleBlock> indexBlocks = new ArrayList<> ();
  private final List<AppleBlock> dataBlocks = new ArrayList<> ();

  private byte[] data;

  // All ForkProdos files have a single FileProdos parent. Forks are also AppleFiles,
  // but only the DATA and RESOURCE forks are treated as standalone files. Normal
  // prodos files simply use a ForkProdos for their data (as the code to read them 
  // is identical.
  // DATA and RESOURCE forks are stored as children of the FileProdos, normal
  // prodos files keep a private reference to its data 'fork'.
  // ---------------------------------------------------------------------------------//
  ForkProdos (FileProdos parentFile, ForkType forkType, int keyPtr, int storageType,
      int size, int eof)
  // ---------------------------------------------------------------------------------//
  {
    super (parentFile.getParentFileSystem ());

    isFork = forkType != null;

    fileType = parentFile.getFileType ();
    fileTypeText = parentFile.getFileTypeText ();

    this.parentFile = parentFile;
    this.forkType = forkType;
    this.fileName = forkType == ForkType.DATA ? "Data fork"
        : forkType == ForkType.RESOURCE ? "Resource fork" : "Not forked";
    this.fileSystem = (FsProdos) parentFile.getParentFileSystem ();

    this.storageType = storageType;
    this.keyPtr = keyPtr;
    this.size = size;
    this.eof = eof;

    storageTypeText = ProdosConstants.storageTypes[storageType];

    List<Integer> blockNos = new ArrayList<> ();
    AppleBlock dataBlock = fileSystem.getBlock (keyPtr);
    dataBlock.setBlockType (BlockType.FS_DATA);

    if (dataBlock.isValid ())
      switch (storageType)
      {
        case ProdosConstants.SEEDLING:
          //          dataBlock.setBlockType (BlockType.FILE_DATA);
          blockNos.add (keyPtr);
          break;

        case ProdosConstants.SAPLING:
          blockNos.addAll (readIndex (keyPtr));
          break;

        case ProdosConstants.TREE:
          for (Integer indexBlock : readMasterIndex (keyPtr))
          {
            AppleBlock block = fileSystem.getBlock (indexBlock);
            if (block.isValid ())
            {
              block.setBlockType (BlockType.FS_DATA);
              blockNos.addAll (readIndex (indexBlock));
            }
          }
          break;

        case ProdosConstants.PASCAL_ON_PROFILE:
          for (int i = keyPtr; i < fileSystem.getTotalBlocks (); i++)
            blockNos.add (i);
          break;

        default:
          System.out.printf ("Impossible storage type: %02X%n", storageType);
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
      {
        AppleBlock block = fileSystem.getBlock (blockNo);
        block.setBlockType (BlockType.FILE_DATA);
        dataBlocks.add (block);
      }
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
      AppleBlock indexBlock = fileSystem.getBlock (blockPtr);
      indexBlock.setBlockType (BlockType.FS_DATA);
      indexBlocks.add (indexBlock);

      byte[] buffer = indexBlock.read ();

      for (int i = 0; i < 256; i++)
      {
        int blockNo = (buffer[i] & 0xFF) | ((buffer[i + 0x100] & 0xFF) << 8);
        if (blockNo > 0)
        {
          AppleBlock dataBlock = fileSystem.getBlock (blockNo);
          dataBlock.setBlockType (BlockType.FILE_DATA);
          blocks.add (dataBlock.isValid () ? blockNo : 0);      // should throw error
        }
      }
    }

    return blocks;
  }

  // ---------------------------------------------------------------------------------//
  private List<Integer> readMasterIndex (int keyPtr)
  // ---------------------------------------------------------------------------------//
  {
    AppleBlock indexBlock = fileSystem.getBlock (keyPtr);
    indexBlock.setBlockType (BlockType.FS_DATA);
    masterIndexBlock = indexBlock;
    indexBlocks.add (indexBlock);

    byte[] buffer = masterIndexBlock.read ();             // master index

    int highest = 0x80;
    while (highest-- > 0)                                 // decrement after test
      if (buffer[highest] != 0 || buffer[highest + 0x100] != 0)
        break;

    List<Integer> blocks = new ArrayList<> (highest + 1);
    for (int i = 0; i <= highest; i++)
    {
      int blockNo = (buffer[i] & 0xFF) | ((buffer[i + 256] & 0xFF) << 8);
      if (blockNo > 0)
      {
        AppleBlock dataBlock = fileSystem.getBlock (blockNo);
        dataBlock.setBlockType (BlockType.FS_DATA);
        blocks.add (dataBlock.isValid () ? blockNo : 0);
      }
    }

    return blocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    // maybe this routine should always declare the buffer and pass it to read()
    if (data == null)
    {
      data = fileSystem.readBlocks (dataBlocks);

      if (data.length < eof)
      {
        // see TOTAL.REPLAY/X/COLUMNS/COL2P/COLUMNS.MGEMS
        System.out.printf ("Buffer not long enough in %s%n", parentFile.getPath ());
        System.out.printf ("EOF: %06X, buffer length: %06X%n", eof, data.length);
        byte[] temp = new byte[eof];
        System.arraycopy (data, 0, temp, 0, data.length);
        data = temp;
      }
    }

    return data;
  }

  // ---------------------------------------------------------------------------------//
  public int getAuxType ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFile.getAuxType ();
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
  @Override
  public ForkType getForkType ()
  // ---------------------------------------------------------------------------------//
  {
    return forkType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    LocalDateTime created = parentFile.getCreated ();
    LocalDateTime modified = parentFile.getModified ();

    int fileLength = isForkedFile () ? 0 : getFileLength ();

    String dateCreated = created == null ? NO_DATE : created.format (sdf);
    String timeCreated = created == null ? "" : created.format (stf);
    String dateModified = modified == null ? NO_DATE : modified.format (sdf);
    String timeModified = modified == null ? "" : modified.format (stf);

    String forkFlag = isForkedFile () ? "+" : " ";

    text.append (String.format ("%s%-15s %3s%s  %5d  %9s %5s  %9s %5s %8d %7s%n",
        isLocked () ? "*" : " ", getFileName (), getFileTypeText (), forkFlag,
        getTotalBlocks (), dateModified, timeModified, dateCreated, timeCreated,
        fileLength, getSubType ()));

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  private String getSubType ()
  // ---------------------------------------------------------------------------------//
  {
    switch (getFileType ())
    {
      case ProdosConstants.FILE_TYPE_TEXT:
        return String.format ("R=%5d", parentFile.getAuxType ());

      case ProdosConstants.FILE_TYPE_BINARY:
      case ProdosConstants.FILE_TYPE_PNT:
      case ProdosConstants.FILE_TYPE_PIC:
      case ProdosConstants.FILE_TYPE_FOT:
        return String.format ("A=$%4X", parentFile.getAuxType ());
    }

    return "";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    if (isFork)         // an actual fork, not the default data for a FileProdos
    {
      text.append (String.format ("File name ............. %s%n", parentFile.fileName));
      text.append (
          String.format ("File system type ...... %s%n%n", fileSystem.fileSystemType));
      text.append (String.format ("Storage type .......... %02X  %s%n", storageType,
          storageTypeText));
      text.append (String.format ("Key ptr ............... %04X    %<,7d%n%n", keyPtr));
      text.append (String.format ("Size (blocks) ......... %04X    %<,7d%n", size));
    }

    String message =
        dataBlocks.size () * 512 < eof ? message = "<-- past data blocks" : "";

    text.append (
        String.format ("Index blocks .......... %04X    %<,7d%n", indexBlocks.size ()));
    text.append (
        String.format ("Data blocks ........... %04X    %<,7d%n", dataBlocks.size ()));
    text.append (
        String.format ("Eof ................... %06X  %<,7d  %s%n", eof, message));

    return Utility.rtrim (text);
  }
}
