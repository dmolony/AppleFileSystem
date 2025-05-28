package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class SingleSectorCopier implements ByteCopier
// -----------------------------------------------------------------------------------//
{
  private final Buffer dataBuffer;
  private final int bytesPerTrack;
  private final int interleave;

  // ---------------------------------------------------------------------------------//
  SingleSectorCopier (Buffer dataBuffer, int bytesPerTrack, int interleave)
  // ---------------------------------------------------------------------------------//
  {
    this.dataBuffer = dataBuffer;
    this.bytesPerTrack = bytesPerTrack;
    this.interleave = interleave;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void read (AppleBlock block, byte[] blockBuffer, int bufferOffset)
  // ---------------------------------------------------------------------------------//
  {
    byte[] diskBuffer = dataBuffer.data ();
    int diskOffset = dataBuffer.offset ();

    int diskBufferOffset = diskOffset + block.getTrackNo () * bytesPerTrack
        + interleaves[interleave][block.getSectorNo ()] * SECTOR_SIZE;
    int xfrBytes = Math.min (SECTOR_SIZE, diskBuffer.length - diskBufferOffset);

    if (xfrBytes > 0)
      System.arraycopy (diskBuffer, diskBufferOffset, blockBuffer, bufferOffset,
          xfrBytes);
    else
      System.out.printf ("Sector %d out of range%n", block.getBlockNo ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    byte[] blockBuffer = block.getBuffer ();
    int bufferOffset = 0;     // fix this later

    byte[] diskBuffer = dataBuffer.data ();
    int diskOffset = dataBuffer.offset ();

    int offset = block.getTrackNo () * bytesPerTrack
        + interleaves[interleave][block.getSectorNo ()] * SECTOR_SIZE;

    System.arraycopy (blockBuffer, bufferOffset, diskBuffer, diskOffset + offset,
        SECTOR_SIZE);
  }
}
