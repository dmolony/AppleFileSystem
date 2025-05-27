package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class ForkNuFX extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private final FileNuFX parentFile;
  private final FsNuFX fileSystem;
  private final ForkType forkType;
  private final NuFXThread thread;

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
  @Override
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
  public Buffer getRawFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    if (rawFileBuffer == null)
    {
      byte[] buffer = thread.getData ();
      rawFileBuffer = new Buffer (buffer, 0, buffer.length);
    }

    return rawFileBuffer;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    //    LocalDateTime created = parentFile.getCreated ();
    //    LocalDateTime modified = parentFile.getModified ();

    int fileLength = isForkedFile () ? 0 : getFileLength ();

    //    String dateCreated = created == null ? NO_DATE : created.format (sdf);
    //    String timeCreated = created == null ? "" : created.format (stf);
    //    String dateModified = modified == null ? NO_DATE : modified.format (sdf);
    //    String timeModified = modified == null ? "" : modified.format (stf);

    //    String forkFlag = isForkedFile () ? "+" : " ";

    //    text.append (String.format ("%s%-15s %3s%s  %5d  %9s %5s  %9s %5s %8d %7s%n",
    //        isLocked () ? "*" : " ", getFileName (), getFileTypeText (), forkFlag,
    //        getTotalBlocks (), dateModified, timeModified, dateCreated, timeCreated,
    //        fileLength, getSubType ()));
    text.append (String.format ("%s%-15s %3s  %5d  %8d  %3d", isLocked () ? "*" : " ",
        getFileName (), getFileTypeText (), getTotalBlocks (), fileLength,
        getAuxType ()));

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    formatText (text, "File name", getFileName ());
    formatText (text, "Fork type", forkType.toString ());
    formatText (text, "File type", 2, getFileType (), getFileTypeText ());
    formatText (text, "Aux", 4, getAuxType ());
    formatText (text, "Eof", 4, getFileLength ());
    formatText (text, "Parent", parentFile.fileName);
    formatText (text, "File system", fileSystem.fileSystemType.toString ());
    formatText (text, "File system id", 2, getFileSystemId ());

    text.append ("\n");
    text.append (thread.toString ());

    return Utility.rtrim (text);
  }
}
