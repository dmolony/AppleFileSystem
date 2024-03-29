package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.filesystem.AppleFileSystem.FileSystemType;
import com.bytezone.filesystem.FileProdos.ForkType;

// -----------------------------------------------------------------------------------//
public interface AppleFile
// -----------------------------------------------------------------------------------//
{
  public String getFileName ();

  public int getFileType ();

  public String getFileTypeText ();

  public boolean hasEmbeddedFileSystem ();

  public boolean isFolder ();

  public boolean isForkedFile ();

  public boolean isFork ();

  public boolean isActualFile ();              // reject DOS catalog nonsense files

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

  public byte[] read ();

  public void write (byte[] buffer);

  public String getErrorMessage ();                 // if file can't be read
}
