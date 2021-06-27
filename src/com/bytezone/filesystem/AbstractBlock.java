package com.bytezone.filesystem;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractBlock implements AppleBlock
// -----------------------------------------------------------------------------------//
{
  final AppleFileSystem fs;
  final int blockNo;
  final int track;
  final int sector;

  // ---------------------------------------------------------------------------------//
  AbstractBlock (AppleFileSystem fs, int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    this.fs = fs;
    this.blockNo = blockNo;

    int blocksPerTrack = fs.getBlocksPerTrack ();
    if (blocksPerTrack > 0)
    {
      track = blockNo / blocksPerTrack;
      sector = blockNo % blocksPerTrack;
    }
    else
    {
      track = -1;
      sector = -1;
    }

    assert fs.getType () == AddressType.BLOCK;
  }

  // ---------------------------------------------------------------------------------//
  AbstractBlock (AppleFileSystem fs, int track, int sector)
  // ---------------------------------------------------------------------------------//
  {
    this.fs = fs;
    this.blockNo = fs.getBlocksPerTrack () * track + sector;

    this.track = track;
    this.sector = sector;

    assert fs.getType () == AddressType.SECTOR;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlockNo ()
  // ---------------------------------------------------------------------------------//
  {
    return blockNo;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTrack ()
  // ---------------------------------------------------------------------------------//
  {
    return track;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSector ()
  // ---------------------------------------------------------------------------------//
  {
    return sector;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isValid ()
  // ---------------------------------------------------------------------------------//
  {
    return blockNo >= 0 && blockNo < fs.getSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return fs.readBlock (this);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write ()
  // ---------------------------------------------------------------------------------//
  {
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-6s: %4d  %3d  %3d", fs.getType (), blockNo, track, sector);
  }
}
