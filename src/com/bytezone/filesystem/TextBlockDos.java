package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------------//
public class TextBlockDos extends TextBlock
// -----------------------------------------------------------------------------------//
{

  private List<Record> records = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  public TextBlockDos (AppleFileSystem fs, List<AppleBlock> blocks, int startBlockNo)
  // ---------------------------------------------------------------------------------//
  {
    super (fs, blocks, startBlockNo);

    int blockSize = fs.getBlockSize ();
    firstLogicalByte = startBlockNo * blockSize;

    //    System.out.println (getText ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    getBuffer ();
    //    System.out.println (Utility.format (buffer));

    boolean inData = false;
    int logicalPtr = firstLogicalByte;
    int recordStart = -1;

    for (int i = 0; i < buffer.length; i++)
    {
      if (buffer[i] == 0x00)
      {
        if (inData)
        {
          inData = false;
          Record record = new Record (recordStart, i - recordStart);
          records.add (record);
        }
      }
      else
      {
        if (!inData)
        {
          inData = true;
          recordStart = i;
          //          System.out.printf ("%06X : ", logicalPtr);
          text.append (String.format ("%06X : ", logicalPtr));
        }

        //        System.out.print ((char) (buffer[i] & 0x7F));
        text.append ((char) (buffer[i] & 0x7F));
      }

      ++logicalPtr;
    }

    if (inData)
    {
      Record rec = new Record (recordStart, buffer.length - recordStart);
      records.add (rec);
    }

    //    System.out.println ();

    //    for (Record record : records)
    //      System.out.println (record);

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("Total records ............ %,7d%n", records.size ()));

    return text.toString ();
  }

  record Record (int offset, int length)
  {
  };

}
