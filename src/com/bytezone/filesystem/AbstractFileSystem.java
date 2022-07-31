package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractFileSystem implements AppleFileSystem
// -----------------------------------------------------------------------------------//
{
  protected final String fileName;

  private final byte[] diskBuffer;      // entire buffer including any header or other disks
  protected final int fileOffset;         // start of this disk
  private final int fileLength;         // length of this disk

  protected final BlockReader blockReader;

  protected final int totalBlocks;
  protected int catalogBlocks;

  protected List<AppleFile> files = new ArrayList<> ();

  protected String fileSystemName;        // DosX.X, Prodos, Pascal, CPM, Data

  protected FileSystemFactory factory;
  protected int totalFileSystems = 0;
  protected int totalFiles = 0;

  // ---------------------------------------------------------------------------------//
  public AbstractFileSystem (String fileName, byte[] buffer, int offset, int length,
      BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    Objects.checkFromIndexSize (offset, length, buffer.length);

    this.fileName = fileName;
    this.diskBuffer = buffer;
    this.fileOffset = offset;
    this.fileLength = length;
    this.blockReader = Objects.requireNonNull (blockReader);

    totalBlocks = fileLength / blockReader.blockSize;

    assert totalBlocks > 0;
  }

  // ---------------------------------------------------------------------------------//
  void setFileSystemName (String fileSystemName)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystemName = fileSystemName;
  }

  // ---------------------------------------------------------------------------------//
  int getTotalCatalogBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogBlocks;
  }

  // ---------------------------------------------------------------------------------//
  void setCatalogBlocks (int total)
  // ---------------------------------------------------------------------------------//
  {
    catalogBlocks = total;
  }

  // ---------------------------------------------------------------------------------//
  boolean isValidBlockNo (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockNo >= 0 && blockNo < totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock allocate ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("allocate() not implemented");
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
    return blockReader.read (diskBuffer, fileOffset, block);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] readBlocks (List<AppleBlock> blocks)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.read (diskBuffer, fileOffset, blocks);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void writeBlock (AppleBlock block, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    blockReader.write (diskBuffer, fileOffset, block, buffer);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void writeBlocks (List<AppleBlock> blocks, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    blockReader.write (diskBuffer, fileOffset, blocks, buffer);
  }

  // AppleFile methods

  // ---------------------------------------------------------------------------------//
  @Override
  public String getName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileName;
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
  public int getBlockSize ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.blockSize;
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
  public byte[] getBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return diskBuffer;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getOffset ()
  // ---------------------------------------------------------------------------------//
  {
    return fileOffset;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()         // in bytes
  // ---------------------------------------------------------------------------------//
  {
    return fileLength;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()           // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  protected void addFileSystem (AppleFile parent, AbstractFile file)
  // ---------------------------------------------------------------------------------//
  {
    if (factory == null)
      factory = new FileSystemFactory ();

    List<AppleFileSystem> fileSystems = factory.getFileSystems (file);

    if (fileSystems.size () == 0)
    {
      System.out.println ("No file systems found");
      parent.addFile (file);
      ++totalFiles;
    }
    else
      for (AppleFileSystem fs : fileSystems)
      {
        parent.addFile (fs);
        ++totalFileSystems;
      }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String catalog ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (toString () + "\n");

    for (AppleFile file : files)
      if (file.isFileSystem () || file.isDirectory ())
      {
        text.append ("\n");
        text.append (file.catalog ());
        text.append ("\n");
      }
      else
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
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File system ........... %s%n", fileSystemName));
    text.append (String.format ("File offset ........... %,d%n", fileOffset));
    text.append (String.format ("File length ........... %,d%n", fileLength));
    text.append (String.format ("Total blocks .......... %,d%n", totalBlocks));
    text.append (String.format ("Block size ............ %d%n", blockReader.blockSize));
    text.append (String.format ("Interleave ............ %d%n", blockReader.interleave));
    text.append (String.format ("Catalog blocks ........ %d%n", catalogBlocks));
    text.append (String.format ("Total files ........... %d", files.size ()));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-20.20s %-6s %,8d  %d %,7d  %4d %3d %3d", fileName, fileSystemName,
        fileOffset, blockReader.interleave, totalBlocks, blockReader.blockSize, catalogBlocks,
        files.size ());
  }
}
