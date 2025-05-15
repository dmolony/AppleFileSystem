package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatMeta;

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
  protected int textFileGaps;       // total sparse file empty data sectors
  protected int wastedBlocks;

  protected List<AppleBlock> indexBlocks = new ArrayList<> ();
  private final List<TextBlock> textBlocks = new ArrayList<> ();

  protected CatalogEntryDos catalogEntry;
  protected int recordLength;

  // ---------------------------------------------------------------------------------//
  FileDos (FsDos fs)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);
  }

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

        if (textFileGaps > 0)             // random-access file
        {
          createTextBlocks (dataBlocks);
          break;
        }

        if (fileContainsZero ())          // random-access file
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

      case FsDos.FILE_TYPE_S:                 // AEPRO1.DSK uses this
        eof = dataBlocks.size () * parentFileSystem.getBlockSize ();
        break;

      default:
        System.out.println (
            "Unexpected file type: " + getFileType () + " in " + getFileName ());
    }
  }

  // some binary files are used as text files, which screws up the eof/load bytes
  // ---------------------------------------------------------------------------------//
  private void checkEof ()
  // ---------------------------------------------------------------------------------//
  {
    int blockSize = parentFileSystem.getBlockSize ();
    int maxEof = dataBlocks.size () * blockSize;

    if (false)
      if (eof > maxEof)
        System.out.printf ("%,9d %,9d  %s  %-20s %s%n", eof, maxEof, getFileTypeText (),
            getFileName (), parentFileSystem.getFileName ());

    if (eof > 0)
    {
      int blocksUsed = dataBlocks.size ();
      int blocksNeeded = (eof - 1) / blockSize + 1;
      wastedBlocks = blocksUsed - blocksNeeded;
    }
  }

  // set eof for text files (size of file in bytes)
  // ---------------------------------------------------------------------------------//
  private int getTextFileEof ()
  // ---------------------------------------------------------------------------------//
  {
    // remove any trailing nulls (valid files won't have any)
    while (dataBlocks.size () > 0 && dataBlocks.get (dataBlocks.size () - 1) == null)
    {
      dataBlocks.remove (dataBlocks.size () - 1);
      --textFileGaps;
    }

    if (dataBlocks.size () == 0)
      return 0;

    // get last block
    AppleBlock dataBlock = dataBlocks.get (dataBlocks.size () - 1);
    byte[] buffer = dataBlock.getBuffer ();

    // set eof to maximum possible
    int blockSize = parentFileSystem.getBlockSize ();
    int eof = dataBlocks.size () * blockSize;
    int ptr = blockSize;

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
    assert textFileGaps == 0;

    // test entire buffer (in case reclen > block size)
    Buffer fileBuffer = getRawFileBuffer ();
    byte[] buffer = fileBuffer.data ();
    int max = fileBuffer.max () - 2;            // avoid the last two bytes

    for (int i = fileBuffer.offset (); i < max; i++)
      if (buffer[i] == 0)
        return true;

    return false;
  }

  // Some text files on DISASM1.DSK contain a single zero two bytes before eof. They
  // are all assembler source files, so should not be counted as random-access files.
  // ---------------------------------------------------------------------------------//
  private boolean fileContainsTwoZeros ()
  // ---------------------------------------------------------------------------------//
  {
    assert textFileGaps == 0;

    // test (up to) the entire buffer (in case reclen > block size)
    Buffer fileBuffer = getRawFileBuffer ();
    byte[] buffer = fileBuffer.data ();
    int max = fileBuffer.max ();
    int totZeros = 0;

    for (int i = fileBuffer.offset (); i < max; i++)
      if (buffer[i] == 0 && ++totZeros > 1)
        return true;

    return false;
  }

  // file is random-access
  // ---------------------------------------------------------------------------------//
  void createTextBlocks (List<AppleBlock> dataBlocks)
  // ---------------------------------------------------------------------------------//
  {
    // collect contiguous data blocks into TextBlocks
    List<AppleBlock> contiguousBlocks = new ArrayList<> ();      // temporary storage
    int startBlock = -1;

    int logicalBlockNo = 0;                           // block # within the file

    for (AppleBlock dataBlock : dataBlocks)
    {
      if (dataBlock == null)
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

    // calculate likely record length
    for (TextBlock textBlock : textBlocks)
      for (TextRecord record : textBlock)
      {
        int ptr = record.offset () + textBlock.firstByteNumber;
        recordLength = recordLength == 0 ? ptr : Utility.gcd (recordLength, ptr);
      }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Buffer getFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
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
          exactFileBuffer = rawFileBuffer;
        else
          exactFileBuffer = new Buffer (buffer, 4, eof - 4);
        break;

      default:
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
  public int getProbableRecordLength ()
  // ---------------------------------------------------------------------------------//
  {
    return recordLength;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                      // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return indexBlocks.size () + dataBlocks.size () - textFileGaps;
  }

  // This is used by the disk display to highlight the blocks that are relevant to
  // this file.
  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getAllBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    List<AppleBlock> blocks = new ArrayList<AppleBlock> (dataBlocks);
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
    return dataBlocks.size () - textFileGaps;
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

  // attempt to weed out the catalog entries that are just labels
  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isValidFile ()
  // ---------------------------------------------------------------------------------//
  {
    // Beagle Brothers "applesoft program"
    if (getFileType () == FsDos.FILE_TYPE_APPLESOFT && eof <= 3
        && getFileName ().startsWith ("  "))
      return false;

    // empty text files
    if (getFileType () == FsDos.FILE_TYPE_TEXT && eof <= 1)
      return false;

    return catalogEntry.isNameValid && dataBlocks.size () > 0
        && catalogEntry.sectorCount > 0;
  }

  // ---------------------------------------------------------------------------------//
  protected boolean checkName (String name)
  // ---------------------------------------------------------------------------------//
  {
    if (name.length () == 0)
      return false;

    for (byte b : name.getBytes ())
    {
      int val = b & 0x7F;
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

  // ---------------------------------------------------------------------------------//
  protected String getAddressText ()
  // ---------------------------------------------------------------------------------//
  {
    int loadAddress = getLoadAddress ();
    return loadAddress == 0 || loadAddress == 0x801 ? ""
        : String.format ("$%4X", loadAddress);
  }

  // ---------------------------------------------------------------------------------//
  protected String getLengthText ()
  // ---------------------------------------------------------------------------------//
  {
    return getFileLength () == 0 ? "" : String.format ("$%5X %<,7d", getFileLength ());
  }

  // ---------------------------------------------------------------------------------//
  protected String getCatalogMessage ()
  // ---------------------------------------------------------------------------------//
  {
    int actualSize = getTotalIndexSectors () + getTotalDataSectors ();
    int maxDataSize = getTotalDataSectors () * 256;
    int fileType = getFileType ();

    StringBuilder message = new StringBuilder ();

    if (recordLength > 0)
      addMessage (message, String.format ("Reclen = %,d ?", recordLength));

    if (getSectorCount () != actualSize && getTotalDataSectors () > 0)
      addMessage (message, String.format ("Actual size: %03d", actualSize));

    if (getSectorCount () > 999)
      addMessage (message, " - Reported " + getSectorCount ());

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

    AppleBlock catalogEntryBlock = catalogEntry.catalogBlock;

    formatMeta (text, "Index blocks", 4, indexBlocks.size ());
    formatMeta (text, "Locked", catalogEntry.isLocked ? "true" : "false");
    formatMeta (text, "Catalog track", 2, catalogEntryBlock.getTrackNo ());
    formatMeta (text, "Catalog sector", 2, catalogEntryBlock.getSectorNo ());
    formatMeta (text, "Catalog slot #", 2, catalogEntry.slot);
    formatMeta (text, "Sectors", 4, catalogEntry.sectorCount);
    formatMeta (text, "Load address", 4, loadAddress);

    if (isRandomAccess ())
    {
      formatMeta (text, "Text file gaps", 4, textFileGaps);
      formatMeta (text, "Text blocks", 4, textBlocks.size ());
      formatMeta (text, "Possible reclen", 4, recordLength);
    }

    return Utility.rtrim (text);
  }
}
