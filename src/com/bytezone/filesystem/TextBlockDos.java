package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
public class TextBlockDos extends TextBlock
// -----------------------------------------------------------------------------------//
{
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
  }
}
