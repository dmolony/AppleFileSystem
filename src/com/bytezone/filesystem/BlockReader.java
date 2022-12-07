package com.bytezone.filesystem;

import java.util.List;
import java.util.Objects;

// -----------------------------------------------------------------------------------//
public class BlockReader
// -----------------------------------------------------------------------------------//
{
  private static final int SECTOR_SIZE = 256;

  private int[][] interleaves = { { //
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,       //
      17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 },   // no interleave
      { 0, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 15 },       // 
      { 0, 6, 12, 3, 9, 15, 14, 5, 11, 2, 8, 7, 13, 4, 10, 1 } };     // CPM

  final AddressType addressType;      // BLOCK, SECTOR
  final int blockSize;                // 256, 512, 1024
  final int interleave;               // 0, 1, 2
  final int blocksPerTrack;           // 4, 8, 13, 16, 32

  final int trackSize;                // bytes per track

  enum AddressType
  {
    BLOCK, SECTOR
  }

  // ---------------------------------------------------------------------------------//
  public BlockReader (int blockSize, AddressType addressType, int interleave, int blocksPerTrack)
  // ---------------------------------------------------------------------------------//
  {
    this.blockSize = blockSize;
    this.addressType = Objects.requireNonNull (addressType, "Address type is null");
    this.interleave = interleave;
    this.blocksPerTrack = blocksPerTrack;

    trackSize = blocksPerTrack * blockSize;
  }

  // ---------------------------------------------------------------------------------//
  public AppleBlock getBlock (AppleFileSystem fs, int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    assert addressType == AddressType.BLOCK;
    return new BlockProdos (fs, blockNo);
  }

  // ---------------------------------------------------------------------------------//
  public AppleBlock getSector (AppleFileSystem fs, int track, int sector)
  // ---------------------------------------------------------------------------------//
  {
    assert addressType == AddressType.SECTOR;
    return new BlockDos (fs, track, sector);
  }

  // ---------------------------------------------------------------------------------//
  public byte[] read (byte[] diskBuffer, int diskOffset, AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    byte[] blockBuffer = new byte[blockSize];

    read (diskBuffer, diskOffset, block, blockBuffer, 0);

    return blockBuffer;
  }

  // ---------------------------------------------------------------------------------//
  public byte[] read (byte[] diskBuffer, int diskOffset, List<AppleBlock> blocks)
  // ---------------------------------------------------------------------------------//
  {
    byte[] blockBuffer = new byte[blockSize * blocks.size ()];

    for (int i = 0; i < blocks.size (); i++)
      read (diskBuffer, diskOffset, blocks.get (i), blockBuffer, i * blockSize);

    return blockBuffer;
  }

  // ---------------------------------------------------------------------------------//
  private void read (byte[] diskBuffer, int diskOffset, AppleBlock block, byte[] blockBuffer,
      int bufferOffset)
  // ---------------------------------------------------------------------------------//
  {
    if (block == null)        // sparse file
      return;

    switch (addressType)
    {
      case SECTOR:
        assert blockSize == SECTOR_SIZE;
        int offset =
            block.getTrack () * trackSize + interleaves[interleave][block.getSector ()] * blockSize;
        System.arraycopy (diskBuffer, diskOffset + offset, blockBuffer, bufferOffset, blockSize);
        break;

      case BLOCK:
        if (interleave == 0)
        {
          int start = diskOffset + block.getBlockNo () * blockSize;
          if (start + blockSize <= diskBuffer.length)
            System.arraycopy (diskBuffer, start, blockBuffer, bufferOffset, blockSize);
          else
            System.out.printf ("Block %d out of range%n", block.getBlockNo ());
          break;
        }

        int sectorsPerBlock = blockSize / SECTOR_SIZE;

        for (int i = 0; i < sectorsPerBlock; i++)
        {
          offset = block.getTrack () * trackSize
              + interleaves[interleave][block.getSector () * sectorsPerBlock + i] * SECTOR_SIZE;
          if (diskOffset + offset + SECTOR_SIZE <= diskBuffer.length)
            System.arraycopy (diskBuffer, diskOffset + offset, blockBuffer,
                bufferOffset + i * SECTOR_SIZE, SECTOR_SIZE);
          else
          {
            System.out.printf ("Block %d out of range (%d in %d)%n", block.getBlockNo (),
                diskOffset + offset + SECTOR_SIZE, diskBuffer.length);
            break;
          }
        }

        break;

      default:
        System.out.println ("Unknown address type: " + addressType);
        assert false;
        break;          // impossible
    }
  }

  // ---------------------------------------------------------------------------------//
  public void write (byte[] diskBuffer, int diskOffset, AppleBlock block, byte[] blockBuffer)
  // ---------------------------------------------------------------------------------//
  {
    write (diskBuffer, diskOffset, block, blockBuffer, 0);
  }

  // ---------------------------------------------------------------------------------//
  public void write (byte[] diskBuffer, int diskOffset, List<AppleBlock> blocks, byte[] blockBuffer)
  // ---------------------------------------------------------------------------------//
  {
    for (int i = 0; i < blocks.size (); i++)
      write (diskBuffer, diskOffset, blocks.get (i), blockBuffer, i * blockSize);
  }

  // ---------------------------------------------------------------------------------//
  private void write (byte[] diskBuffer, int diskOffset, AppleBlock block, byte[] blockBuffer,
      int bufferOffset)
  // ---------------------------------------------------------------------------------//
  {
    switch (addressType)
    {
      case SECTOR:
        int offset =
            block.getTrack () * trackSize + interleaves[interleave][block.getSector ()] * blockSize;
        System.arraycopy (blockBuffer, bufferOffset, diskBuffer, diskOffset + offset, blockSize);
        break;

      case BLOCK:
        if (interleave == 0)
        {
          System.arraycopy (blockBuffer, bufferOffset, diskBuffer,
              diskOffset + block.getBlockNo () * blockSize, blockSize);
          break;
        }

        int base = block.getTrack () * trackSize;
        int sectorsPerBlock = blockSize / 256;

        for (int i = 0; i < sectorsPerBlock; i++)
        {
          offset = base + interleaves[interleave][block.getSector () * sectorsPerBlock + i] * 256;
          System.arraycopy (blockBuffer, bufferOffset + i * 256, diskBuffer, diskOffset + offset,
              256);
        }

        break;

      default:
        System.out.println ("Unknown address type: " + addressType);
        assert false;
        break;          // impossible
    }

  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("Type: %s, BlockSize: %d, Interleave: %d, BlocksPerTrack: %2d",
        addressType, blockSize, interleave, blocksPerTrack);
  }
}
