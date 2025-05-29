package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import java.security.InvalidParameterException;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public record DiskParameters (int bytesPerBlock, int interleave, int blocksPerTrack)
// -----------------------------------------------------------------------------------//
{
  private static final int SECTOR_SIZE = 256;

  //  bytesPerBlock;            // 128, 256, 512, 1024
  //  interleave;               // 0, 1, 2
  //  blocksPerTrack;           // 0, 4, 8, 13, 16, 32

  // ---------------------------------------------------------------------------------//
  public DiskParameters (int bytesPerBlock, int interleave, int blocksPerTrack)
  // ---------------------------------------------------------------------------------//
  {
    if (blocksPerTrack == 0)
    {
      if (bytesPerBlock == SECTOR_SIZE)
        throw new InvalidParameterException (
            "Must specify track size when sector size = " + SECTOR_SIZE);

      if (interleave > 0)
        throw new InvalidParameterException (
            "Must specify track size when interleave > 0");
    }

    this.bytesPerBlock = bytesPerBlock;
    this.interleave = interleave;
    this.blocksPerTrack = blocksPerTrack;
  }

  // ---------------------------------------------------------------------------------//
  private int totalBlocks (int diskLength)
  // ---------------------------------------------------------------------------------//
  {
    return (diskLength - 1) / bytesPerBlock + 1;        // includes partial blocks
  }

  // ---------------------------------------------------------------------------------//
  public AppleBlock[] getAppleBlockArray (Buffer dataBuffer)
  // ---------------------------------------------------------------------------------//
  {
    return new AppleBlock[totalBlocks (dataBuffer.length ())];
  }

  // ---------------------------------------------------------------------------------//
  public ByteCopier getByteCopier (Buffer dataBuffer)
  // ---------------------------------------------------------------------------------//
  {
    if (bytesPerBlock == SECTOR_SIZE)
      return new SingleSectorCopier (dataBuffer, this);

    if (interleave == 0)
      return new SingleBlockCopier (dataBuffer, this);

    return new MultipleSectorCopier (dataBuffer, this);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    formatText (text, "Bytes per block", 4, bytesPerBlock);
    formatText (text, "Blocks per track", 2, blocksPerTrack);
    formatText (text, "Interleave", 2, interleave);

    return Utility.rtrim (text);
  }
}
