package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FolderZip extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  FolderZip (FsZip fs, String name)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    this.fileName = name;

    isFolder = true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    return Utility.rtrim (text);
  }
}
