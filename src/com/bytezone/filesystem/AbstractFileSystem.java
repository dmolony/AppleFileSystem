package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractFileSystem implements AppleFileSystem
// -----------------------------------------------------------------------------------//
{
  private String fileSystemName;        // DosX.X, Prodos, Pascal, CPM
  private String fileName;

  final byte[] diskBuffer;      // entire buffer including any header or other disks
  final int diskOffset;         // start of this disk
  final int diskLength;         // length of this disk

  private BlockReader blockReader;

  int totalBlocks;
  int catalogBlocks;

  List<AppleFile> files = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  public AbstractFileSystem (String fileName, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    this.fileName = fileName;
    this.diskBuffer = buffer;
    this.diskOffset = offset;
    this.diskLength = length;

    assert offset + length <= diskBuffer.length : String.format (
        "Disk length: %,d too small for offset %,d + length %,d", diskBuffer.length,
        offset, length);
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
  boolean isValidBlockNo (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockNo >= 0 && blockNo < getSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void setBlockReader (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this.blockReader = blockReader;

    totalBlocks = diskLength / blockReader.blockSize;
    catalogBlocks = 0;
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
  public boolean isDirectory ()
  // ---------------------------------------------------------------------------------//
  {
    return false;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFile ()
  // ---------------------------------------------------------------------------------//
  {
    return false;
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
    throw new UnsupportedOperationException ("Cannot read() a file system");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("Cannot write() a file system");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()
  // ---------------------------------------------------------------------------------//
  {
    return diskLength;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()
  // ---------------------------------------------------------------------------------//
  {
    return totalBlocks;
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
  public String catalog ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();
    text.append (toText () + "\n");

    for (AppleFile file : files)
      if (file.isFileSystem () || file.isDirectory ())
        text.append (file.catalog () + "\n");
      else
        text.append (file + "\n");

    while (text.charAt (text.length () - 1) == '\n')
      text.deleteCharAt (text.length () - 1);

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-20.20s %-6s %,8d  %d %,7d  %2d  %3d ", fileName,
        fileSystemName, diskOffset, blockReader.interleave, totalBlocks,
        getTotalCatalogBlocks (), files.size ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File system ........... %s%n", fileSystemName));
    text.append (String.format ("Disk offset ........... %d%n", diskOffset));
    text.append (String.format ("Disk length ........... %,d%n", diskLength));
    text.append (String.format ("Total blocks .......... %,d%n", totalBlocks));
    text.append (String.format ("Catalog blocks ........ %d%n", catalogBlocks));
    text.append (String.format ("Block size ............ %d%n", blockReader.blockSize));
    text.append (String.format ("Interleave ............ %d%n", blockReader.interleave));
    text.append (String.format ("Total files ........... %d", files.size ()));

    return text.toString ();
  }

  // to remove later

}
