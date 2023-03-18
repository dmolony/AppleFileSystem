package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.filesystem.AppleFileSystem.FileSystemType;

// -----------------------------------------------------------------------------------//
public interface AppleFile
// -----------------------------------------------------------------------------------//
{
  public String getFileName ();

  public boolean isFileSystem ();

  public boolean isFolder ();

  public boolean isFile ();

  public boolean isForkedFile ();

  public boolean isFork ();

  public boolean isContainer ();

  public String[] getPathFolders ();                // move to Utility

  // File Stuff

  public byte[] read ();                            // if isFile()

  public void write (byte[] buffer);                // if isFile()

  public AppleFileSystem getFileSystem ();          // if isFile()

  public boolean isLocked ();                       // if isFile()

  public int getFileLength ();                      // in bytes (eof)

  public int getTotalBlocks ();                     // in data blocks

  public List<AppleBlock> getBlocks ();

  public int getFileType ();

  public String getFileTypeText ();

  public String getErrorMessage ();                 // if file can't be read

  // File System Stuff

  public FileSystemType getFileSystemType ();       // if isFileSystem()

  public void addFile (AppleFile file);             // if isFolder() or isFileSystem()

  public List<AppleFile> getFiles ();               // if isFolder() or isFileSystem()

  public int getBlockSize ();                       // returns blockReader.blockSize()
}
