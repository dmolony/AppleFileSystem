package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class SingleBlockCopier implements ByteCopier
// -----------------------------------------------------------------------------------//
{
  private final int bytesPerBlock;

  private final byte[] diskBuffer;
  private final int diskOffset;

  // ---------------------------------------------------------------------------------//
  SingleBlockCopier (Buffer dataBuffer, DiskParameters diskParameters)
  // ---------------------------------------------------------------------------------//
  {
    bytesPerBlock = diskParameters.bytesPerBlock ();

    diskBuffer = dataBuffer.data ();
    diskOffset = dataBuffer.offset ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void read (AppleBlock block, byte[] blockBuffer, int bufferOffset)
  // ---------------------------------------------------------------------------------//
  {
    int diskBufferOffset = diskOffset + block.getBlockNo () * bytesPerBlock;
    int xfrBytes = Math.min (bytesPerBlock, diskBuffer.length - diskBufferOffset);

    if (xfrBytes > 0)
      System.arraycopy (diskBuffer, diskBufferOffset, blockBuffer, bufferOffset,
          xfrBytes);
    else
      System.out.printf ("Block %d out of range%n", block.getBlockNo ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    byte[] blockBuffer = block.getBuffer ();
    int bufferOffset = 0;     // fix this later

    System.arraycopy (blockBuffer, bufferOffset, diskBuffer,
        diskOffset + block.getBlockNo () * bytesPerBlock, bytesPerBlock);
  }
}
