package com.bytezone.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

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
      { 0, 6, 12, 3, 9, 15, 14, 5, 11, 2, 8, 7, 13, 4, 10, 1 } };     // CPM

  private final byte[] diskBuffer;
  private final int diskOffset;
  private final int diskLength;

  private Path path;
  private String name;

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
  public BlockReader (Path path)
  // ---------------------------------------------------------------------------------//
  {
    this.path = path;
    byte[] buffer = readAllBytes (path);

    diskBuffer = buffer;
    diskOffset = 0;

    if (buffer.length == 143_488)
      diskLength = 143_360;
    else
      diskLength = buffer.length;

    name = path.toFile ().getName ();
  }

  // ---------------------------------------------------------------------------------//
  public BlockReader (String name, byte[] diskBuffer, int diskOffset, int diskLength)
  // ---------------------------------------------------------------------------------//
  {
    Objects.checkFromIndexSize (diskOffset, diskLength, diskBuffer.length);

    if (diskLength == 143_488)
      diskLength = 143_360;

    this.diskBuffer = diskBuffer;
    this.diskOffset = diskOffset;
    this.diskLength = diskLength;

    this.name = name;
  }

  // ---------------------------------------------------------------------------------//
  public BlockReader (BlockReader original)
  // ---------------------------------------------------------------------------------//
  {
    this.diskBuffer = original.diskBuffer;
    this.diskOffset = original.diskOffset;
    this.diskLength = original.diskLength;

    this.path = original.path;
    this.name = original.name;
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
    totalBlocks = (diskLength - 1) / bytesPerBlock + 1;   // includes partial blocks
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
    this.name = correctName;
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
  public AppleBlock getSector (AppleFileSystem fs, byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    int track = buffer[offset] & 0xFF;
    int sector = buffer[++offset] & 0xFF;

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

          //          if (start + bytesPerBlock <= diskBuffer.length)
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
  Path getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return path;
  }

  // ---------------------------------------------------------------------------------//
  boolean isValidBlockNo (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockNo >= 0 && blockNo < totalBlocks;
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

    text.append (String.format ("File system offset .... %,d%n", diskOffset));
    text.append (String.format ("File system length .... %,d%n", diskLength));
    text.append (String.format ("Address type .......... %s%n", addressType));
    text.append (String.format ("Total blocks .......... %,d%n", totalBlocks));
    text.append (String.format ("Bytes per block ....... %d%n", bytesPerBlock));
    text.append (String.format ("Blocks per track ...... %d%n", blocksPerTrack));
    text.append (String.format ("Interleave ............ %d", interleave));

    return text.toString ();
  }
}
