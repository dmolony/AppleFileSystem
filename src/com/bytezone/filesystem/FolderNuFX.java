package com.bytezone.filesystem;

public class FolderNuFX extends AbstractAppleFile
{
  // ---------------------------------------------------------------------------------//
  public FolderNuFX (FsNuFX parent, String name)
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
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (String.format ("File type ............. %s", "NuFX Folder"));

    return text.toString ();
  }
}
