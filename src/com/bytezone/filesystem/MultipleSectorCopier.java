package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class MultipleSectorCopier implements ByteCopier
// -----------------------------------------------------------------------------------//
{
  private final int bytesPerTrack;
  private final int interleave;
  private final int sectorsPerBlock;

  private final byte[] diskBuffer;
  private final int diskOffset;

  // ---------------------------------------------------------------------------------//
  MultipleSectorCopier (Buffer dataBuffer, DiskParameters diskParameters)
  // ---------------------------------------------------------------------------------//
  {
    bytesPerTrack = diskParameters.bytesPerBlock () * diskParameters.blocksPerTrack ();
    interleave = diskParameters.interleave ();

    sectorsPerBlock = diskParameters.bytesPerBlock () / SECTOR_SIZE;

    diskBuffer = dataBuffer.data ();
    diskOffset = dataBuffer.offset ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void read (AppleBlock block, byte[] blockBuffer, int bufferOffset)
  // ---------------------------------------------------------------------------------//
  {
    int base = block.getTrackNo () * bytesPerTrack;

    for (int sectorNo = 0; sectorNo < sectorsPerBlock; sectorNo++)
    {
      int diskBufferOffset = diskOffset + base
          + interleaves[interleave][block.getSectorNo () * sectorsPerBlock + sectorNo]
              * SECTOR_SIZE;
      int xfrBytes = Math.min (SECTOR_SIZE, diskBuffer.length - diskBufferOffset);

      if (xfrBytes > 0)
      {
        System.arraycopy (diskBuffer, diskBufferOffset, blockBuffer, bufferOffset,
            xfrBytes);
        bufferOffset += SECTOR_SIZE;
      }
      else
        System.out.printf ("Block %d, Sector %d out of range%n", block.getBlockNo (),
            sectorNo);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    byte[] blockBuffer = block.getBuffer ();
    int bufferOffset = 0;     // fix this later

    int base = block.getTrackNo () * bytesPerTrack;

    for (int sectorNo = 0; sectorNo < sectorsPerBlock; sectorNo++)
    {
      int offset = base
          + interleaves[interleave][block.getSectorNo () * sectorsPerBlock + sectorNo]
              * SECTOR_SIZE;
      System.arraycopy (blockBuffer, bufferOffset + sectorNo * SECTOR_SIZE, diskBuffer,
          diskOffset + offset, SECTOR_SIZE);
    }
  }
}
