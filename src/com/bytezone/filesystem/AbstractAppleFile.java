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
  protected boolean isForkedFile;             // FileProdos or FileNuFX with fork(s)
  protected boolean isFork;                   // Data or Resource fork

  protected String errorMessage = "";
  protected List<AppleBlock> dataBlocks = new ArrayList<> ();
  protected Buffer fileBuffer;

  // ---------------------------------------------------------------------------------//
  AbstractAppleFile (AppleFileSystem parentFileSystem)
  // ---------------------------------------------------------------------------------//
  {
    this.parentFileSystem = parentFileSystem;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean hasEmbeddedFileSystem ()
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

  // top-level file containing RESOURCE and/or DATA fork
  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isForkedFile ()
  // ---------------------------------------------------------------------------------//
  {
    return isForkedFile;
  }

  // either a RESOURCE or DATA fork
  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFork ()
  // ---------------------------------------------------------------------------------//
  {
    return isFork;
  }

  // override to flag files that are just catalog fillers
  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isValidFile ()
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size () > 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isContainer ()
  // ---------------------------------------------------------------------------------//
  {
    return hasEmbeddedFileSystem () || isFolder () || isForkedFile ();
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
  public boolean hasData ()
  // ---------------------------------------------------------------------------------//
  {
    Buffer dataRecord = getFileBuffer ();

    return dataRecord.data () != null && dataRecord.length () > 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Buffer getFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    if (fileBuffer == null)
    {
      byte[] data = parentFileSystem.readBlocks (dataBlocks);
      fileBuffer = new Buffer (data, 0, data.length);
    }

    return fileBuffer;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Buffer getFileBuffer (int eof)
  // ---------------------------------------------------------------------------------//
  {
    if (fileBuffer == null)
    {
      byte[] data = parentFileSystem.readBlocks (dataBlocks);
      fileBuffer = new Buffer (data, 0, eof);
    }

    return fileBuffer;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "write() not implemented in " + getFileName ());
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public void delete ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    getParentFileSystem ().deleteFile (this);
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                         // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "getFileLength() not implemented in " + getFileName ());
  }

  // assumes no index blocks
  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                    // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size ();
  }

  // assumes no index blocks
  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getAllBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks;
  }

  // assumes no index blocks
  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getDataBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public ForkType getForkType ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "getForkType() not implemented in " + getFileName ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void delete (boolean force)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "deleteFile() not implemented in " + getFileName ());
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
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    return getFileName ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("-------- File ---------\n");
    text.append (String.format ("File name ............. %s%n", getFileName ()));
    text.append (String.format ("File system type ...... %s%n", getFileSystemType ()));
    if (embeddedFileSystem != null)
      text.append (String.format ("Embedded FS type ...... %s%n",
          embeddedFileSystem.getFileSystemType ()));
    text.append (String.format ("File type ............. %02X      %<,9d  %s%n",
        getFileType (), getFileTypeText ()));
    text.append (
        String.format ("EOF ................... %06X  %<,9d%n", getFileLength ()));

    return text.toString ();
  }
}
