package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatMeta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
// Convert a byte array (disk buffer) into an array of AppleBlocks. The type of
// file system is not known at this point. Block size and interleave must be specified
// before use.
// -----------------------------------------------------------------------------------//
public class BlockReader
// -----------------------------------------------------------------------------------//
{
  private static final int SECTOR_SIZE = 256;

  private static int[][] interleaves = { { //
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,       //
      17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 },   // no interleave
      { 0, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 15 },       // pascal
      { 0, 6, 12, 3, 9, 15, 14, 5, 11, 2, 8, 7, 13, 4, 10, 1 } };     // CPM Dos

  //      { 0, 9, 3, 12, 6, 15, 1, 10, 4, 13, 7, 8, 2, 11, 5, 14 },       // CPM Prodos
  //      { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 } };     // test

  private final Buffer dataRecord;

  private String name;

  private AddressType addressType;      // BLOCK, SECTOR

  private int bytesPerBlock;            // 128, 256, 512, 1024
  private int interleave;               // 0, 1, 2
  private int blocksPerTrack;           // 4, 8, 13, 16, 32
  private int bytesPerTrack;            // 3328, 4096, 8192
  private int totalBlocks;

  private AppleBlock[] appleBlocks;
  private List<AppleBlock> dirtyBlocks = new ArrayList<> ();

  public enum AddressType
  {
    BLOCK, SECTOR
  }

  // ---------------------------------------------------------------------------------//
  public BlockReader (Path path)
  // ---------------------------------------------------------------------------------//
  {
    if (!path.toFile ().exists ())
      throw new FileFormatException (String.format ("Path %s does not exist%n", path));

    byte[] buffer = readAllBytes (path);

    int diskLength = buffer.length == 143_488 ? 143_360 : buffer.length;

    dataRecord = new Buffer (buffer, 0, diskLength);

    name = path.toFile ().getName ();
  }

  // ---------------------------------------------------------------------------------//
  public BlockReader (String name, byte[] diskBuffer)
  // ---------------------------------------------------------------------------------//
  {
    this (name, diskBuffer, 0, diskBuffer.length);
  }

  // ---------------------------------------------------------------------------------//
  public BlockReader (String name, byte[] diskBuffer, int diskOffset, int diskLength)
  // ---------------------------------------------------------------------------------//
  {
    Objects.checkFromIndexSize (diskOffset, diskLength, diskBuffer.length);

    if (diskLength == 143_488)
      diskLength = 143_360;

    dataRecord = new Buffer (diskBuffer, diskOffset, diskLength);
    this.name = name;
  }

  // ---------------------------------------------------------------------------------//
  public BlockReader (String name, Buffer dataRecord)
  // ---------------------------------------------------------------------------------//
  {
    this.dataRecord = dataRecord.copyBuffer ();
    this.name = name;
  }

  // ---------------------------------------------------------------------------------//
  BlockReader (BlockReader original)
  // ---------------------------------------------------------------------------------//
  {
    dataRecord = original.dataRecord;       //.copyBuffer ();
    name = original.name;
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
    totalBlocks = (dataRecord.length () - 1)            //
        / bytesPerBlock + 1;                            // includes partial blocks

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
    return Utility.isMagic (dataRecord.data (), dataRecord.offset () + offset, magic);
  }

  // ---------------------------------------------------------------------------------//
  boolean byteAt (int offset, byte magic)
  // ---------------------------------------------------------------------------------//
  {
    return dataRecord.data ()[dataRecord.offset () + offset] == magic;
  }

  // this routine always reads the block (in order to set block type)
  // ---------------------------------------------------------------------------------//
  public AppleBlock getBlock (AppleFileSystem fs, int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    if (!isValidAddress (blockNo))
      return null;

    AppleBlock block = appleBlocks[blockNo];
    if (block == null)                             // first time here
    {
      block = new BlockProdos (fs, blockNo);
      block.setBlockType (isEmpty (block) ? BlockType.EMPTY : BlockType.ORPHAN);
      appleBlocks[blockNo] = block;
    }

    return block;
  }

  // this routine never reads the block (block type is provided)
  // ---------------------------------------------------------------------------------//
  AppleBlock getBlock (AppleFileSystem fs, int blockNo, BlockType blockType)
  // ---------------------------------------------------------------------------------//
  {
    if (!isValidAddress (blockNo))
      return null;

    AppleBlock block = appleBlocks[blockNo];
    if (block == null)                             // first time here
    {
      block = new BlockProdos (fs, blockNo);
      appleBlocks[blockNo] = block;
    }

    block.setBlockType (blockType);

    return block;
  }

  // this routine always reads the sector (in order to set block type)
  // ---------------------------------------------------------------------------------//
  public AppleBlock getSector (AppleFileSystem fs, int track, int sector)
  // ---------------------------------------------------------------------------------//
  {
    assert addressType == AddressType.SECTOR;

    if (!isValidAddress (track, sector))
      return null;

    int blockNo = track * blocksPerTrack + sector;
    AppleBlock block = appleBlocks[blockNo];

    if (block == null)                             // first time here
    {
      block = new BlockDos (fs, track, sector);
      block.setBlockType (isEmpty (block) ? BlockType.EMPTY : BlockType.ORPHAN);
      appleBlocks[blockNo] = block;
    }

    return block;
  }

  // this routine never reads the sector (block type is provided)
  // ---------------------------------------------------------------------------------//
  AppleBlock getSector (AppleFileSystem fs, int track, int sector, BlockType blockType)
  // ---------------------------------------------------------------------------------//
  {
    assert addressType == AddressType.SECTOR;

    if (!isValidAddress (track, sector))
      return null;

    int blockNo = track * blocksPerTrack + sector;
    AppleBlock block = appleBlocks[blockNo];

    if (block == null)                             // first time here
    {
      block = new BlockDos (fs, track, sector);
      appleBlocks[blockNo] = block;
    }

    block.setBlockType (blockType);

    return block;
  }

  // Get the sector pointed to by the track/sector at buffer[offset]
  // ---------------------------------------------------------------------------------//
  AppleBlock getSector (AppleFileSystem fs, byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    assert addressType == AddressType.SECTOR;

    int track = buffer[offset] & 0xFF;
    int sector = buffer[offset + 1] & 0xFF;

    return getSector (fs, track, sector);
  }

  // Get the sector pointed to by the track/sector at buffer[offset]
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

    return blockBuffer;         // this will be placed in the block's local buffer
  }

  // this doesn't belong here (BlockReader should only deal with single blocks)
  // ---------------------------------------------------------------------------------//
  public byte[] read (List<AppleBlock> blocks)
  // ---------------------------------------------------------------------------------//
  {
    byte[] blockBuffer = new byte[bytesPerBlock * blocks.size ()];

    for (int i = 0; i < blocks.size (); i++)
      read (blocks.get (i), blockBuffer, i * bytesPerBlock);

    return blockBuffer;
  }

  // copy the needed disk buffer bytes into the provided local buffer
  // this should fill the block's local buffer
  // ---------------------------------------------------------------------------------//
  private void read (AppleBlock block, byte[] blockBuffer, int bufferOffset)
  // ---------------------------------------------------------------------------------//
  {
    if (block == null)        // sparse file
      return;

    byte[] diskBuffer = dataRecord.data ();
    int diskOffset = dataRecord.offset ();

    switch (addressType)
    {
      case SECTOR:
        int start = diskOffset + block.getTrackNo () * bytesPerTrack
            + interleaves[interleave][block.getSectorNo ()] * bytesPerBlock;
        int xfrBytes = Math.min (bytesPerBlock, diskBuffer.length - start);

        if (xfrBytes > 0)
          System.arraycopy (diskBuffer, start, blockBuffer, bufferOffset, xfrBytes);
        else
          System.out.printf ("Sector %d out of range%n", block.getBlockNo ());

        break;

      case BLOCK:
        if (interleave == 0)
        {
          start = diskOffset + block.getBlockNo () * bytesPerBlock;
          xfrBytes = Math.min (bytesPerBlock, diskBuffer.length - start);

          if (xfrBytes > 0)
            System.arraycopy (diskBuffer, start, blockBuffer, bufferOffset, xfrBytes);
          else
            System.out.printf ("Block %d out of range%n", block.getBlockNo ());

          break;
        }

        // non-zero interleave
        int sectorsPerBlock = bytesPerBlock / SECTOR_SIZE;
        int destStart = bufferOffset;

        for (int i = 0; i < sectorsPerBlock; i++)
        {
          start = diskOffset + block.getTrackNo () * bytesPerTrack
              + interleaves[interleave][block.getSectorNo () * sectorsPerBlock + i]
                  * SECTOR_SIZE;
          xfrBytes = Math.min (SECTOR_SIZE, diskBuffer.length - start);

          if (xfrBytes > 0)
          {
            System.arraycopy (diskBuffer, start, blockBuffer, destStart, xfrBytes);
            destStart += SECTOR_SIZE;
          }
          else
          {
            System.out.printf ("Block %d out of range%n", block.getBlockNo ());
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
  public void write (List<AppleBlock> blocks)
  // ---------------------------------------------------------------------------------//
  {
    for (int i = 0; i < blocks.size (); i++)
      //      write (blocks.get (i), blockBuffer, i * bytesPerBlock);
      write (blocks.get (i));
  }

  // write the block's local buffer back to the disk buffer
  // ---------------------------------------------------------------------------------//
  public void write (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    byte[] blockBuffer = block.getBuffer ();
    int bufferOffset = 0;     // fix this later

    byte[] diskBuffer = dataRecord.data ();
    int diskOffset = dataRecord.offset ();

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

        // non-zero interleave
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
  Buffer getDiskBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return dataRecord;
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
  int getInterleave ()
  // ---------------------------------------------------------------------------------//
  {
    return interleave;
  }

  // ---------------------------------------------------------------------------------//
  AddressType getAddressType ()
  // ---------------------------------------------------------------------------------//
  {
    return addressType;
  }

  // ---------------------------------------------------------------------------------//
  boolean isValidAddress (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockNo >= 0 && blockNo < totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  boolean isValidAddress (int trackNo, int sectorNo)
  // ---------------------------------------------------------------------------------//
  {
    return sectorNo >= 0 && sectorNo < blocksPerTrack
        && isValidAddress (trackNo * blocksPerTrack + sectorNo);
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
  boolean isEmpty (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    for (byte b : block.getBuffer ())
      if (b != 0)               // won't work for CPM disks
        return false;

    return true;
  }

  // ---------------------------------------------------------------------------------//
  public void markDirty (AppleBlock dirtyBlock)
  // ---------------------------------------------------------------------------------//
  {
    assert dirtyBlock.isDirty ();

    if (!dirtyBlocks.contains (dirtyBlock))
      dirtyBlocks.add (dirtyBlock);
  }

  // ---------------------------------------------------------------------------------//
  public void markClean (AppleBlock dirtyBlock)
  // ---------------------------------------------------------------------------------//
  {
    assert !dirtyBlock.isDirty ();

    if (dirtyBlocks.contains (dirtyBlock))
      dirtyBlocks.remove (dirtyBlock);
  }

  // ---------------------------------------------------------------------------------//
  void clean ()
  // ---------------------------------------------------------------------------------//
  {
    for (AppleBlock block : dirtyBlocks)
    {
      write (block);
      block.markClean ();
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    formatMeta (text, "File system offset", 4, dataRecord.offset ());
    formatMeta (text, "File system length", 8, dataRecord.length ());
    formatMeta (text, "Address type", addressType.toString ());
    formatMeta (text, "Total blocks", 4, totalBlocks);
    formatMeta (text, "Bytes per block", 4, bytesPerBlock);
    formatMeta (text, "Blocks per track", 2, blocksPerTrack);
    formatMeta (text, "Interleave", 2, interleave);

    return Utility.rtrim (text);
  }
}
