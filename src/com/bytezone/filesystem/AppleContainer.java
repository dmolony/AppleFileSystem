package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
public interface AppleContainer
// -----------------------------------------------------------------------------------//
{
  public void addFile (AppleFile file);

  public List<AppleFile> getFiles ();

  public void addFileSystem (AppleFileSystem fileSystem);

  public List<AppleFileSystem> getFileSystems ();
}
