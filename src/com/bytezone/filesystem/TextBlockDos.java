package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
public class TextBlockDos extends TextBlock
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  public TextBlockDos (AppleFileSystem fs, AppleFile appleFile, List<AppleBlock> blocks,
      int startBlockNo)
  // ---------------------------------------------------------------------------------//
  {
    super (fs, appleFile, blocks, startBlockNo);

    buildRecords ();
  }

  // ---------------------------------------------------------------------------------//
  private void buildRecords ()
  // ---------------------------------------------------------------------------------//
  {
    getBuffer ();

    int ptr = 0;
    int startPtr = -1;
    boolean inData = false;

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
