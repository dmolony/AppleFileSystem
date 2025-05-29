package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class SingleBlockCopier implements ByteCopier
// -----------------------------------------------------------------------------------//
{
  private final Buffer dataBuffer;
  private final int bytesPerBlock;
  private final DiskParameters diskParameters;

  // ---------------------------------------------------------------------------------//
  SingleBlockCopier (Buffer dataBuffer, DiskParameters diskParameters)
  // ---------------------------------------------------------------------------------//
  {
    this.dataBuffer = dataBuffer;
    this.diskParameters = diskParameters;

    bytesPerBlock = diskParameters.bytesPerBlock ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void read (AppleBlock block, byte[] blockBuffer, int bufferOffset)
  // ---------------------------------------------------------------------------------//
  {
    byte[] diskBuffer = dataBuffer.data ();

    int diskBufferOffset = dataBuffer.offset () + block.getBlockNo () * bytesPerBlock;
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

    byte[] diskBuffer = dataBuffer.data ();
    int diskOffset = dataBuffer.offset ();

    System.arraycopy (blockBuffer, bufferOffset, diskBuffer,
        diskOffset + block.getBlockNo () * bytesPerBlock, bytesPerBlock);
  }
}
