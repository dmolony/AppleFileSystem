package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
public interface AppleFile
// -----------------------------------------------------------------------------------//
{
  public String getName ();

  public boolean isFileSystem ();

  public boolean isDirectory ();

  public boolean isFile ();

  public void addFile (AppleFile file);    // if isDirectory() or isFileSystem()

  public List<AppleFile> getFiles ();      // if isDirectory() or isFileSystem()

  public int getBlockSize ();

  public byte[] read ();

  public void write (byte[] buffer);

  public int getLength ();                 // in bytes (eof)

  public int getSize ();                   // in data blocks

  public List<AppleBlock> getBlocks ();

  public String catalog ();
}
