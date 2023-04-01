package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.AppleFileSystem.FileSystemType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractAppleFile implements AppleFile
// -----------------------------------------------------------------------------------//
{
  protected AppleFileSystem parentFileSystem;
  protected AppleFileSystem embeddedFileSystem;

  protected boolean isFile = true;
  protected boolean isFolder;
  //  protected boolean isFileSystem;
  protected boolean isForkedFile;             // FileProdos only
  protected boolean isFork;

  protected String fileName;
  protected int fileType;
  protected String fileTypeText;
  protected boolean isLocked;

  protected final List<AppleFile> files = new ArrayList<> ();
  protected String errorMessage = "";

  // ---------------------------------------------------------------------------------//
  AbstractAppleFile (AppleFileSystem appleFileSystem)
  // ---------------------------------------------------------------------------------//
  {
    this.parentFileSystem = appleFileSystem;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return isLocked;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isEmbeddedFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return embeddedFileSystem != null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFolder ()
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
  public boolean isForkedFile ()
  // ---------------------------------------------------------------------------------//
  {
    return isForkedFile;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFork ()
  // ---------------------------------------------------------------------------------//
  {
    return isFork;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isContainer ()
  // ---------------------------------------------------------------------------------//
  {
    return isEmbeddedFileSystem () || isFolder () || isForkedFile ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleFileSystem getParentFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFileSystem;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public FileSystemType getFileSystemType ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFileSystem.getFileSystemType ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleFileSystem getEmbeddedFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return embeddedFileSystem;
  }

  // ---------------------------------------------------------------------------------//
  void embedFileSystem (AppleFileSystem fileSystem)
  // ---------------------------------------------------------------------------------//
  {
    embeddedFileSystem = fileSystem;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String[] getPathFolders ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "getPathFolders() not implemented in " + fileName);
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public FileSystemType getFileSystemType ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return appleFileSystem.getFileSystemType ();
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public int getBlockSize ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return appleFileSystem.getBlockSize ();
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("read() not implemented in " + fileName);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("write() not implemented in " + fileName);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                         // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "getFileLength() not implemented in " + fileName);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (String
        .format ("getTotalBlocks() not implemented %s %s%n", fileTypeText, fileName));
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "getBlocks() not implemented in " + fileName);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return fileType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return fileTypeText;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getErrorMessage ()
  // ---------------------------------------------------------------------------------//
  {
    return errorMessage;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (
        String.format ("File type ............. %d    %s%n%n", fileType, fileTypeText));
    //    text.append (String.format ("File system type ...... %s%n%n", getFileSystemType ()));

    return text.toString ();
  }
}
