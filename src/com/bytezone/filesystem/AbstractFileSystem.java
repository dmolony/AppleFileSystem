package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractFileSystem implements AppleFileSystem
// -----------------------------------------------------------------------------------//
{
  private final String fileName;

  private final byte[] diskBuffer;      // entire buffer including any header or other disks
  private final int diskOffset;         // start of this disk
  private final int diskLength;         // length of this disk

  private final BlockReader blockReader;

  private final int totalBlocks;
  private int catalogBlocks;

  private List<AppleFile> files = new ArrayList<> ();

  private String fileSystemName;        // DosX.X, Prodos, Pascal, CPM, Data

  // ---------------------------------------------------------------------------------//
  public AbstractFileSystem (String fileName, byte[] buffer, int offset, int length,
      BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    Objects.checkFromIndexSize (offset, length, buffer.length);

    this.fileName = fileName;
    this.diskBuffer = buffer;
    this.diskOffset = offset;
    this.diskLength = length;
    this.blockReader = Objects.requireNonNull (blockReader);

    totalBlocks = diskLength / blockReader.blockSize;

    assert totalBlocks > 0;

    readCatalog ();
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
    return blockReader.read (diskBuffer, diskOffset, block);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] readBlocks (List<AppleBlock> blocks)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.read (diskBuffer, diskOffset, blocks);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void writeBlock (AppleBlock block, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    blockReader.write (diskBuffer, diskOffset, block, buffer);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void writeBlocks (List<AppleBlock> blocks, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    blockReader.write (diskBuffer, diskOffset, blocks, buffer);
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
    return diskOffset;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()         // in bytes
  // ---------------------------------------------------------------------------------//
  {
    return diskLength;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()           // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return totalBlocks;
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
    text.append (String.format ("Disk offset ........... %,d%n", diskOffset));
    text.append (String.format ("Disk length ........... %,d%n", diskLength));
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
    return String.format ("%-20.20s %-6s %,8d  %d %,7d  %4d %2d  %3d", fileName, fileSystemName,
        diskOffset, blockReader.interleave, totalBlocks, blockReader.blockSize, catalogBlocks,
        files.size ());
  }
}
