package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public interface AppleFilePath
// -----------------------------------------------------------------------------------//
{
  public char getSeparator ();

  public String[] getPathFolders ();

  public String getFullFileName ();
}
