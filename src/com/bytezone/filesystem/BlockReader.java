package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
// Convert a byte array (disk buffer) into an array of AppleBlocks. The type of file
// system is not known at this point. DiskParameters must be provided before use.
// -----------------------------------------------------------------------------------//
public class BlockReader
// -----------------------------------------------------------------------------------//
{
  private static final int SECTOR_SIZE = 256;

  private final Buffer diskBuffer;
  private String name;

  private DiskParameters diskParameters;
  private ByteCopier byteCopier;

  private AppleBlock[] appleBlocks;
  private List<AppleBlock> dirtyBlocks = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  public BlockReader (Path path)
  // ---------------------------------------------------------------------------------//
  {
    if (!path.toFile ().exists ())
      throw new FileFormatException (String.format ("Path %s does not exist%n", path));

    byte[] buffer = readAllBytes (path);

    int diskLength = buffer.length == 143_488 ? 143_360 : buffer.length;

    diskBuffer = new Buffer (buffer, 0, diskLength);

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

    this.diskBuffer = new Buffer (diskBuffer, diskOffset, diskLength);
    this.name = name;
  }

  // ---------------------------------------------------------------------------------//
  public BlockReader (String name, Buffer dataBuffer)
  // ---------------------------------------------------------------------------------//
  {
    this.diskBuffer = dataBuffer.copyBuffer ();
    this.name = name;
  }

  // ---------------------------------------------------------------------------------//
  BlockReader (BlockReader original)
  // ---------------------------------------------------------------------------------//
  {
    diskBuffer = original.diskBuffer;       //.copyBuffer ();
    name = original.name;
  }

  // ---------------------------------------------------------------------------------//
  public void setParameters (DiskParameters diskParameters)
  // ---------------------------------------------------------------------------------//
  {
    this.diskParameters = diskParameters;

    appleBlocks = getAppleBlockArray ();
    byteCopier = getByteCopier ();
  }

  // ---------------------------------------------------------------------------------//
  private AppleBlock[] getAppleBlockArray ()
  // ---------------------------------------------------------------------------------//
  {
    int totalBlocks = (diskBuffer.length () - 1) / diskParameters.bytesPerBlock () + 1;

    return new AppleBlock[totalBlocks];
  }

  // ---------------------------------------------------------------------------------//
  private ByteCopier getByteCopier ()
  // ---------------------------------------------------------------------------------//
  {
    if (diskParameters.bytesPerBlock () == SECTOR_SIZE)
      return new SingleSectorCopier (diskBuffer, diskParameters);

    if (diskParameters.interleave () == 0)
      return new SingleBlockCopier (diskBuffer, diskParameters);

    return new MultipleSectorCopier (diskBuffer, diskParameters);
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
  //  boolean byteAt (int offset, byte magic)
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return diskBuffer.data ()[diskBuffer.offset () + offset] == magic;
  //  }

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
    if (!isValidAddress (track, sector))
      return null;

    int blockNo = track * diskParameters.blocksPerTrack () + sector;
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
    if (!isValidAddress (track, sector))
      return null;

    int blockNo = track * diskParameters.blocksPerTrack () + sector;
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
    int track = buffer[offset] & 0xFF;
    int sector = buffer[offset + 1] & 0xFF;

    return getSector (fs, track, sector, blockType);
  }

  // ---------------------------------------------------------------------------------//
  public byte[] read (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    byte[] blockBuffer = new byte[diskParameters.bytesPerBlock ()];

    byteCopier.read (block, blockBuffer, 0);

    return blockBuffer;         // this will be placed in the block's local buffer
  }

  // ---------------------------------------------------------------------------------//
  public byte[] read (List<AppleBlock> blocks)
  // ---------------------------------------------------------------------------------//
  {
    byte[] blockBuffer = new byte[diskParameters.bytesPerBlock () * blocks.size ()];

    int ptr = 0;
    for (int i = 0; i < blocks.size (); i++)
    {
      byteCopier.read (blocks.get (i), blockBuffer, ptr);
      ptr += diskParameters.bytesPerBlock ();
    }

    return blockBuffer;
  }

  // ---------------------------------------------------------------------------------//
  public void write (List<AppleBlock> blocks)
  // ---------------------------------------------------------------------------------//
  {
    for (int i = 0; i < blocks.size (); i++)
      write (blocks.get (i));
  }

  // write the block's local buffer back to the disk buffer
  // ---------------------------------------------------------------------------------//
  public void write (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    byteCopier.write (block);
  }

  // ---------------------------------------------------------------------------------//
  Buffer getDiskBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return diskBuffer;
  }

  // ---------------------------------------------------------------------------------//
  public DiskParameters getDiskParameters ()
  // ---------------------------------------------------------------------------------//
  {
    return diskParameters;
  }

  // ---------------------------------------------------------------------------------//
  int getDiskLength ()
  // ---------------------------------------------------------------------------------//
  {
    return diskBuffer.length ();
  }

  // ---------------------------------------------------------------------------------//
  int getBlockSize ()
  // ---------------------------------------------------------------------------------//
  {
    return diskParameters.bytesPerBlock ();
  }

  // ---------------------------------------------------------------------------------//
  int getInterleave ()
  // ---------------------------------------------------------------------------------//
  {
    return diskParameters.interleave ();
  }

  // ---------------------------------------------------------------------------------//
  int getBlocksPerTrack ()
  // ---------------------------------------------------------------------------------//
  {
    return diskParameters.blocksPerTrack ();
  }

  // ---------------------------------------------------------------------------------//
  int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return appleBlocks.length;
  }

  // ---------------------------------------------------------------------------------//
  boolean isValidAddress (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockNo >= 0 && blockNo < getTotalBlocks ();
  }

  // ---------------------------------------------------------------------------------//
  boolean isValidAddress (int trackNo, int sectorNo)
  // ---------------------------------------------------------------------------------//
  {
    return sectorNo >= 0 && sectorNo < diskParameters.blocksPerTrack ()
        && isValidAddress (trackNo * diskParameters.blocksPerTrack () + sectorNo);
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

    formatText (text, "Name", name);
    formatText (text, "Actual buffer length", 8, diskBuffer.data ().length);
    formatText (text, "File system offset", 8, diskBuffer.offset ());
    formatText (text, "File system length", 8, diskBuffer.length ());
    formatText (text, "Total blocks", 6, getTotalBlocks ());
    text.append ("\n");
    text.append (diskParameters);

    return Utility.rtrim (text);
  }
}
