package com.bytezone.filesystem;

import com.bytezone.filesystem.FileProdos.ForkType;

// -----------------------------------------------------------------------------------//
public class ForkNuFX extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private FileNuFX parentFile;
  private FsNuFX fileSystem;
  private ForkType forkType;
  //  private byte[] buffer;
  private NuFXThread thread;

  // ---------------------------------------------------------------------------------//
  ForkNuFX (FileNuFX parentFile, ForkType forkType, NuFXThread thread)
  // ---------------------------------------------------------------------------------//
  {
    super (parentFile.getFileSystem ());

    isFile = true;
    isFork = forkType != null;

    fileType = parentFile.getFileType ();
    fileTypeText = parentFile.getFileTypeText ();

    this.parentFile = parentFile;
    this.forkType = forkType;
    this.fileName = forkType == ForkType.DATA ? "Data fork"
        : forkType == ForkType.RESOURCE ? "Resource fork" : "Not forked";

    this.fileSystem = (FsNuFX) parentFile.getFileSystem ();
    this.thread = thread;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    return thread.uncompressedEOF;
  }

  // ---------------------------------------------------------------------------------//
  public int getFileSystemId ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFile.getFileSystemId ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return thread.getData ();
  }
}
