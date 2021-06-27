package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
public abstract class AbstractFile implements AppleFile
// -----------------------------------------------------------------------------------//
{
  final AppleFileSystem fileSystem;
  String name;

  boolean isFile;
  boolean isFolder;
  boolean isFileSystem;

  // ---------------------------------------------------------------------------------//
  AbstractFile (AppleFileSystem fileSystem)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystem = fileSystem;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getName ()
  // ---------------------------------------------------------------------------------//
  {
    return name;
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
  public boolean isDirectory ()
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
    assert isDirectory () || isFileSystem;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()      // if isDirectory() or isFileSystem()
  // ---------------------------------------------------------------------------------//
  {
    assert isDirectory () || isFileSystem;
    return null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {

  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return -1;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()                   // in data blocks
  // ---------------------------------------------------------------------------------//
  {
    return -1;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlockSize ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystem.getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%s", name);
  }
}
