package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.filesystem.AppleFileSystem.FileSystemType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public interface AppleFile
// -----------------------------------------------------------------------------------//
{
  public String getFileName ();

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

  public AppleFileSystem getFileSystem ();          // if isFile()

  public FileSystemType getFileSystemType ();       // if isFileSystem()

  public byte[] read ();                            // if isFile()

  public void write (byte[] buffer);                // if isFile()

  public int getLength ();                          // in bytes (eof)

  public int getTotalBlocks ();                     // in data blocks

  public List<AppleBlock> getBlocks ();

  public String catalog ();

  public String getCatalogLine ();

  public String getFileTypeText ();

  public int getFileType ();

  public int getBlockSize ();                       // returns blockReader.blockSize()

  public default void dump ()
  {
    System.out.println (Utility.format (read ()));
  }
}
