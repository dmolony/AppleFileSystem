package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class TextBlockDos extends TextBlock
// -----------------------------------------------------------------------------------//
{
  private int gcd;

  // ---------------------------------------------------------------------------------//
  public TextBlockDos (AppleFileSystem fs, List<AppleBlock> blocks, int startBlockNo)
  // ---------------------------------------------------------------------------------//
  {
    super (fs, blocks, startBlockNo);

    firstLogicalByte = startBlockNo * fs.getBlockSize ();

    buildRecords ();
  }

  // ---------------------------------------------------------------------------------//
  private void buildRecords ()
  // ---------------------------------------------------------------------------------//
  {
    getBuffer ();

    int ptr = 0;
    boolean inData = false;
    int startPtr = -1;

    while (ptr < buffer.length)
    {
      if (buffer[ptr] == 0)
      {
        if (inData)
        {
          inData = false;
          records.add (new TextRecord (startPtr, ptr - startPtr));
        }
      }
      else if (!inData)
      {
        inData = true;
        startPtr = ptr;
      }

      ptr++;
    }

    if (inData)
      records.add (new TextRecord (startPtr, ptr - startPtr));

    for (TextRecord record : records)
    {
      ptr = record.offset () + firstLogicalByte;
      gcd = gcd == 0 ? ptr : Utility.gcd (gcd, ptr);
    }
  }

  // ---------------------------------------------------------------------------------//
  public int getProbableRecordLength ()
  // ---------------------------------------------------------------------------------//
  {
    return gcd;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("Probable record length ... %,7d%n%n", gcd));

    return text.toString ();
  }
}
