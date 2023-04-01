package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileData extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  byte[] buffer;

  // ---------------------------------------------------------------------------------//
  FileData (FsData fs, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    //    isFile = true;
    fileName = "Raw data";
    fileTypeText = "DATA";
    this.buffer = buffer;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return buffer;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return buffer.length;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                            // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return buffer.length / 512;
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

    text.append (String.format ("Length ................ %,d", buffer.length));

    return Utility.rtrim (text);
  }
}
