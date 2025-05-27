package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

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
  protected int fileGaps;      // empty blocks are not stored, leaving gaps in the index

  protected Buffer rawFileBuffer;             // all data blocks
  protected Buffer exactFileBuffer;           // adjusted for any offset or eof

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
  public boolean isRandomAccess ()
  // ---------------------------------------------------------------------------------//
  {
    return false;
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
  public int getAuxType ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "getAuxType() not implemented in " + getFileName ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean hasData ()
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size () > 0;
  }

  // Used to obtain the full buffer using every data block in full. Any adjustments
  // based on eof or offset can be applied by overriding getFileBuffer().
  // ---------------------------------------------------------------------------------//
  @Override
  public Buffer getRawFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    if (rawFileBuffer == null)
    {
      byte[] data = parentFileSystem.readBlocks (dataBlocks);
      rawFileBuffer = new Buffer (data, 0, data.length);           // do not use eof!
    }

    return rawFileBuffer;
  }

  // same data as rawFileBuffer, but with any offset or eof applied
  // ---------------------------------------------------------------------------------//
  @Override
  public Buffer getFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    // Override this if the file knows better
    if (exactFileBuffer == null)
      exactFileBuffer = getRawFileBuffer ();

    return exactFileBuffer;
  }

  // Override this if the file has a known offset or eof
  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                         // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    if (exactFileBuffer != null)
      return exactFileBuffer.length ();

    return dataBlocks.size () * parentFileSystem.getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "write() not implemented in " + getFileName ());
  }

  // Override this to include index blocks
  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                    // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalFileGaps ()
  // ---------------------------------------------------------------------------------//
  {
    return fileGaps;
  }

  // Override this to add index/catalog blocks
  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getAllBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks;                            // assumes no index or catalog blocks
  }

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
    formatText (text, "File name", getFileName ());
    formatText (text, "File system type", getFileSystemType ().toString ());
    if (embeddedFileSystem != null)
      formatText (text, "Embedded FS type",
          embeddedFileSystem.getFileSystemType ().toString ());

    formatText (text, "File type", 2, getFileType (), getFileTypeText ());
    formatText (text, "EOF", 6, getFileLength ());
    formatText (text, "Data blocks", 4, dataBlocks.size () - fileGaps);

    return text.toString ();
  }
}
