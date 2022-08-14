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
    this (fileSystems.get (0).getName (), fileSystems.get (0).getBuffer (),
        fileSystems.get (0).getOffset (), fileSystems.get (0).getLength (),
        fileSystems.get (0).getBlockReader ());

    for (AppleFileSystem fs : fileSystems)
      addFile (fs);
  }

  // ---------------------------------------------------------------------------------//
  public FsHybrid (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);

    setFileSystemName ("Hybrid");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    System.out.println ("Nothing to see here");
  }
}
