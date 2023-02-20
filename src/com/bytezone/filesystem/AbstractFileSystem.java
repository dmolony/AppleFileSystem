package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractFileSystem implements AppleFileSystem
// -----------------------------------------------------------------------------------//
{
  protected static FileSystemFactory factory;   // only needed for embedded file systems

  protected final BlockReader blockReader;
  protected int catalogBlocks;

  // When this FS is embedded, it either came from an existing AppleFile (eg PAR), or is
  // was a disk image in one of the non-standard file systems (zip, gz, NuFX 2img etc).
  // In order to obtain the parent FS, it will stored here. The AppleFile is needed to
  // keep the file details of the file that was reinterpreted as a FS (PAR, LBR)
  protected AppleFileSystem appleFileSystem;    // the parent of this FS
  protected AppleFile appleFile;                // the source of this FS

  protected List<AppleFile> files = new ArrayList<> ();   // files, folders and file systems

  protected FileSystemType fileSystemType;

  private int totalFileSystems = 0;
  private int totalFiles = 0;

  private boolean partOfHybrid;           // this fs is one of two file systems on the disk

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
  public void setHybrid ()
  // ---------------------------------------------------------------------------------//
  {
    partOfHybrid = true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isHybrid ()
  // ---------------------------------------------------------------------------------//
  {
    return partOfHybrid;
  }

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
  @Override
  public boolean isFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    files.add (file);

    if (file.isFileSystem ())
      ++totalFileSystems;
    else
      ++totalFiles;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()
  // ---------------------------------------------------------------------------------//
  {
    return files;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("Cannot call read() on a file system");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("Cannot call write() on a file system");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("Cannot call getBlocks() on a file system");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleFileSystem getFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFileSystem;
    //    throw new UnsupportedOperationException ("Cannot call getFileSystem() on a file system");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("Cannot call getFileType() on a file system");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("Cannot call getFileTypeText() on a file system");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] getBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getDiskBuffer ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getOffset ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getDiskOffset ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                 // in bytes
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getDiskLength ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()           // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.totalBlocks;
  }

  // FsNuFX
  // FsBinary2
  // FsProdos (LBR files)
  // ---------------------------------------------------------------------------------//
  protected void addFileSystem (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    addFileSystem (file, 0);
  }

  // FsProdos (PAR files)
  // ---------------------------------------------------------------------------------//
  protected void addFileSystem (AppleFile file, int offset)
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = file.read ();
    BlockReader blockReader =
        new BlockReader (file.getFileName (), buffer, offset, buffer.length - offset);

    AppleFileSystem fs = addFileSystem (blockReader);

    if (fs == null)
    {
      System.out.println ("No file systems found");
      addFile (file);        // not a file system, so revert to adding it as a file
    }
    else
      ((AbstractFileSystem) fs).setAppleFile (file);     // don't lose the file details
  }

  // FsZip
  // FsGzip
  // ---------------------------------------------------------------------------------//
  protected AppleFileSystem addFileSystem (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    return addFileSystem (new BlockReader (name, buffer, 0, buffer.length));
  }

  // FsWoz
  // Fs2img
  // ---------------------------------------------------------------------------------//
  protected AppleFileSystem addFileSystem (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (factory == null)
      factory = new FileSystemFactory ();

    AppleFileSystem fs = factory.getFileSystem (blockReader);

    if (fs != null)
    {
      addFile (fs);
      ((AbstractFileSystem) fs).setParentFileSystem (this);
    }

    return fs;
  }

  // ---------------------------------------------------------------------------------//
  void setParentFileSystem (AppleFileSystem appleFileSystem)
  // ---------------------------------------------------------------------------------//
  {
    this.appleFileSystem = appleFileSystem;
  }

  // ---------------------------------------------------------------------------------//
  void setAppleFile (AppleFile appleFile)
  // ---------------------------------------------------------------------------------//
  {
    this.appleFile = appleFile;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String catalog ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (toString ());
    text.append ("\n");

    for (AppleFile file : files)
    {
      text.append (file);
      text.append ("\n");
    }

    while (text.charAt (text.length () - 1) == '\n')
      text.deleteCharAt (text.length () - 1);

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    return toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File name ............. %s%n", getFileName ()));
    //    text.append (String.format ("File system name ...... %s%n", fileSystemName));
    text.append (String.format ("File system type ...... %s%n", fileSystemType));

    text.append (blockReader.toText ());
    text.append ("\n");

    text.append (String.format ("Catalog blocks ........ %d%n", catalogBlocks));
    text.append (String.format ("Total file systems .... %d%n", totalFileSystems));
    text.append (String.format ("Total files ........... %d %d", files.size (), totalFiles));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    if (appleFile != null)
      return appleFile.toString ();

    return String.format ("%-12s %-6s %,8d  %d %,7d  %4d %3d %4d %3d", getFileName (),
        getFileSystemType (), blockReader.getDiskOffset (), blockReader.interleave,
        blockReader.totalBlocks, blockReader.bytesPerBlock, catalogBlocks, totalFiles,
        totalFileSystems);
  }
}
