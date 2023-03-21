package com.bytezone.filesystem;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractBlock implements AppleBlock
// -----------------------------------------------------------------------------------//
{
  protected final AppleFileSystem fileSystem;

  protected final int blockNo;
  protected final int trackNo;
  protected final int sectorNo;

  protected final boolean valid;

  // ---------------------------------------------------------------------------------//
  AbstractBlock (AppleFileSystem fileSystem, int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystem = fileSystem;
    this.blockNo = blockNo;

    int blocksPerTrack = fileSystem.getBlocksPerTrack ();
    if (blocksPerTrack > 0)
    {
      trackNo = blockNo / blocksPerTrack;
      sectorNo = blockNo % blocksPerTrack;
    }
    else
    {
      trackNo = -1;
      sectorNo = -1;
    }

    valid = blockNo >= 0 && blockNo < fileSystem.getTotalBlocks ();

    assert fileSystem.getType () == AddressType.BLOCK;
  }

  // ---------------------------------------------------------------------------------//
  AbstractBlock (AppleFileSystem fileSystem, int trackNo, int sectorNo)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystem = fileSystem;
    this.blockNo = fileSystem.getBlocksPerTrack () * trackNo + sectorNo;

    this.trackNo = trackNo;
    this.sectorNo = sectorNo;

    valid = sectorNo >= 0 && sectorNo < fileSystem.getBlocksPerTrack () && blockNo >= 0
        && blockNo < fileSystem.getTotalBlocks ();

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
  public int getTrackNo ()
  // ---------------------------------------------------------------------------------//
  {
    return trackNo;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSectorNo ()
  // ---------------------------------------------------------------------------------//
  {
    return sectorNo;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isValid ()
  // ---------------------------------------------------------------------------------//
  {
    return valid;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystem.readBlock (this);
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public void write (byte[] buffer)
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    fileSystem.write (buffer);
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public void write ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    // ????
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-6s  B:%4d,  T:%3d,  S:%3d", fileSystem.getType (), blockNo,
        trackNo, sectorNo);
  }
}
