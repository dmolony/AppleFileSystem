package com.bytezone.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class BlockReader
// -----------------------------------------------------------------------------------//
{
  private static final int SECTOR_SIZE = 256;

  private static int[][] interleaves = { { //
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,       //
      17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 },   // no interleave
      { 0, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 15 },       // pascal
      { 0, 6, 12, 3, 9, 15, 14, 5, 11, 2, 8, 7, 13, 4, 10, 1 },       // CPM Dos
      { 0, 9, 3, 12, 6, 15, 1, 10, 4, 13, 7, 8, 2, 11, 5, 14 },       // CPM Prodos
      { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 } };     // test

  private final byte[] diskBuffer;
  private final int diskOffset;
  private final int diskLength;

  private final Path path;
  private String name;

  private AddressType addressType;      // BLOCK, SECTOR

  private int bytesPerBlock;            // 128, 256, 512, 1024
  private int interleave;               // 0, 1, 2
  private int blocksPerTrack;           // 4, 8, 13, 16, 32
  private int bytesPerTrack;            // 3328, 4096, 8192
  private int totalBlocks;

  private AppleBlock[] appleBlocks;

  public enum AddressType
  {
    BLOCK, SECTOR
  }

  // ---------------------------------------------------------------------------------//
  BlockReader (Path path)
  // ---------------------------------------------------------------------------------//
  {
    this.path = path;
    byte[] buffer = readAllBytes (path);

    diskBuffer = buffer;
    diskOffset = 0;
    diskLength = buffer.length == 143_488 ? 143_360 : buffer.length;

    name = path.toFile ().getName ();
  }

  // ---------------------------------------------------------------------------------//
  BlockReader (String name, byte[] diskBuffer, int diskOffset, int diskLength)
  // ---------------------------------------------------------------------------------//
  {
    Objects.checkFromIndexSize (diskOffset, diskLength, diskBuffer.length);

    this.diskBuffer = diskBuffer;
    this.diskOffset = diskOffset;
    this.diskLength = diskLength == 143_488 ? 143_360 : diskLength;

    this.name = name;
    this.path = null;
  }

  // ---------------------------------------------------------------------------------//
  BlockReader (BlockReader original)
  // ---------------------------------------------------------------------------------//
  {
    this.diskBuffer = original.diskBuffer;
    this.diskOffset = original.diskOffset;
    this.diskLength = original.diskLength;

    this.path = original.path;
    this.name = original.name;
  }

  // ---------------------------------------------------------------------------------//
  void setParameters (int bytesPerBlock, AddressType addressType, int interleave,
      int blocksPerTrack)
  // ---------------------------------------------------------------------------------//
  {
    this.bytesPerBlock = bytesPerBlock;
    this.addressType = Objects.requireNonNull (addressType, "Address type is null");
    this.interleave = interleave;
    this.blocksPerTrack = blocksPerTrack;

    bytesPerTrack = bytesPerBlock * blocksPerTrack;
    totalBlocks = (diskLength - 1) / bytesPerBlock + 1;   // includes partial blocks

    appleBlocks = new AppleBlock[totalBlocks];
  }

  // ---------------------------------------------------------------------------------//
  String getName ()
  // ---------------------------------------------------------------------------------//
  {
    return name;
  }

  // some 2img disks get it wrong
  // ---------------------------------------------------------------------------------//
  void fixIncorrectName (String correctName)
  // ---------------------------------------------------------------------------------//
  {
    this.name = Objects.requireNonNull (correctName, "Name is null");
  }

  // ---------------------------------------------------------------------------------//
  boolean isMagic (int offset, byte[] magic)
  // ---------------------------------------------------------------------------------//
  {
    return Utility.isMagic (diskBuffer, diskOffset + offset, magic);
  }

  // ---------------------------------------------------------------------------------//
  boolean byteAt (int offset, byte magic)
  // ---------------------------------------------------------------------------------//
  {
    return diskBuffer[diskOffset + offset] == magic;
  }

  // ---------------------------------------------------------------------------------//
  public AppleBlock getBlock (AppleFileSystem fs, int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    if (!isValidBlockNo (blockNo))
      return null;

    if (appleBlocks[blockNo] == null)                             // first time here
    {
      AppleBlock block = new BlockProdos (fs, blockNo);
      block.setBlockType (isEmpty (block) ? BlockType.EMPTY : BlockType.ORPHAN);
      appleBlocks[blockNo] = block;
    }

    return appleBlocks[blockNo];
  }

  // ---------------------------------------------------------------------------------//
  AppleBlock getBlock (AppleFileSystem fs, int blockNo, BlockType blockType)
  // ---------------------------------------------------------------------------------//
  {
    if (!isValidBlockNo (blockNo))
      return null;

    if (appleBlocks[blockNo] == null)                             // first time here
      appleBlocks[blockNo] = new BlockProdos (fs, blockNo);

    appleBlocks[blockNo].setBlockType (blockType);

    return appleBlocks[blockNo];
  }

  // ---------------------------------------------------------------------------------//
  public AppleBlock getSector (AppleFileSystem fs, int track, int sector)
  // ---------------------------------------------------------------------------------//
  {
    assert addressType == AddressType.SECTOR;

    int blockNo = track * blocksPerTrack + sector;

    if (!isValidSectorAddress (track, sector))
      return null;

    if (appleBlocks[blockNo] == null)
    {
      AppleBlock block = new BlockDos (fs, track, sector);
      block.setBlockType (isEmpty (block) ? BlockType.EMPTY : BlockType.ORPHAN);
      appleBlocks[blockNo] = block;
    }

    return appleBlocks[blockNo];
  }

  // ---------------------------------------------------------------------------------//
  AppleBlock getSector (AppleFileSystem fs, int track, int sector, BlockType blockType)
  // ---------------------------------------------------------------------------------//
  {
    assert addressType == AddressType.SECTOR;

    int blockNo = track * blocksPerTrack + sector;

    if (!isValidSectorAddress (track, sector))
      return null;

    if (appleBlocks[blockNo] == null)                             // first time here
      appleBlocks[blockNo] = new BlockDos (fs, track, sector);

    appleBlocks[blockNo].setBlockType (blockType);

    return appleBlocks[blockNo];
  }

  // ---------------------------------------------------------------------------------//
  AppleBlock getSector (AppleFileSystem fs, byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    assert addressType == AddressType.SECTOR;

    int track = buffer[offset] & 0xFF;
    int sector = buffer[offset + 1] & 0xFF;

    return getSector (fs, track, sector);
  }

  // ---------------------------------------------------------------------------------//
  AppleBlock getSector (AppleFileSystem fs, byte[] buffer, int offset,
      BlockType blockType)
  // ---------------------------------------------------------------------------------//
  {
    assert addressType == AddressType.SECTOR;

    int track = buffer[offset] & 0xFF;
    int sector = buffer[offset + 1] & 0xFF;

    return getSector (fs, track, sector, blockType);
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
        int offset = block.getTrackNo () * bytesPerTrack
            + interleaves[interleave][block.getSectorNo ()] * bytesPerBlock;
        System.arraycopy (diskBuffer, diskOffset + offset, blockBuffer, bufferOffset,
            bytesPerBlock);
        break;

      case BLOCK:
        if (interleave == 0)
        {
          int start = diskOffset + block.getBlockNo () * bytesPerBlock;
          int xfrBytes = Math.min (bytesPerBlock, diskBuffer.length - start);

          if (xfrBytes > 0)
            System.arraycopy (diskBuffer, start, blockBuffer, bufferOffset, xfrBytes);
          else
            System.out.printf ("Block %d out of range%n", block.getBlockNo ());
          break;
        }

        int sectorsPerBlock = bytesPerBlock / SECTOR_SIZE;

        for (int i = 0; i < sectorsPerBlock; i++)
        {
          offset = block.getTrackNo () * bytesPerTrack
              + interleaves[interleave][block.getSectorNo () * sectorsPerBlock + i]
                  * SECTOR_SIZE;

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
        int offset = block.getTrackNo () * bytesPerTrack
            + interleaves[interleave][block.getSectorNo ()] * bytesPerBlock;
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

        int base = block.getTrackNo () * bytesPerTrack;
        int sectorsPerBlock = bytesPerBlock / 256;

        for (int i = 0; i < sectorsPerBlock; i++)
        {
          offset = base
              + interleaves[interleave][block.getSectorNo () * sectorsPerBlock + i] * 256;
          System.arraycopy (blockBuffer, bufferOffset + i * 256, diskBuffer,
              diskOffset + offset, 256);
        }

        break;

      default:
        System.out.println ("Unknown address type: " + addressType);
        assert false;
        break;          // impossible
    }
  }

  // ---------------------------------------------------------------------------------//
  byte[] getDiskBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return diskBuffer;
  }

  // ---------------------------------------------------------------------------------//
  int getDiskOffset ()
  // ---------------------------------------------------------------------------------//
  {
    return diskOffset;
  }

  // ---------------------------------------------------------------------------------//
  int getDiskLength ()
  // ---------------------------------------------------------------------------------//
  {
    return diskLength;
  }

  // ---------------------------------------------------------------------------------//
  Path getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return path;
  }

  // ---------------------------------------------------------------------------------//
  int getBlockSize ()
  // ---------------------------------------------------------------------------------//
  {
    return bytesPerBlock;
  }

  // ---------------------------------------------------------------------------------//
  int getBlocksPerTrack ()
  // ---------------------------------------------------------------------------------//
  {
    return blocksPerTrack;
  }

  // ---------------------------------------------------------------------------------//
  int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  AddressType getAddressType ()
  // ---------------------------------------------------------------------------------//
  {
    return addressType;
  }

  // ---------------------------------------------------------------------------------//
  boolean isValidBlockNo (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockNo >= 0 && blockNo < totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  boolean isValidSectorAddress (int trackNo, int sectorNo)
  // ---------------------------------------------------------------------------------//
  {
    return sectorNo >= 0 && sectorNo < blocksPerTrack
        && isValidBlockNo (trackNo * blocksPerTrack + sectorNo);
  }

  // ---------------------------------------------------------------------------------//
  boolean isEmpty (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    for (byte b : block.read ())
      if (b != 0)               // won't work for CPM disks
        return false;

    return true;
  }

  // ---------------------------------------------------------------------------------//
  private byte[] readAllBytes (Path path)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      return Files.readAllBytes (path);
    }
    catch (IOException e)
    {
      e.printStackTrace ();
      return null;
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("File system offset .... %,d%n".formatted (diskOffset));
    text.append ("File system length .... %,d%n".formatted (diskLength));
    text.append ("Address type .......... %s%n".formatted (addressType));
    text.append ("Total blocks .......... %,d  (%<04X)%n".formatted (totalBlocks));
    text.append ("Bytes per block ....... %d%n".formatted (bytesPerBlock));
    text.append ("Blocks per track ...... %d%n".formatted (blocksPerTrack));
    text.append ("Interleave ............ %d".formatted (interleave));

    return text.toString ();
  }
}
