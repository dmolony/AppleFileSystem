package com.bytezone.filesystem;

import com.bytezone.filesystem.FileProdos.ForkType;

// -----------------------------------------------------------------------------------//
public class ForkNuFX extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private FileNuFX parentFile;
  private FsProdos fileSystem;
  private ForkType forkType;

  // ---------------------------------------------------------------------------------//
  ForkNuFX (FileNuFX parentFile, ForkType forkType, int keyPtr, int storageType, int size,
      int eof)
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
    this.fileSystem = (FsProdos) parentFile.getFileSystem ();
  }

}
