package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FileZip extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private final char separator = '/';

  // ---------------------------------------------------------------------------------//
  FileZip (FsZip fs, byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFile = true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    int pos = fileName.lastIndexOf (separator);
    return pos < 0 ? fileName : fileName.substring (pos + 1);
  }

  // ---------------------------------------------------------------------------------//
  public String getFullFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String[] getPathFolders ()
  // ---------------------------------------------------------------------------------//
  {
    String[] pathItems = fileName.split ("\\" + separator);
    String[] pathFolders = new String[pathItems.length - 1];

    for (int i = 0; i < pathFolders.length; i++)
      pathFolders[i] = pathItems[i];

    return pathFolders;
  }

  // ---------------------------------------------------------------------------------//
  public char getSeparator ()
  // ---------------------------------------------------------------------------------//
  {
    return separator;
  }
}
