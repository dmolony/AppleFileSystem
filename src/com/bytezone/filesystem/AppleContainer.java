package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.filesystem.AppleFileSystem.FileSystemType;

// -----------------------------------------------------------------------------------//
public interface AppleContainer
// -----------------------------------------------------------------------------------//
{
  public void addFile (AppleFile file);

  public List<AppleFile> getFiles ();

  public void addFileSystem (AppleFileSystem fileSystem);

  public List<AppleFileSystem> getFileSystems ();

  public FileSystemType getFileSystemType ();

  public String getCatalog ();

  public String getPath ();
}
