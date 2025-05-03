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
        break;

      case FsDos.FILE_TYPE_APPLESOFT:
        buffer = dataBlocks.get (0).getBuffer ();
        eof = Utility.unsignedShort (buffer, 0) + 2;
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
        break;

      case FsDos.FILE_TYPE_S:                 // AEPRO1.DSK uses this
        eof = dataBlocks.size () * parentFileSystem.getBlockSize ();
        break;

      default:
        System.out.println (
            "Unexpected file type: " + getFileType () + " in " + getFileName ());
    }
  }

  // set eof for text files (size of file in bytes)
  // ---------------------------------------------------------------------------------//
  private int getTextFileEof ()
  // ---------------------------------------------------------------------------------//
  {
    // get last block
    AppleBlock dataBlock = dataBlocks.get (dataBlocks.size () - 1);
    byte[] buffer = dataBlock.getBuffer ();

    // set eof to maximum possible
    int blockSize = parentFileSystem.getBlockSize ();
    int ptr = blockSize;
    int eof = dataBlocks.size () * blockSize;

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
    Buffer fileBuffer = getFileBuffer ();
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
    Buffer fileBuffer = getFileBuffer ();
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

    // Call-apple title files
    if (getFileType () == FsDos.FILE_TYPE_TEXT && eof == 1 && stupidName ())
      return false;

    return catalogEntry.isNameValid && dataBlocks.size () > 0;
  }

  // ---------------------------------------------------------------------------------//
  private boolean stupidName ()
  // ---------------------------------------------------------------------------------//
  {
    if (getFileName ().startsWith ("+=") || getFileName ().startsWith ("[ "))
      return true;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  protected boolean checkName (String name)
  // ---------------------------------------------------------------------------------//
  {
    for (byte b : name.getBytes ())
      if (b == (byte) 0x88)
        return false;

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
