package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class FileData extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  FileData (FsData fs, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    this (fs, new Buffer (buffer, 0, buffer.length));
  }

  // ---------------------------------------------------------------------------------//
  FileData (FsData fs, Buffer dataRecord)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    fileName = "Raw data";
    fileTypeText = "DATA";
    this.dataRecord = dataRecord;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return dataRecord.length ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                            // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return dataRecord.length () / 512;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    //    text.append (String.format ("Length ................ %,d", buffer.length));
    text.append (String.format ("Length ................ %,d", dataRecord.length ()));

    return Utility.rtrim (text);
  }
}
