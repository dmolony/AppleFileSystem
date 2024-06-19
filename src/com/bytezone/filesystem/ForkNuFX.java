package com.bytezone.filesystem;

import com.bytezone.filesystem.FileProdos.ForkType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class ForkNuFX extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private FileNuFX parentFile;
  private FsNuFX fileSystem;
  private ForkType forkType;
  private NuFXThread thread;

  // ---------------------------------------------------------------------------------//
  ForkNuFX (FileNuFX parentFile, ForkType forkType, NuFXThread thread)
  // ---------------------------------------------------------------------------------//
  {
    super (parentFile.getParentFileSystem ());

    isFork = forkType != null;

    fileType = parentFile.getFileType ();
    fileTypeText = parentFile.getFileTypeText ();

    this.parentFile = parentFile;
    this.forkType = forkType;
    this.fileName = forkType == ForkType.DATA ? "Data fork"
        : forkType == ForkType.RESOURCE ? "Resource fork" : "Not forked";

    this.fileSystem = (FsNuFX) parentFile.getParentFileSystem ();
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
  @Override
  public int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return (thread.uncompressedEOF - 1) / 512 + 1;      // wrong, but close
  }

  // ---------------------------------------------------------------------------------//
  public int getFileSystemId ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFile.getFileSystemId ();
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public byte[] read ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return thread.getData ();
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public DataRecord getDataRecord ()
  // ---------------------------------------------------------------------------------//
  {
    if (dataRecord == null)
    {
      byte[] buffer = thread.getData ();
      dataRecord = new DataRecord (buffer, 0, buffer.length);
    }

    return dataRecord;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (String.format ("Fork type ............. %s%n", forkType));
    text.append (
        String.format ("File type ............. %02X  %s%n", fileType, fileTypeText));
    text.append (
        String.format ("Eof ................... %04X  %<,7d%n", getFileLength ()));
    text.append (String.format ("Parent ................ %s%n", parentFile.fileName));
    text.append (
        String.format ("File system ........... %s%n", fileSystem.fileSystemType));
    text.append (String.format ("File system id ........ %d%n", getFileSystemId ()));
    text.append (String.format ("Thread ................ %n%n%s%n", thread.toString ()));

    return Utility.rtrim (text);
  }
}
