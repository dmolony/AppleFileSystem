package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractFileSystem implements AppleFileSystem
// -----------------------------------------------------------------------------------//
{
  protected FileSystemFactory factory;          // only needed for embedded file systems

  protected final BlockReader blockReader;
  protected int catalogBlocks;

  protected AppleFileSystem parentFileSystem;   // pascal on prodos, dos in 2img etc
  protected List<AppleFile> files = new ArrayList<> ();   // files, folders and file systems

  //  protected String fileSystemName;        // DosX.X, Prodos, Pascal, CPM, NuFX, 2img, Bin2, Data
  protected FileSystemType fileSystemType;

  private int totalFileSystems = 0;
  private int totalFiles = 0;

  private boolean partOfHybrid;           // this fs is one of two file systems on the disk

  // ---------------------------------------------------------------------------------//
  public AbstractFileSystem (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this.blockReader = Objects.requireNonNull (blockReader);
  }

  // ---------------------------------------------------------------------------------//
  //  void setFileSystemName (String fileSystemName)
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    this.fileSystemName = fileSystemName;
  //  }

  // ---------------------------------------------------------------------------------//
  void setFileSystemType (FileSystemType fileSystemType)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystemType = fileSystemType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleFileSystem getParentFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFileSystem;
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
  //  @Override
  //  public String getFileSystemName ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return fileSystemName;
  //  }

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
    throw new UnsupportedOperationException ("Cannot call getFileSystem() on a file system");
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

  // ---------------------------------------------------------------------------------//
  protected void addFileSystem (AppleFileSystem parent, AbstractAppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    AppleFileSystem fs = addFileSystem (parent, file.getFileName (), file.read ());

    if (fs == null)
    {
      System.out.println ("No file systems found");
      parent.addFile (file);
    }
  }

  // ---------------------------------------------------------------------------------//
  protected AppleFileSystem addFileSystem (AppleFileSystem parent, String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    return addFileSystem (parent, new BlockReader (name, buffer, 0, buffer.length));
  }

  // ---------------------------------------------------------------------------------//
  protected AppleFileSystem addFileSystem (AppleFileSystem parent, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (factory == null)
      factory = new FileSystemFactory ();

    AppleFileSystem fs = factory.getFileSystem (blockReader);

    if (fs != null)
    {
      parent.addFile (fs);
      fs.setParentFileSystem (parent);
    }

    return fs;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void setParentFileSystem (AppleFileSystem appleFileSystem)
  // ---------------------------------------------------------------------------------//
  {
    this.parentFileSystem = appleFileSystem;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String catalog ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (toString () + "\n");
    boolean inFolder = false;

    //    for (AppleFile file : files)
    //      if (file.isFileSystem () || file.isFolder ())
    //      {
    //        text.append ("\n");
    //        text.append (file.catalog ());
    //        text.append ("\n");
    //        inFolder = true;
    //      }
    //      else
    //      {
    //        if (inFolder)
    //        {
    //          inFolder = false;
    //          text.append ("\n");
    //        }
    //        text.append (file);
    //        text.append ("\n");
    //      }

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
    return String.format ("%-12s %-6s %,8d  %d %,7d  %4d %3d %4d %3d", getFileName (),
        getFileSystemType (), blockReader.getDiskOffset (), blockReader.interleave,
        blockReader.totalBlocks, blockReader.bytesPerBlock, catalogBlocks, totalFiles,
        totalFileSystems);
  }
}
