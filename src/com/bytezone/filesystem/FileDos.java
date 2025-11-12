package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.TextBlock.TextRecord;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public abstract class FileDos extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  static final int ENTRY_SIZE = 0x23;
  static final int HEADER_SIZE = 0x0B;

  protected int eof;
  protected int loadAddress;
  protected int wastedBlocks;

  protected List<AppleBlock> indexBlocks = new ArrayList<> ();
  private final List<TextBlock> textBlocks = new ArrayList<> ();

  protected CatalogEntryDos catalogEntry;
  protected int recordLength;

  int shortestRecord = 99999;
  int longestRecord = 0;
  int totalRecords = 0;
  int totalRecordsOutsideRange = 0;

  // ---------------------------------------------------------------------------------//
  FileDos (FsDos fs)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);
  }

  // Integer, Applesoft and Binary files store the eof in the file itself. Text files
  // can be either a sequence of char values terminated with a CR, or they can be a
  // collection of fixed-length random-access records with zeroes in between.
  // Random-access files are often corrupt with records overflowing, or error messages
  // being written to the file by accident. The record length is not stored anywhere,
  // so it's all mostly guesswork. And who knows what people have done with the other
  // 'unused' file types.
  // ---------------------------------------------------------------------------------//
  protected void setFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    if (dataBlocks.size () == 0)
    {
      eof = 0;
      return;
    }

    switch (getFileType ())
    {
      case FsDos.FILE_TYPE_TEXT:
        eof = getTextFileEof ();
        if (eof == 0)
          break;

        if (fileGaps > 0 || fileContainsZero ())             // random-access file
          createTextBlocks (dataBlocks);

        break;

      case FsDos.FILE_TYPE_INTEGER_BASIC:
        byte[] buffer = dataBlocks.get (0).getBuffer ();
        eof = Utility.unsignedShort (buffer, 0) + 2;
        checkEof ();
        break;

      case FsDos.FILE_TYPE_APPLESOFT:
        buffer = dataBlocks.get (0).getBuffer ();
        eof = Utility.unsignedShort (buffer, 0) + 2;
        checkEof ();
        if (eof > 6)
          loadAddress = Utility.getApplesoftLoadAddress (buffer);
        break;

      case FsDos.FILE_TYPE_BINARY:
      case FsDos.FILE_TYPE_RELOCATABLE:       // Applesoft Toolkit APA and HRCG
      case FsDos.FILE_TYPE_BINARY_B:
      case FsDos.FILE_TYPE_BINARY_L:          // Dos4 uses this
        buffer = dataBlocks.get (0).getBuffer ();
        loadAddress = Utility.unsignedShort (buffer, 0);
        eof = Utility.unsignedShort (buffer, 2) + 4;
        checkEof ();
        break;

      case FsDos.FILE_TYPE_S:                 // AEPRO1.DSK (Ascii Express) uses this
        eof = dataBlocks.size () * parentFileSystem.getBlockSize ();
        checkEof ();
        break;

      default:
        System.out.println (
            "Unexpected file type: " + getFileType () + " in " + getFileName ());
    }
  }

  // Some binary files contain text data, which screws up the eof/load bytes.
  // I have no idea how this happens, but it does. Presumably by programs manipulating
  // the T/S index.
  // ---------------------------------------------------------------------------------//
  private void checkEof ()
  // ---------------------------------------------------------------------------------//
  {
    if (eof > 0)
    {
      int blocksNeeded = (eof - 1) / parentFileSystem.getBlockSize () + 1;
      wastedBlocks = dataBlocks.size () - blocksNeeded;
    }
  }

  // Set eof for text files (size of file in bytes).
  // ---------------------------------------------------------------------------------//
  private int getTextFileEof ()
  // ---------------------------------------------------------------------------------//
  {
    // remove any trailing nulls (valid files won't have any)
    while (dataBlocks.size () > 0 && dataBlocks.get (dataBlocks.size () - 1) == null)
    {
      dataBlocks.remove (dataBlocks.size () - 1);
      --fileGaps;
    }

    if (dataBlocks.size () == 0)
      return 0;

    // get last data block
    AppleBlock dataBlock = dataBlocks.get (dataBlocks.size () - 1);
    byte[] buffer = dataBlock.getBuffer ();

    int ptr = parentFileSystem.getBlockSize ();       // block size
    int eof = dataBlocks.size () * ptr;               // maximum possible

    // decrement eof for each trailing zero
    while (--ptr >= 0 && buffer[ptr] == 0)
      --eof;

    return eof;
  }

  // NB zardax files seem to use 0 as eof
  // Some text files on DISASM1.DSK contain a single zero two bytes before eof. They
  // are all assembler source files, so should not be counted as random-access files.
  // ---------------------------------------------------------------------------------//
  private boolean fileContainsZero ()
  // ---------------------------------------------------------------------------------//
  {
    assert fileGaps == 0;

    // test entire buffer (in case reclen > block size)
    Buffer rawBuffer = getRawFileBuffer ();
    byte[] buffer = rawBuffer.data ();

    int max = (eof > 0 ? eof : rawBuffer.max ()) - 2;      // ignore the last two bytes

    for (int i = rawBuffer.offset (); i < max; i++)
      if (buffer[i] == 0)
        return true;

    return false;
  }

  // Random Access files can have large gaps between records, so keeping a list of
  // consecutive data blocks is not feasible. Text Blocks are groups of contiguous
  // data blocks (essentially islands of data in the file). A Random Access file
  // can have any number of Text Blocks, but most will have only one as the records
  // are all at the start of the file (and usually consecutive).
  // ---------------------------------------------------------------------------------//
  void createTextBlocks (List<AppleBlock> dataBlocks)
  // ---------------------------------------------------------------------------------//
  {
    List<AppleBlock> contiguousBlocks = new ArrayList<> ();    // current data island

    int startBlock = -1;                          // island's first logical block #
    int logicalBlockNo = 0;                       // block # within the file

    for (AppleBlock dataBlock : dataBlocks)
    {
      if (dataBlock == null)                          // gap between islands
      {
        if (contiguousBlocks.size () > 0)
        {
          TextBlockDos textBlock =
              new TextBlockDos (parentFileSystem, this, contiguousBlocks, startBlock);
          textBlocks.add (textBlock);
          contiguousBlocks = new ArrayList<> ();      // ready for a new island
        }
      }
      else
      {
        if (contiguousBlocks.size () == 0)            // this is the start of an island
          startBlock = logicalBlockNo;
        contiguousBlocks.add (dataBlock);
      }

      ++logicalBlockNo;
    }

    assert contiguousBlocks.size () > 0;
    if (contiguousBlocks.size () > 0)                 // should always be true
    {
      TextBlockDos textBlock =
          new TextBlockDos (parentFileSystem, this, contiguousBlocks, startBlock);
      textBlocks.add (textBlock);
    }

    analyseRecords ();

    if ((recordLength < 4 || recordLength > 1000)     // probably not random-access
        && fileGaps == 0)                             // 
      textBlocks.clear ();                            // treat file as normal text
  }

  // ---------------------------------------------------------------------------------//
  private void analyseRecords ()
  // ---------------------------------------------------------------------------------//
  {
    // calculate likely record length
    for (TextBlock textBlock : textBlocks)
      for (TextRecord record : textBlock)
      {
        int ptr = record.offset () + textBlock.firstByteNumber;
        recordLength = recordLength == 0 ? ptr : Utility.gcd (recordLength, ptr);
        shortestRecord = Math.min (shortestRecord, record.length ());
        longestRecord = Math.max (longestRecord, record.length ());
        ++totalRecords;
      }

    // count records longer than GCD
    for (TextBlock textBlock : textBlocks)
      for (TextRecord record : textBlock)
        if (record.length () > recordLength)
          ++totalRecordsOutsideRange;

    if (false)
      showTotals ();
  }

  // ---------------------------------------------------------------------------------//
  private void showTotals ()
  // ---------------------------------------------------------------------------------//
  {
    System.out.printf ("Shortest: %3d, Longest: %3d, GCD: %3d.  Total: %5d (%d)%n",
        shortestRecord, longestRecord, recordLength, totalRecords,
        totalRecordsOutsideRange);
  }

  // Try to convert the filesystem's raw buffer into a more precise buffer. This means
  // removing the eof and load address from A/I/B files, and terminating the file at
  // the eof.
  // ---------------------------------------------------------------------------------//
  @Override
  public Buffer getFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    assert !isRandomAccess ();

    if (exactFileBuffer != null)
      return exactFileBuffer;

    getRawFileBuffer ();
    byte[] buffer = rawFileBuffer.data ();

    switch (getFileType ())
    {
      case FsDos.FILE_TYPE_TEXT:
        if (isRandomAccess ())
          throw new UnsupportedOperationException (
              "getFileBuffer() not supported for random access files " + getFileName ());
        exactFileBuffer = new Buffer (buffer, 0, eof);
        break;

      case FsDos.FILE_TYPE_INTEGER_BASIC:
      case FsDos.FILE_TYPE_APPLESOFT:
        if (eof > rawFileBuffer.max ())
          exactFileBuffer = rawFileBuffer;
        else
          exactFileBuffer = new Buffer (buffer, 2, eof - 2);
        break;

      case FsDos.FILE_TYPE_BINARY:
      case FsDos.FILE_TYPE_RELOCATABLE:
      case FsDos.FILE_TYPE_BINARY_B:
      case FsDos.FILE_TYPE_BINARY_L:
        if (eof > rawFileBuffer.max ())
        {
          eof = getTextFileEof ();
          exactFileBuffer = new Buffer (buffer, 0, eof);
        }
        else
          exactFileBuffer = new Buffer (buffer, 4, eof - 4);
        break;

      case FsDos.FILE_TYPE_S:                 // AEPRO1.DSK uses this
        exactFileBuffer = new Buffer (buffer, 0, eof);
        break;

      default:
        System.out.println ("Impossible: " + getFileType ());
    }

    if (exactFileBuffer == null)
      exactFileBuffer = rawFileBuffer;

    return exactFileBuffer;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                       // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return eof;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isRandomAccess ()
  // ---------------------------------------------------------------------------------//
  {
    return textBlocks.size () > 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFileNameValid ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.isFileNameValid;
  }

  // ---------------------------------------------------------------------------------//
  public int getProbableRecordLength ()
  // ---------------------------------------------------------------------------------//
  {
    assert isRandomAccess ();

    return recordLength;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                      // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return indexBlocks.size () + dataBlocks.size () - fileGaps;
  }

  // This is used by the disk display to highlight the blocks that are relevant to
  // this file.
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
    blocks.add (catalogEntry.catalogBlock);

    return blocks;
  }

  // ---------------------------------------------------------------------------------//
  public int getLoadAddress ()
  // ---------------------------------------------------------------------------------//
  {
    return loadAddress;
  }

  // ---------------------------------------------------------------------------------//
  AppleBlock getCatalogBlock ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.catalogBlock;
  }

  // ---------------------------------------------------------------------------------//
  int getCatalogSlot ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.slot;
  }

  // ---------------------------------------------------------------------------------//
  public int getSectorCount ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.sectorCount;
  }

  // ---------------------------------------------------------------------------------//
  public int getTotalDataSectors ()
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size () - fileGaps;
  }

  // ---------------------------------------------------------------------------------//
  public int getTotalIndexSectors ()
  // ---------------------------------------------------------------------------------//
  {
    return indexBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.isLocked;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.fileType;
  }

  // ---------------------------------------------------------------------------------//
  public List<TextBlock> getTextBlocks ()
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
  public int getWastedBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return wastedBlocks;
  }

  // ---------------------------------------------------------------------------------//
  protected boolean checkName (String name)
  // ---------------------------------------------------------------------------------//
  {
    if (name.length () == 0)
      return false;

    for (byte b : name.getBytes ())
    {
      int val = b & 0xFF;
      if (val < 32 || val == 127)               // non-printable
        return false;
    }

    return true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void delete (boolean force)
  // ---------------------------------------------------------------------------------//
  {
    if (isLocked () && !force)
      throw new FileLockedException (String.format ("%s is locked", getFileName ()));

    catalogEntry.delete ();
    ((FsDos) parentFileSystem).remove (this, force);
  }

  // Used by Dos3 and Dos4 when building a catalog line.
  // ---------------------------------------------------------------------------------//
  protected String getAddressText ()
  // ---------------------------------------------------------------------------------//
  {
    int loadAddress = getLoadAddress ();
    return loadAddress == 0 || loadAddress == 0x801 ? ""
        : String.format ("$%4X", loadAddress);
  }

  // Used by Dos3 and Dos4 when building a catalog line.
  // ---------------------------------------------------------------------------------//
  protected String getLengthText ()
  // ---------------------------------------------------------------------------------//
  {
    return getFileLength () == 0 ? "" : String.format ("$%5X %<,7d", getFileLength ());
  }

  // Used by Dos3 and Dos4 when building a catalog line.
  // ---------------------------------------------------------------------------------//
  protected String getCatalogMessage ()
  // ---------------------------------------------------------------------------------//
  {
    int actualSize = getTotalIndexSectors () + getTotalDataSectors ();
    int maxDataSize = getTotalDataSectors () * 256;
    int fileType = getFileType ();

    StringBuilder message = new StringBuilder ();

    if (getSectorCount () != actualSize && getTotalDataSectors () > 0)
      addMessage (message, String.format ("Actual size: %03d", actualSize));

    if (getSectorCount () > 999)
      addMessage (message, "Reported " + getSectorCount ());

    if (isRandomAccess ())
      addMessage (message, String.format ("Random Access (%d)", recordLength));

    if (fileType != FsDos.FILE_TYPE_TEXT)
    {
      if (eof > maxDataSize)
        addMessage (message, String.format ("eof > max %,d", maxDataSize));

      if (wastedBlocks > 0)
        addMessage (message, String.format ("%d wasted sector%s", wastedBlocks,
            (wastedBlocks > 1) ? "s" : ""));
    }

    return Utility.rtrim (message);
  }

  // ---------------------------------------------------------------------------------//
  private void addMessage (StringBuilder message, String appendText)
  // ---------------------------------------------------------------------------------//
  {
    if (message.length () > 0)
      message.append (", ");

    message.append (appendText);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("\n");
    text.append (catalogEntry);

    formatText (text, "Index blocks", 4, indexBlocks.size ());
    formatText (text, "Load address", 4, loadAddress);

    if (isRandomAccess ())
    {
      text.append ("\n");
      text.append ("---- Random Access ----\n");
      formatText (text, "Text file gaps", 4, fileGaps);
      formatText (text, "Text blocks", 4, textBlocks.size ());
      formatText (text, "Possible reclen", 4, recordLength);
      formatText (text, "Shortest record", 4, shortestRecord);
      formatText (text, "Longest record", 4, longestRecord);
      formatText (text, "Total records", 4, totalRecords);
      formatText (text, "Total records long", 4, totalRecordsOutsideRange);
    }

    return Utility.rtrim (text);
  }
}
