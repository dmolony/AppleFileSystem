package com.bytezone.filesystem;

import java.util.List;

//-----------------------------------------------------------------------------------//
public interface AppleFileContainer
// ----------------------------------------------------------------------------------//
{
  public void addFile (AppleFile file);

  public List<AppleFile> getFiles ();
}
