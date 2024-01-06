package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

class File2img extends AbstractAppleFile
{
  List<AppleBlock> dataBlocks = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  File2img (Fs2img fs, String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    fileTypeText = "2img";
    fileName = name;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getBlocks ()
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
}
