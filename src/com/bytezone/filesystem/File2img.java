package com.bytezone.filesystem;

import java.util.List;

class File2img extends AbstractAppleFile
{
  String fileName;

  // ---------------------------------------------------------------------------------//
  File2img (Fs2img fs, String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    fileName = name;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getDataBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return "2img";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return false;
  }
}
