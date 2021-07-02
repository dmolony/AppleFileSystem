package com.bytezone.filesystem;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractBlock implements AppleBlock
// -----------------------------------------------------------------------------------//
{
  final AppleFileSystem fileSystem;

  final int blockNo;
  final int track;
  final int sector;

  // ---------------------------------------------------------------------------------//
  AbstractBlock (AppleFileSystem fileSystem, int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystem = fileSystem;
    this.blockNo = blockNo;

    int blocksPerTrack = fileSystem.getBlocksPerTrack ();
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

    assert fileSystem.getType () == AddressType.BLOCK;
  }

  // ---------------------------------------------------------------------------------//
  AbstractBlock (AppleFileSystem fileSystem, int track, int sector)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystem = fileSystem;
    this.blockNo = fileSystem.getBlocksPerTrack () * track + sector;

    this.track = track;
    this.sector = sector;

    assert fileSystem.getType () == AddressType.SECTOR;
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
    return blockNo >= 0 && blockNo < fileSystem.getSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystem.readBlock (this);
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
    return String.format ("%-6s: %4d  %3d  %3d", fileSystem.getType (), blockNo, track,
        sector);
  }
}
