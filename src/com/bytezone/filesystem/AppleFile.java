package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.filesystem.AppleFileSystem.FileSystemType;

// -----------------------------------------------------------------------------------//
public interface AppleFile
// -----------------------------------------------------------------------------------//
{
  public String getFileName ();

  public int getFileType ();

  public String getFileTypeText ();

  public boolean hasEmbeddedFileSystem ();

  public boolean isFolder ();

  public boolean isForkedFile ();             // top-level file containing fork(s)

  public boolean isFork ();                   // either a RESOURCE or DATA fork

  public boolean isValidFile ();              // reject DOS catalog nonsense files

  public ForkType getForkType ();

  public boolean isContainer ();

  public AppleFileSystem getParentFileSystem ();

  public AppleFileSystem getEmbeddedFileSystem ();

  public FileSystemType getFileSystemType ();

  public boolean isLocked ();

  public int getFileLength ();                      // in bytes (eof)

  public int getTotalBlocks ();                     // in data blocks

  public List<AppleBlock> getBlocks ();

  public String getCatalogLine ();

  public boolean hasData ();

  public Buffer getFileBuffer ();                   // reads file if required

  public Buffer getFileBuffer (int eof);            // reads file if required

  public void write (byte[] buffer);

  public String getErrorMessage ();                 // if file can't be read

  public enum ForkType
  {
    DATA, RESOURCE;
  }
}
