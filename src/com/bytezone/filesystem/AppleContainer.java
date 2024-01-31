package com.bytezone.filesystem;

import java.util.List;
import java.util.Optional;

// -----------------------------------------------------------------------------------//
public interface AppleContainer
// -----------------------------------------------------------------------------------//
{
  public void addFile (AppleFile file);

  public List<AppleFile> getFiles ();

  public Optional<AppleFile> getFile (String fileName);

  public void addFileSystem (AppleFileSystem fileSystem);

  public List<AppleFileSystem> getFileSystems ();

  public String getCatalogText ();

  public String getPath ();
}
