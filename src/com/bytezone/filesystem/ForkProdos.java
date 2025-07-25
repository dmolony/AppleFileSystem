package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.filesystem.TextBlock.TextRecord;
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
  final ForkType forkType;

  final int storageType;
  final int size;
  final int eof;
  final int keyPtr;

  private AppleBlock masterIndexBlock;
  private final List<AppleBlock> indexBlocks = new ArrayList<> ();

  private final List<TextBlockProdos> textBlocks = new ArrayList<> ();

  // All ForkProdos files have a single FileProdos parent. Forks are also AppleFiles,
  // but only the DATA and RESOURCE forks are treated as standalone files. Normal
  // prodos files simply use a ForkProdos for their data (as the code to read them 
  // is identical).
  // DATA and RESOURCE forks are stored as children of the FileProdos, normal
  // prodos files keep a private reference to its data 'fork'.
  // ---------------------------------------------------------------------------------//
  ForkProdos (FileProdos parentFile, ForkType forkType, int keyPtr, int storageType,
      int size, int eof)
  // ---------------------------------------------------------------------------------//
  {
    super (parentFile.getParentFileSystem ());

    isFork = forkType != null;

    this.parentFile = parentFile;
    this.forkType = forkType;

    this.storageType = storageType;
    this.keyPtr = keyPtr;
    this.size = size;
    this.eof = eof;

    List<Integer> blockNumbers = new ArrayList<> ();
    AppleBlock dataBlock = parentFileSystem.getBlock (keyPtr, BlockType.FS_DATA);

    if (dataBlock != null)
      switch (storageType)
      {
        case ProdosConstants.SEEDLING:
          blockNumbers.add (keyPtr);
          dataBlock.setBlockType (BlockType.FILE_DATA);
          break;

        case ProdosConstants.SAPLING:
          blockNumbers.addAll (getSaplingBlocks (keyPtr));
          break;

        case ProdosConstants.TREE:
          blockNumbers.addAll (getTreeBlocks (keyPtr));
          break;

        case ProdosConstants.PASCAL_ON_PROFILE:
          // this file type has no index blocks, and the data blocks are all contiguous
          for (int i = keyPtr; i < parentFileSystem.getTotalBlocks (); i++)
            blockNumbers.add (i);
          break;

        default:
          System.out.printf ("Impossible storage type: %02X%n", storageType);
          break;
      }

    // remove trailing empty block numbers
    while (blockNumbers.size () > 0 && blockNumbers.get (blockNumbers.size () - 1) == 0)
    {
      blockNumbers.remove (blockNumbers.size () - 1);
      --fileGaps;
    }

    // fill data blocks (cannot call fileContainsZero () until this is done)
    for (Integer blockNo : blockNumbers)
    {
      if (blockNo > 0)
      {
        dataBlock = parentFileSystem.getBlock (blockNo);
        dataBlock.setBlockType (BlockType.FILE_DATA);
        dataBlock.setFileOwner (this);
        dataBlocks.add (dataBlock);
      }
      else
        dataBlocks.add (null);
    }

    // if eof is past the end of the listed blocks, add empty blocks
    while (eof > dataBlocks.size () * 512)
    {
      dataBlocks.add (null);
      ++fileGaps;
    }

    if (getFileType () == ProdosConstants.FILE_TYPE_TEXT      // text file
        && forkType != ForkType.RESOURCE                      // but not resource fork
        && parentFile.getAuxType () > 1                       // with reclen > 1
        && parentFile.getAuxType () < 1000                    // but not stupid
        && (fileGaps > 0 || fileContainsZero ()))             // random-access file
      createTextBlocks (blockNumbers);
  }

  // ---------------------------------------------------------------------------------//
  private void createTextBlocks (List<Integer> blockNumbers)
  // ---------------------------------------------------------------------------------//
  {
    // collect contiguous data blocks into TextBlocks
    List<AppleBlock> contiguousBlocks = new ArrayList<> ();      // temporary storage
    int startBlock = -1;
    int aux = parentFile.getAuxType ();

    int logicalBlockNo = 0;                           // block # within the file

    for (Integer blockNo : blockNumbers)
    {
      if (blockNo > 0)
      {
        if (contiguousBlocks.size () == 0)            // this is the start of an island
          startBlock = logicalBlockNo;

        contiguousBlocks.add (parentFileSystem.getBlock (blockNo));
      }
      else if (contiguousBlocks.size () > 0)
      {
        addNewTextBlock (contiguousBlocks, startBlock, aux);
        contiguousBlocks = new ArrayList<> ();        // ready for a new island
      }

      ++logicalBlockNo;
    }

    addNewTextBlock (contiguousBlocks, startBlock, aux);

    if (textBlocks.size () == 1 && !verifyTextBlocks ())
      textBlocks.clear ();
  }

  // ---------------------------------------------------------------------------------//
  private void addNewTextBlock (List<AppleBlock> contiguousBlocks, int startBlock,
      int aux)
  // ---------------------------------------------------------------------------------//
  {
    assert contiguousBlocks.size () > 0;
    TextBlockProdos textBlock = new TextBlockProdos ((FsProdos) parentFileSystem, this,
        contiguousBlocks, startBlock, aux);
    textBlocks.add (textBlock);
  }

  // this may not be required
  // ---------------------------------------------------------------------------------//
  private boolean verifyTextBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    //    System.out.printf ("%s  %,d%n", getFileName (), getAuxType ());
    //    System.out.printf ("Total blocks: %d%n", textBlocks.size ());

    int passed = 0;
    int failed = 0;
    int aux = getAuxType ();

    for (TextBlock textBlock : textBlocks)
    {
      byte[] buffer = textBlock.getBuffer ();
      for (TextRecord record : textBlock)
      {
        int ptr = record.offset () + aux - 1;
        if (ptr >= buffer.length)
          ptr = buffer.length - 1;

        if (buffer[ptr] == 0)
          ++passed;
        else
          ++failed;
        int ratio = record.length () / aux;
        //        System.out.printf ("Ratio: %d%n", ratio);
        if (ratio > 5)
          return false;
      }
    }

    //    System.out.printf ("Passed: %,5d%n", passed);
    //    System.out.printf ("Failed: %,5d%n", failed);

    return passed > failed;
  }

  // ---------------------------------------------------------------------------------//
  public FileProdos getParentFile ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFile;
  }

  // ---------------------------------------------------------------------------------//
  private List<Integer> getTreeBlocks (int keyPtr)
  // ---------------------------------------------------------------------------------//
  {
    List<Integer> blockNumbers = new ArrayList<> ();

    for (Integer indexBlock : readMasterIndex (keyPtr))
      if (indexBlock > 0)
      {
        AppleBlock block = parentFileSystem.getBlock (indexBlock, BlockType.FS_DATA);
        if (block != null)
          blockNumbers.addAll (getSaplingBlocks (indexBlock));
      }
      else
        for (int i = 0; i < 256; i++)
          blockNumbers.add (0);

    return blockNumbers;
  }

  // ---------------------------------------------------------------------------------//
  private List<Integer> getSaplingBlocks (int blockPtr)
  // ---------------------------------------------------------------------------------//
  {
    assert blockPtr > 0;

    List<Integer> blockNumbers = new ArrayList<> (256);
    AppleBlock indexBlock = parentFileSystem.getBlock (blockPtr, BlockType.FS_DATA);

    indexBlock.setBlockSubType ("INDEX");
    indexBlock.setFileOwner (this);
    indexBlocks.add (indexBlock);

    byte[] buffer = indexBlock.getBuffer ();

    for (int i = 0; i < 256; i++)
    {
      int blockNo = (buffer[i] & 0xFF) | ((buffer[i + 0x100] & 0xFF) << 8);
      if (blockNo > 0)
      {
        AppleBlock dataBlock = parentFileSystem.getBlock (blockNo, BlockType.FILE_DATA);
        blockNumbers.add (dataBlock == null ? 0 : blockNo);
      }
      else
      {
        blockNumbers.add (0);
        ++fileGaps;
      }
    }

    return blockNumbers;
  }

  // ---------------------------------------------------------------------------------//
  private List<Integer> readMasterIndex (int keyPtr)
  // ---------------------------------------------------------------------------------//
  {
    masterIndexBlock = parentFileSystem.getBlock (keyPtr, BlockType.FS_DATA);
    masterIndexBlock.setBlockSubType ("M-INDEX");
    masterIndexBlock.setFileOwner (this);

    byte[] buffer = masterIndexBlock.getBuffer ();             // master index

    int highest = 0x80;
    while (highest-- > 0)                                      // decrement after test
      if (buffer[highest] != 0 || buffer[highest + 0x100] != 0)
        break;

    List<Integer> indexBlockNumbers = new ArrayList<> (highest + 1);
    for (int i = 0; i <= highest; i++)
    {
      int blockNo = (buffer[i] & 0xFF) | ((buffer[i + 256] & 0xFF) << 8);
      if (blockNo > 0)
      {
        AppleBlock dataBlock = parentFileSystem.getBlock (blockNo, BlockType.FS_DATA);
        indexBlockNumbers.add (dataBlock == null ? 0 : blockNo);
      }
      else
      {
        indexBlockNumbers.add (0);
        fileGaps += 256;
      }
    }

    return indexBlockNumbers;
  }

  // ---------------------------------------------------------------------------------//
  private boolean fileContainsZero ()
  // ---------------------------------------------------------------------------------//
  {
    assert fileGaps == 0;

    // test entire buffer (in case reclen > block size)
    Buffer fileBuffer = getRawFileBuffer ();

    byte[] buffer = fileBuffer.data ();
    int max = fileBuffer.max ();

    for (int i = fileBuffer.offset (); i < max; i++)
      if (buffer[i] == 0)
        return true;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Buffer getFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    if (exactFileBuffer == null)
    {
      getRawFileBuffer ();
      byte[] data = rawFileBuffer.data ();

      // prodos doesn't store a block number in the index when the block is empty
      if (rawFileBuffer.length () < eof)      // pad the larger buffer with nulls
      {
        // see TOTAL.REPLAY/X/COLUMNS/COL2P/COLUMNS.MGEMS
        System.out.printf ("Buffer not long enough in %s%n", parentFile.getPath ());
        System.out.printf ("EOF: %06X, buffer length: %06X%n", eof, data.length);
        byte[] temp = new byte[eof];
        System.arraycopy (data, 0, temp, 0, data.length);
        data = temp;
      }

      exactFileBuffer = new Buffer (data, 0, eof == 0 ? data.length : eof);
    }

    return exactFileBuffer;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getAllBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    List<AppleBlock> blocks = new ArrayList<AppleBlock> ();

    for (AppleBlock block : dataBlocks)
      if (block != null)
        blocks.add (block);

    blocks.addAll (indexBlocks);

    if (masterIndexBlock != null)
      blocks.add (masterIndexBlock);

    return blocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return switch (forkType)
    {
      case DATA -> "Data fork";
      case RESOURCE -> "Resource fork";
      case null -> parentFile.getFileName ();
    };
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFile.getFileType ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isRandomAccess ()
  // ---------------------------------------------------------------------------------//
  {
    return textBlocks.size () > 0;
  }

  // ---------------------------------------------------------------------------------//
  public List<TextBlockProdos> getTextBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return textBlocks;
  }

  // ---------------------------------------------------------------------------------//
  public int getTotalTextBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return textBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFile.getFileTypeText ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFile.isLocked ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
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
        fileLength, getAuxText ()));

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  private String getAuxText ()
  // ---------------------------------------------------------------------------------//
  {
    int auxType = parentFile.getAuxType ();

    if (auxType == 0)
      return "";

    return getFileType () == ProdosConstants.FILE_TYPE_TEXT
        ? String.format ("R=%5d", auxType) : String.format ("A=$%4X", auxType);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    if (isFork)             // an actual fork, not the default data for a FileProdos
    {
      formatText (text, "File name", parentFile.getFileName ());
      formatText (text, "File system type",
          parentFileSystem.getFileSystemType ().toString ());
      formatText (text, "Storage type", 2, storageType,
          ProdosConstants.storageTypes[storageType]);
      formatText (text, "Key ptr", 4, keyPtr);
      formatText (text, "Size (blocks)", 4, size);
      formatText (text, "Text file gaps", 4, fileGaps);
      text.append ("\n");
    }

    int dataSize = dataBlocks.size () * 512;
    String message = dataSize < eof
        ? message = String.format ("<-- past data blocks (%,d)", dataSize) : "";
    if (eof == 0)
      message = "<-- zero";

    formatText (text, "Index blocks", 4, indexBlocks.size ());
    formatText (text, "Data blocks", 4, dataBlocks.size () - fileGaps);
    formatText (text, "File gaps", 4, fileGaps);
    formatText (text, "EOF", 6, eof, message);

    return Utility.rtrim (text);
  }
}
