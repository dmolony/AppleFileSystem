package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
// A TextBlock is a list of consecutive disk blocks that make up a part of a prodos
// text file. It represents an island of records somewhere within the text file.
// -----------------------------------------------------------------------------------//
public class TextBlockProdos extends TextBlock
// -----------------------------------------------------------------------------------//
{
  private int recordLength;             // aux

  private int firstLogicalRecordNo;     // first complete record number
  private int offsetToFirstRecord;      // skip incomplete record if present
  private int maxRecords;               // possible # full records in this island
  private int totalRecords;             // # full records in this island with data

  // ---------------------------------------------------------------------------------//
  public TextBlockProdos (FsProdos fs, List<AppleBlock> blocks, int startBlockNo,
      int recordLength)
  // ---------------------------------------------------------------------------------//
  {
    super (fs, blocks, startBlockNo);

    this.recordLength = recordLength;

    int blockSize = fs.getBlockSize ();

    firstLogicalByte = startBlockNo * blockSize;
    int skipped = firstLogicalByte % recordLength;

    offsetToFirstRecord = skipped == 0 ? 0 : recordLength - skipped;
    firstLogicalRecordNo = (firstLogicalByte + offsetToFirstRecord) / recordLength;

    int dataSize = blocks.size () * blockSize - offsetToFirstRecord;
    maxRecords = dataSize / recordLength + 1;    // allow for partly filled record at the end

    // NB: if a full record would normally require an extra (partial) block, but the
    //     actual record fitted in at the end of the current block, then the actual
    //     records will be 1 more than calculated.
    // EG: if reclen is 500 (less than 1 block), then a single block could fit two
    //     records if the second record is 12 bytes or less.
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    getBuffer ();

    int ptr = offsetToFirstRecord;
    int recordNo = firstLogicalRecordNo;
    boolean showTextOffsets = true;
    int blockSize = fs.getBlockSize ();

    int firstLogicalByte = startBlockNo * blockSize;

    // check each full record in the island

    for (int i = 0; i < maxRecords; i++)
    {
      if (ptr >= buffer.length)
        break;

      if (buffer[ptr] != 0)
      {
        if (showTextOffsets)
          text.append (String.format ("%,10d %,9d  ", firstLogicalByte + ptr, recordNo));

        ++totalRecords;

        int ptr2 = ptr;
        int max = ptr + recordLength;

        while (ptr2 < max)
        {
          int val = buffer[ptr2++] & 0x7F;                   // strip hi-order bit

          if (val == 0)
            break;

          text.append ((char) val);
        }
      }

      ptr += recordLength;
      recordNo++;
    }

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    int skipped = firstLogicalByte % recordLength;
    int dataSize = blocks.size () * fs.getBlockSize () - offsetToFirstRecord;

    text.append (String.format ("Record length ............ %,6d%n", recordLength));
    text.append (String.format ("Skipped .................. %,6d%n", skipped));
    text.append (
        String.format ("Offset to first record ... %,6d%n", offsetToFirstRecord));
    text.append (
        String.format ("First logical record ..... %,6d%n", firstLogicalRecordNo));
    text.append (String.format ("Data size ................ %,6d%n", dataSize));
    text.append (String.format ("Max records .............. %,6d%n%n", maxRecords));
    text.append (String.format ("Total records ............ %,6d%n%n", totalRecords));

    return text.toString ();
  }
}
