package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public interface ByteCopier
// -----------------------------------------------------------------------------------//
{
  static final int SECTOR_SIZE = 256;

  static int[][] interleaves = { { //
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,       //
      17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 },   // no interleave
      { 0, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 15 },       // pascal
      { 0, 6, 12, 3, 9, 15, 14, 5, 11, 2, 8, 7, 13, 4, 10, 1 } };     // CPM Dos

  void read (AppleBlock block, byte[] blockBuffer, int bufferOffset);

  void write (AppleBlock block);
}
