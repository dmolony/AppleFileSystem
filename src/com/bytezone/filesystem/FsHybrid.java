package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
public class FsHybrid extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  public FsHybrid (List<AppleFileSystem> fileSystems)
  // ---------------------------------------------------------------------------------//
  {
    this (fileSystems.get (0));

    for (AppleFileSystem fs : fileSystems)
    {
      addFile (fs);
      ((AbstractFileSystem) fs).partOfHybrid = true;
    }
  }

  // ---------------------------------------------------------------------------------//
  public FsHybrid (AppleFileSystem fs)
  // ---------------------------------------------------------------------------------//
  {
    this (fs.getBlockReader ());
  }

  // ---------------------------------------------------------------------------------//
  public FsHybrid (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.HYBRID);
  }
}
