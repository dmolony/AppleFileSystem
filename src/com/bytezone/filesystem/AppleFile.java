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

  public default boolean isFolder ()
  {
    return false;
  }

  public default boolean isFile ()
  {
    return false;
  }

  public void addFile (AppleFile file);             // if isFolder() or isFileSystem()

  public List<AppleFile> getFiles ();               // if isFolder() or isFileSystem()

  public byte[] read ();

  public void write (byte[] buffer);

  public int getLength ();                          // in bytes (eof)

  public int getSize ();                            // in data blocks

  public List<AppleBlock> getBlocks ();

  public String catalog ();

  public String getCatalogLine ();

  public int getBlockSize ();                       // returns blockReader.blockSize()

  public AppleFileSystem getFileSystem ();
}
