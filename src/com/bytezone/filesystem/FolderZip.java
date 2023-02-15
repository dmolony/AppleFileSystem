package com.bytezone.filesystem;

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
    return String.format ("%-30s ZIPDIR", fileName);
  }
}
