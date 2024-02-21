package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
class FsHybrid extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  public FsHybrid (List<AppleFileSystem> fileSystems)
  // ---------------------------------------------------------------------------------//
  {
    this (fileSystems.get (0));

    for (AppleFileSystem fs : fileSystems)
    {
      addFileSystem (fs);
      ((AbstractFileSystem) fs).partOfHybrid = true;
    }
  }

  // ---------------------------------------------------------------------------------//
  FsHybrid (AppleFileSystem fs)
  // ---------------------------------------------------------------------------------//
  {
    this (fs.getBlockReader ());
  }

  // ---------------------------------------------------------------------------------//
  FsHybrid (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.HYBRID);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    for (AppleFileSystem fs : getFileSystems ())
    {
      text.append (fs.getCatalogText ());
      text.append ("\n\n");
    }

    return text.toString ();
  }
}
