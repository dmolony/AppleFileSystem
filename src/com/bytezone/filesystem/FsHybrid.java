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
      ((AbstractFileSystem) fs).appleFileSystem = this;
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

  // ---------------------------------------------------------------------------------//
  @Override
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File name ............. %s%n", getFileName ()));
    text.append (String.format ("File system type ...... %s%n", fileSystemType));
    text.append ("\n");

    for (AppleFile appleFile : files)
      text.append (String.format ("File system type ...... %s%n", appleFile.getFileSystemType ()));

    return text.toString ();
  }
}
