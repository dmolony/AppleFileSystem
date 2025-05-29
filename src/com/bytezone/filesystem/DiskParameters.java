package com.bytezone.filesystem;

public record DiskParameters (int bytesPerBlock, int interleave, int blocksPerTrack)
{

  //  private int bytesPerBlock;            // 128, 256, 512, 1024
  //  private int interleave;               // 0, 1, 2
  //  private int blocksPerTrack;           // 4, 8, 13, 16, 32

  //  private int bytesPerTrack;            // 3328, 4096, 8192     (calculated)
}
