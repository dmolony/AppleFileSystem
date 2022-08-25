package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
public interface AppleFile
// -----------------------------------------------------------------------------------//
{
  public String getName ();

  public default boolean isFileSystem ()
  {
    return false;
  }

  public default boolean isDirectory ()
  {
    return false;
  }

  public default boolean isFile ()
  {
    return false;
  }

  public void addFile (AppleFile file);             // if isDirectory() or isFileSystem()

  public List<AppleFile> getFiles ();               // if isDirectory() or isFileSystem()

  public byte[] read ();

  public void write (byte[] buffer);

  public int getLength ();                          // in bytes (eof)

  public int getSize ();                            // in data blocks

  public List<AppleBlock> getBlocks ();

  public String catalog ();

  public String getCatalogLine ();

  public int getBlockSize ();                       // returns blockReader.blockSize()
}
