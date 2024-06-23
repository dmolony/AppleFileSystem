package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.AppleFileSystem.FileSystemType;
import com.bytezone.filesystem.FileProdos.ForkType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractAppleFile implements AppleFile
// -----------------------------------------------------------------------------------//
{
  protected AppleFileSystem parentFileSystem;
  protected AppleFileSystem embeddedFileSystem;

  protected boolean isFile = true;
  protected boolean isFolder;
  protected boolean isForkedFile;             // FileProdos or FileNuFX
  protected boolean isFork;                   // Data or Resource fork

  protected String fileName;
  protected int fileType;
  protected String fileTypeText;
  protected boolean isLocked;

  protected String errorMessage = "";
  protected List<AppleBlock> dataBlocks = new ArrayList<> ();
  protected DataRecord dataRecord;

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

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isValidFile ()
  // ---------------------------------------------------------------------------------//
  {
    return true;
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
    DataRecord dataRecord = getDataRecord ();

    return dataRecord.data () != null && dataRecord.length () > 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public DataRecord getDataRecord ()
  // ---------------------------------------------------------------------------------//
  {
    if (dataRecord == null)
    {
      byte[] data = parentFileSystem.readBlocks (dataBlocks);
      dataRecord = new DataRecord (data, 0, data.length);
    }

    return dataRecord;
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
  public int getTotalBlocks ()                        // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks;
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
  public ForkType getForkType ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "getForkType() not implemented in " + fileName);
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
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    return fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("-------- File ---------\n");
    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (String.format ("File system type ...... %s%n", getFileSystemType ()));
    if (embeddedFileSystem != null)
      text.append (String.format ("Embedded FS type ...... %s%n",
          embeddedFileSystem.getFileSystemType ()));
    text.append (
        String.format ("File type ............. %02X  %s%n", fileType, fileTypeText));
    text.append (
        String.format ("EOF ................... %,6d  %<04X%n", getFileLength ()));

    return text.toString ();
  }
}
