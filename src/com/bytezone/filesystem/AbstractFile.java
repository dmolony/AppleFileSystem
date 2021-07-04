package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------------//
public abstract class AbstractFile implements AppleFile
// -----------------------------------------------------------------------------------//
{
  final AppleFileSystem fileSystem;
  String name;

  boolean isFile;
  boolean isFolder;
  boolean isFileSystem;

  List<AppleFile> files = new ArrayList<> ();
  List<AppleBlock> dataBlocks = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  AbstractFile (AppleFileSystem fileSystem)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystem = fileSystem;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getName ()
  // ---------------------------------------------------------------------------------//
  {
    return name;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return isFileSystem;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isDirectory ()
  // ---------------------------------------------------------------------------------//
  {
    return isFolder;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFile ()
  // ---------------------------------------------------------------------------------//
  {
    return isFile;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)    // if isDirectory() or isFileSystem()
  // ---------------------------------------------------------------------------------//
  {
    assert isDirectory () || isFileSystem ();
    files.add (file);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()      // if isDirectory() or isFileSystem()
  // ---------------------------------------------------------------------------------//
  {
    assert isDirectory () || isFileSystem ();
    return files;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlockSize ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystem.getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("read() not implemented");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {

  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return -1;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()                   // in data blocks
  // ---------------------------------------------------------------------------------//
  {
    return -1;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String catalog ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (toString () + "\n");

    for (AppleFile file : files)
      if (file.isDirectory () || file.isFileSystem ())
        text.append (file.catalog () + "\n");
      else
        text.append (file + "\n");

    while (text.charAt (text.length () - 1) == '\n')
      text.deleteCharAt (text.length () - 1);

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%s", name);
  }
}
