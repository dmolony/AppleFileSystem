package com.bytezone.filesystem;

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

    this.parentFile = parentFile;
    this.forkType = forkType;

    this.fileSystem = (FsNuFX) parentFile.getParentFileSystem ();
    this.thread = thread;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return forkType == ForkType.DATA ? "Data fork"
        : forkType == ForkType.RESOURCE ? "Resource fork" : "Not forked";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFile.getFileType ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFile.getFileTypeText ();
  }

  // ---------------------------------------------------------------------------------//
  public int getAuxType ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFile.getAuxType ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public ForkType getForkType ()
  // ---------------------------------------------------------------------------------//
  {
    return forkType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFile.isLocked ();
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
  @Override
  public Buffer getFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    if (fileBuffer == null)
    {
      byte[] buffer = thread.getData ();
      fileBuffer = new Buffer (buffer, 0, buffer.length);
    }

    return fileBuffer;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    Utility.formatMeta (text, "File name", getFileName ());
    Utility.formatMeta (text, "Fork type", forkType.toString ());
    Utility.formatMeta (text, "File type", 2, getFileType (), getFileTypeText ());
    Utility.formatMeta (text, "Aux", 4, getAuxType ());
    Utility.formatMeta (text, "Eof", 4, getFileLength ());
    Utility.formatMeta (text, "Parent", parentFile.fileName);
    Utility.formatMeta (text, "File system", fileSystem.fileSystemType.toString ());
    Utility.formatMeta (text, "File system id", 2, getFileSystemId ());

    text.append ("\n");
    text.append (thread.toString ());

    return text.toString ();
  }
}
