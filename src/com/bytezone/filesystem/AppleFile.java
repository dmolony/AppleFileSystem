package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
public interface AppleFile
// -----------------------------------------------------------------------------------//
{
  public String getFileName ();

  public boolean isFileSystem ();                   // if file has an embedded FS

  public boolean isFolder ();

  public boolean isFile ();

  public boolean isForkedFile ();

  public boolean isFork ();

  public boolean isContainer ();

  public String[] getPathFolders ();                // move to Utility

  public byte[] read ();

  public void write (byte[] buffer);

  public AppleFileSystem getParentFileSystem ();

  public AppleFileSystem getEmbeddedFileSystem ();

  public boolean isLocked ();

  public int getFileLength ();                      // in bytes (eof)

  public int getTotalBlocks ();                     // in data blocks

  public List<AppleBlock> getBlocks ();

  public int getFileType ();

  public String getFileTypeText ();

  public String getErrorMessage ();                 // if file can't be read
}
