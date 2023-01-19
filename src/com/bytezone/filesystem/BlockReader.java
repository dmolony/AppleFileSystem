package com.bytezone.filesystem;

import java.util.List;
import java.util.Objects;

// -----------------------------------------------------------------------------------//
public class BlockReader
// -----------------------------------------------------------------------------------//
{
  private static final int SECTOR_SIZE = 256;

  private static int[][] interleaves = { { //
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,       //
      17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 },   // no interleave
      { 0, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 15 },       // pascal
      { 0, 6, 12, 3, 9, 15, 14, 5, 11, 2, 8, 7, 13, 4, 10, 1 } };     // CPM

  final byte[] diskBuffer;
  final int diskOffset;
  final int length;

  AddressType addressType;      // BLOCK, SECTOR
  int bytesPerBlock;            // 256, 512, 1024
  int interleave;               // 0, 1, 2
  int blocksPerTrack;           // 4, 8, 13, 16, 32
  int bytesPerTrack;            // 3328, 4096, 8192
  int totalBlocks;

  enum AddressType
  {
    BLOCK, SECTOR
  }

  // ---------------------------------------------------------------------------------//
  public BlockReader (byte[] diskBuffer, int diskOffset, int length)
  // ---------------------------------------------------------------------------------//
  {
    Objects.checkFromIndexSize (diskOffset, length, diskBuffer.length);

    if (length == 143_488)
      length = 143_360;

    this.diskBuffer = diskBuffer;
    this.diskOffset = diskOffset;
    this.length = length;
  }

  // ---------------------------------------------------------------------------------//
  public BlockReader (BlockReader clone)
  // ---------------------------------------------------------------------------------//
  {
    this.diskBuffer = clone.diskBuffer;
    this.diskOffset = clone.diskOffset;
    this.length = clone.length;
  }

  // ---------------------------------------------------------------------------------//
  public void setParameters (int bytesPerBlock, AddressType addressType, int interleave,
      int blocksPerTrack)
  // ---------------------------------------------------------------------------------//
  {
    this.bytesPerBlock = bytesPerBlock;
    this.addressType = Objects.requireNonNull (addressType, "Address type is null");
    this.interleave = interleave;
    this.blocksPerTrack = blocksPerTrack;

    bytesPerTrack = bytesPerBlock * blocksPerTrack;
    totalBlocks = length / bytesPerBlock;
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
  public byte[] read (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    byte[] blockBuffer = new byte[bytesPerBlock];

    read (block, blockBuffer, 0);

    return blockBuffer;
  }

  // ---------------------------------------------------------------------------------//
  public byte[] read (List<AppleBlock> blocks)
  // ---------------------------------------------------------------------------------//
  {
    byte[] blockBuffer = new byte[bytesPerBlock * blocks.size ()];

    for (int i = 0; i < blocks.size (); i++)
      read (blocks.get (i), blockBuffer, i * bytesPerBlock);

    return blockBuffer;
  }

  // ---------------------------------------------------------------------------------//
  private void read (AppleBlock block, byte[] blockBuffer, int bufferOffset)
  // ---------------------------------------------------------------------------------//
  {
    if (block == null)        // sparse file
      return;

    switch (addressType)
    {
      case SECTOR:
        assert bytesPerBlock == SECTOR_SIZE;
        int offset = block.getTrack () * bytesPerTrack
            + interleaves[interleave][block.getSector ()] * bytesPerBlock;
        System.arraycopy (diskBuffer, diskOffset + offset, blockBuffer, bufferOffset,
            bytesPerBlock);
        break;

      case BLOCK:
        if (interleave == 0)
        {
          int start = diskOffset + block.getBlockNo () * bytesPerBlock;
          if (start + bytesPerBlock <= diskBuffer.length)
            System.arraycopy (diskBuffer, start, blockBuffer, bufferOffset, bytesPerBlock);
          else
            System.out.printf ("Block %d out of range%n", block.getBlockNo ());
          break;
        }

        int sectorsPerBlock = bytesPerBlock / SECTOR_SIZE;

        for (int i = 0; i < sectorsPerBlock; i++)
        {
          offset = block.getTrack () * bytesPerTrack
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
        break;
    }
  }

  // ---------------------------------------------------------------------------------//
  public void write (AppleBlock block, byte[] blockBuffer)
  // ---------------------------------------------------------------------------------//
  {
    write (block, blockBuffer, 0);
  }

  // ---------------------------------------------------------------------------------//
  public void write (List<AppleBlock> blocks, byte[] blockBuffer)
  // ---------------------------------------------------------------------------------//
  {
    for (int i = 0; i < blocks.size (); i++)
      write (blocks.get (i), blockBuffer, i * bytesPerBlock);
  }

  // ---------------------------------------------------------------------------------//
  private void write (AppleBlock block, byte[] blockBuffer, int bufferOffset)
  // ---------------------------------------------------------------------------------//
  {
    switch (addressType)
    {
      case SECTOR:
        int offset = block.getTrack () * bytesPerTrack
            + interleaves[interleave][block.getSector ()] * bytesPerBlock;
        System.arraycopy (blockBuffer, bufferOffset, diskBuffer, diskOffset + offset,
            bytesPerBlock);
        break;

      case BLOCK:
        if (interleave == 0)
        {
          System.arraycopy (blockBuffer, bufferOffset, diskBuffer,
              diskOffset + block.getBlockNo () * bytesPerBlock, bytesPerBlock);
          break;
        }

        int base = block.getTrack () * bytesPerTrack;
        int sectorsPerBlock = bytesPerBlock / 256;

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
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File offset ........... %,d%n", diskOffset));
    text.append (String.format ("File length ........... %,d%n", length));
    text.append (String.format ("Total blocks .......... %,d%n", totalBlocks));
    text.append (String.format ("Block size ............ %d%n", bytesPerBlock));
    text.append (String.format ("Interleave ............ %d", interleave));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("Type: %s, BlockSize: %d, Interleave: %d, BlocksPerTrack: %2d",
        addressType, bytesPerBlock, interleave, blocksPerTrack);
  }
}
