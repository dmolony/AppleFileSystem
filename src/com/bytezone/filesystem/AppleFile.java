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

  public boolean isRandomAccess ();

  public ForkType getForkType ();

  public boolean isContainer ();

  public AppleFileSystem getParentFileSystem ();

  public AppleFileSystem getEmbeddedFileSystem ();

  public FileSystemType getFileSystemType ();

  public boolean isLocked ();

  public int getFileLength ();                      // in bytes (eof)

  public int getTotalBlocks ();                     // in data+index blocks

  public List<AppleBlock> getDataBlocks ();         // dataBlocks only

  public List<AppleBlock> getAllBlocks ();          // dataBlocks + indexBlocks

  public String getCatalogLine ();

  public boolean hasData ();

  public Buffer getRawFileBuffer ();                // reads file if required

  public Buffer getFileBuffer ();                   // override if eof known

  public void write (byte[] buffer);

  public void delete (boolean force);

  public String getErrorMessage ();                 // if file can't be read

  public enum ForkType
  {
    DATA, RESOURCE;
  }
}
