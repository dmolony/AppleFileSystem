package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FileZip extends AbstractAppleFile implements AppleFilePath
// -----------------------------------------------------------------------------------//
{
  byte[] buffer;
  private final char separator = '/';

  // ---------------------------------------------------------------------------------//
  FileZip (FsZip fs, String fileName, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    this.buffer = buffer;
    this.fileName = fileName;
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
  public char getSeparator ()
  // ---------------------------------------------------------------------------------//
  {
    return separator;
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
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    int pos = fileName.lastIndexOf (separator);
    return pos < 0 ? fileName : fileName.substring (pos + 1);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFullFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileName;
  }
}
