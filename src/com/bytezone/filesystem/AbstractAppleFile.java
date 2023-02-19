package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.AppleFileSystem.FileSystemType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractAppleFile implements AppleFile
// -----------------------------------------------------------------------------------//
{
  protected AppleFileSystem appleFileSystem;  // format of the disk on which this file exists

  protected boolean isFile;
  protected boolean isFolder;
  protected boolean isFileSystem;

  protected String fileName;
  protected int fileType;
  protected String fileTypeText;

  protected final List<AppleFile> files = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  AbstractAppleFile (AppleFileSystem appleFileSystem)
  // ---------------------------------------------------------------------------------//
  {
    this.appleFileSystem = appleFileSystem;
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
  public boolean isFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return isFileSystem;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFolder ()
  // ---------------------------------------------------------------------------------//
  {
    return isFolder;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFile ()
  // ---------------------------------------------------------------------------------//
  {
    return isFile;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)    // if isDirectory() or isFileSystem()
  // ---------------------------------------------------------------------------------//
  {
    if (!isFolder () && !isFileSystem ())
      throw new UnsupportedOperationException ("cannot addFile()");

    files.add (file);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleFileSystem getFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFileSystem;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public FileSystemType getFileSystemType ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFileSystem.getFileSystemType ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()      // if isDirectory() or isFileSystem()
  // ---------------------------------------------------------------------------------//
  {
    if (!isFolder () && !isFileSystem ())
      throw new UnsupportedOperationException ("cannot getFiles() unless Folder or FileSystem");

    return files;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlockSize ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFileSystem.getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("read() not implemented");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("write() not implemented");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("getLength() not implemented");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("getSize() not implemented");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("getBlocks() not implemented");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String catalog ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (toString () + "\n");

    for (AppleFile file : files)
      if (file.isFolder () || file.isFileSystem ())
        text.append (file.catalog () + "\n");
      else
        text.append (file + "\n");

    while (text.charAt (text.length () - 1) == '\n')
      text.deleteCharAt (text.length () - 1);

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%s", fileName);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return fileType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return fileTypeText;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%s", fileName);
  }
}
