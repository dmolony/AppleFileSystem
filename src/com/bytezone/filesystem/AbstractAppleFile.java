package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.AppleFileSystem.FileSystemType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractAppleFile implements AppleFile
// -----------------------------------------------------------------------------------//
{
  protected AppleFileSystem appleFileSystem;  // FS of the disk on which this file exists

  protected boolean isFile;
  protected boolean isFolder;
  protected boolean isFileSystem;
  protected boolean isForkedFile;           // FileProdos only
  protected boolean isFork;

  protected String fileName;
  protected int fileType;
  protected String fileTypeText;
  protected boolean isLocked;

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
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return isLocked;
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
  public boolean isForkedFile ()
  // ---------------------------------------------------------------------------------//
  {
    return isForkedFile;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFork ()
  // ---------------------------------------------------------------------------------//
  {
    return isFork;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isContainer ()
  // ---------------------------------------------------------------------------------//
  {
    return isFileSystem || isFolder || isForkedFile;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    if (!isContainer ())
      throw new UnsupportedOperationException ("cannot addFile() unless Container");

    files.add (file);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()
  // ---------------------------------------------------------------------------------//
  {
    if (!isContainer ())
      throw new UnsupportedOperationException ("cannot getFiles() unless Container");

    return files;
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
  public int getFileLength ()                         // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("getFileLength() not implemented");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("getTotalBlocks() not implemented");
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
