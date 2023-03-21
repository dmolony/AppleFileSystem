package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bytezone.filesystem.BlockReader.AddressType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public abstract class AbstractFileSystem implements AppleFileSystem
// -----------------------------------------------------------------------------------//
{
  protected FileSystemFactory factory;          // never static!!

  protected final BlockReader blockReader;
  protected int catalogBlocks;
  protected int freeBlocks;

  // When this FS is embedded, it either came from an existing AppleFile (eg PAR), or it
  // was a disk image in one of the non-standard file systems (zip, gz, NuFX 2img etc).
  // In order to obtain the parent FS, it will stored here. The AppleFile is needed to
  // keep the file details of the file that is now reinterpreted as a FS (PAR, LBR).

  //  protected AppleFileSystem appleFileSystem;    // the parent of this FS
  //  protected AppleFile appleFile;                // the source of this FS

  // If this file is a container (FS, folder, forked file, hybrid) then the children are
  // stored here
  protected List<AppleFile> files = new ArrayList<> ();

  protected FileSystemType fileSystemType;
  protected String errorMessage = "";

  private int totalFileSystems = 0;
  private int totalFiles = 0;

  protected boolean partOfHybrid;     // this FS is one of two file systems on the disk

  // ---------------------------------------------------------------------------------//
  public AbstractFileSystem (BlockReader blockReader, FileSystemType fileSystemType)
  // ---------------------------------------------------------------------------------//
  {
    this.blockReader = Objects.requireNonNull (blockReader);
    this.fileSystemType = fileSystemType;
  }

  // ---------------------------------------------------------------------------------//
  int getTotalCatalogBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isHybrid ()
  // ---------------------------------------------------------------------------------//
  {
    return partOfHybrid;
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public boolean isContainer ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return true;
  //  }

  // ---------------------------------------------------------------------------------//
  void setCatalogBlocks (int total)
  // ---------------------------------------------------------------------------------//
  {
    catalogBlocks = total;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock allocate ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("allocate() not implemented");
  }

  // ---------------------------------------------------------------------------------//
  boolean isValidBlockNo (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.isValidBlockNo (blockNo);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public BlockReader getBlockReader ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public FileSystemType getFileSystemType ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystemType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AddressType getType ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.addressType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlocksPerTrack ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.blocksPerTrack;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlockSize ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.bytesPerBlock;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock getBlock (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getBlock (this, blockNo);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock getSector (int track, int sector)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getSector (this, track, sector);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock getSector (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getSector (this, buffer, offset);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] readBlock (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.read (block);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] readBlocks (List<AppleBlock> blocks)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.read (blocks);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void writeBlock (AppleBlock block, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    blockReader.write (block, buffer);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void writeBlocks (List<AppleBlock> blocks, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    blockReader.write (blocks, buffer);
  }

  // AppleFile methods

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getName ();
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public String[] getPathFolders ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    throw new UnsupportedOperationException ("getPathFolders() not implemented");
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    files.add (file);

    //    if (file.isFileSystem ())
    //      ++totalFileSystems;
    //    else
    //      ++totalFiles;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()
  // ---------------------------------------------------------------------------------//
  {
    return files;
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public byte[] read ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    throw new UnsupportedOperationException ("Cannot call read() on a file system");
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public void write (byte[] buffer)
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    throw new UnsupportedOperationException ("Cannot call write() on a file system");
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public List<AppleBlock> getBlocks ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    if (appleFile != null)
  //      return appleFile.getBlocks ();
  //
  //    throw new UnsupportedOperationException ("Cannot call getBlocks() on a file system");
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public AppleFileSystem getFileSystem ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    if (appleFile != null)
  //      return appleFile.getFileSystem ();
  //
  //    return appleFileSystem;       // for embedded file systems only (usually null)
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public int getFileType ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    if (appleFile != null)
  //      return appleFile.getFileType ();
  //
  //    throw new UnsupportedOperationException (
  //        "Cannot call getFileType() on a file system");
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public String getFileTypeText ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    if (appleFile != null)
  //      return appleFile.getFileTypeText ();
  //
  //    throw new UnsupportedOperationException (
  //        "Cannot call getFileTypeText() on a file system");
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] getDiskBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getDiskBuffer ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getDiskOffset ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getDiskOffset ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getDiskLength ()               // in bytes
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getDiskLength ();
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public int getFileLength ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    if (appleFile != null)
  //      return appleFile.getFileLength ();
  //
  //    throw new UnsupportedOperationException (
  //        "Cannot call getFileLength() on a file system");
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()              // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFreeBlocks ()              // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return freeBlocks;
  }

  // FsNuFX
  // FsBinary2
  // FsProdos (LBR files)
  // ---------------------------------------------------------------------------------//
  //  protected void addFileSystem (AppleFile parent, AppleFile file)
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    addFileSystem (parent, file, 0);
  //  }

  // FsProdos (PAR files)
  // ---------------------------------------------------------------------------------//
  //  protected void addFileSystem (AppleFile parent, AppleFile file, int offset)
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    byte[] buffer = file.read ();
  //    BlockReader blockReader =
  //        new BlockReader (file.getFileName (), buffer, offset, buffer.length - offset);
  //
  //    AppleFileSystem fs = addFileSystem (parent, blockReader);
  //
  //    if (fs == null)
  //    {
  //      System.out.println ("No file systems found");
  //      parent.addFile (file);        // not a file system, so revert to adding it as a file
  //    }
  //    else
  //      ((AbstractFileSystem) fs).appleFile = file;     // don't lose the file details
  //  }

  // FsZip
  // FsGzip
  // ---------------------------------------------------------------------------------//
  //  protected AppleFileSystem addFileSystem (AppleFile parent, String name, byte[] buffer)
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return addFileSystem (parent, new BlockReader (name, buffer, 0, buffer.length));
  //  }

  // FsWoz
  // Fs2img
  // ---------------------------------------------------------------------------------//
  //  protected AppleFileSystem addFileSystem (AppleFile parent, BlockReader blockReader)
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    if (factory == null)
  //      factory = new FileSystemFactory ();
  //
  //    AppleFileSystem fs = factory.getFileSystem (blockReader);
  //
  //    if (fs != null)
  //    {
  //      parent.addFile (fs);
  //      ((AbstractFileSystem) fs).appleFileSystem = this;
  //    }
  //
  //    return fs;
  //  }

  // ---------------------------------------------------------------------------------//
  protected FileSystemFactory getFactory ()
  // ---------------------------------------------------------------------------------//
  {
    if (factory == null)
      factory = new FileSystemFactory ();

    return factory;
  }

  // ---------------------------------------------------------------------------------//
  protected void checkFileSystem (AbstractAppleFile file, int offset)
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = file.read ();
    BlockReader blockReader =
        new BlockReader (file.getFileName (), buffer, offset, buffer.length - offset);
    file.setFileSystem (getFactory ().getFileSystem (blockReader));
  }

  // ---------------------------------------------------------------------------------//
  protected void checkFileSystem (AbstractAppleFile file, BlockReader blockReader,
      int offset)
  // ---------------------------------------------------------------------------------//
  {
    file.setFileSystem (getFactory ().getFileSystem (blockReader));
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public boolean isLocked ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    throw new UnsupportedOperationException ("Cannot call isLocked() on a file system");
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public boolean isFileSystem ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return true;
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public boolean isFolder ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return false;
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public boolean isFile ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return false;
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public boolean isForkedFile ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return false;
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public boolean isFork ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return false;
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public AppleFile getAppleFile ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return appleFile;
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public String getErrorMessage ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return errorMessage;
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File name ............. %s%n", getFileName ()));
    text.append (String.format ("File system type ...... %s%n", fileSystemType));
    text.append ("\n");

    text.append (blockReader.toString ());
    text.append ("\n\n");

    text.append (String.format ("Catalog blocks ........ %d%n", catalogBlocks));
    text.append (String.format ("Total file systems .... %d%n", totalFileSystems));
    text.append (String.format ("Total files ........... %d%n%n", totalFiles));

    //    if (appleFileSystem != null)
    //      text.append (String.format ("Parent file system .... %s%n",
    //          appleFileSystem.getFileSystemType ()));
    //
    //    if (appleFile != null)
    //    {
    //      text.append (String.format ("Replacing file: %n"));
    //      String catalog = appleFile.toString ();
    //      String[] lines = catalog.split ("\n");
    //      int limit = 24;
    //      for (String line : lines)
    //      {
    //        if (line.length () >= limit)
    //        {
    //          String format = String.format ("  %%%d.%<ds %%s%%n", limit - 3, line);
    //          String newline = String.format (format, line, line.substring (limit));
    //          text.append (newline);
    //        }
    //        else
    //        {
    //          text.append (line);
    //          text.append ("\n");
    //        }
    //      }
    //    }

    return Utility.rtrim (text);
  }
}
