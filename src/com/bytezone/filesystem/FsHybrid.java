package com.bytezone.filesystem;

import java.nio.file.Path;
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
      //      fs.name = fs.
      addFile (fs);
    }
  }

  // ---------------------------------------------------------------------------------//
  public FsHybrid (AppleFileSystem fs)
  // ---------------------------------------------------------------------------------//
  {
    this (fs.getBlockReader ());
  }

  // ---------------------------------------------------------------------------------//
  public FsHybrid (Path path, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (path, blockReader);

    setFileSystemName ("Hybrid");
  }

  // ---------------------------------------------------------------------------------//
  public FsHybrid (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader);

    setFileSystemName ("Hybrid");
  }
}
