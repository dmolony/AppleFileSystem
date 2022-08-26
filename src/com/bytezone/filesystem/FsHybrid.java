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
      //      fs.name = fs.
      addFile (fs);
    }
  }

  // ---------------------------------------------------------------------------------//
  public FsHybrid (AppleFileSystem fs)
  // ---------------------------------------------------------------------------------//
  {
    this (fs.getName (), fs.getBuffer (), fs.getOffset (), fs.getLength (), fs.getBlockReader ());
  }

  // ---------------------------------------------------------------------------------//
  public FsHybrid (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);

    setFileSystemName ("Hybrid");
  }
}
