package com.bytezone.filesystem;

import java.util.List;

//-----------------------------------------------------------------------------------//
public interface AppleFileSystemContainer
//----------------------------------------------------------------------------------//
{
  public void addFileSystem (AppleFileSystem fileSystem);

  public List<AppleFileSystem> getFileSystems ();
}
