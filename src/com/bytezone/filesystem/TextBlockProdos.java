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
  private int firstLogicalRecordNo;     // first possible record number
  private int offsetToFirstRecord;      // skip incomplete record if present

  // ---------------------------------------------------------------------------------//
  public TextBlockProdos (FsProdos fs, List<AppleBlock> blocks, int startBlockNo,
      int recordLength)
  // ---------------------------------------------------------------------------------//
  {
    super (fs, blocks, startBlockNo);

    this.recordLength = recordLength;

    firstLogicalByte = startBlockNo * fs.getBlockSize ();

    int skipped = firstLogicalByte % recordLength;
    offsetToFirstRecord = skipped == 0 ? 0 : recordLength - skipped;
    firstLogicalRecordNo = (firstLogicalByte + offsetToFirstRecord) / recordLength;

    buildRecords ();
  }

  // ---------------------------------------------------------------------------------//
  private void buildRecords ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = offsetToFirstRecord;
    getBuffer ();

    while (ptr < buffer.length)
    {
      if (buffer[ptr] != 0)                         // a valid record
      {
        int ptr2 = ptr;

        while (buffer[ptr2++] != 0)                 // in data
          ;

        records.add (new TextRecord (ptr, ptr2 - ptr + 1));
      }

      ptr += recordLength;                          // next record
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (
        String.format ("Record length ............ %04X  %<,9d%n", recordLength));
    text.append (
        String.format ("Offset to first record ... %04X  %<,9d%n", offsetToFirstRecord));
    text.append (
        String.format ("First logical record # ... %04X  %<,9d%n", firstLogicalRecordNo));

    return text.toString ();
  }
}
