package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class FileData extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  //  byte[] buffer;

  // ---------------------------------------------------------------------------------//
  FileData (FsData fs, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    fileName = "Raw data";
    fileTypeText = "DATA";
    //    this.buffer = buffer;
    dataRecord = new DataRecord (buffer, 0, buffer.length);
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public byte[] read ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return buffer;
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    //    return buffer.length;
    return dataRecord.length ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                            // in blocks
  // ---------------------------------------------------------------------------------//
  {
    //    return buffer.length / 512;
    return dataRecord.length () / 512;
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public String getCatalogLine ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return fileName;
  //  }

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
