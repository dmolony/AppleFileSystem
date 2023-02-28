package com.bytezone.filesystem;

public class FolderNuFX extends AbstractAppleFile
{

  // ---------------------------------------------------------------------------------//
  FolderNuFX (FsNuFX parent, String name)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    this.fileName = name;

    isFolder = true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-30s", fileName);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (
        String.format ("File type ............. %d  %s", fileType, fileTypeText));

    return text.toString ();
  }
}
