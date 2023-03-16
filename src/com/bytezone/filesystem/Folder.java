package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

public class Folder extends AbstractAppleFile
{
  // This class is not used by FileSystem, but may be used by other projects when
  // displaying NuFX/Zip/Gzip file contents (eg DiskBrowser2 and AppleFormat)
  // ---------------------------------------------------------------------------------//
  public Folder (AppleFileSystem parent, String name)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    this.fileName = name;

    isFolder = true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("File type ............. %s", "Folder"));

    return Utility.rtrim (text);
  }
}
