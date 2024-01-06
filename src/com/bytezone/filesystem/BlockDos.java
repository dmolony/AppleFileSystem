package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class BlockDos extends AbstractBlock
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  public BlockDos (AppleFileSystem fs, int track, int sector) //, BlockType blockType)
  // ---------------------------------------------------------------------------------//
  {
    super (fs, track, sector);  //, blockType);
  }

  // ---------------------------------------------------------------------------------//
  public BlockDos (AppleFileSystem fs, byte[] buffer, int offset) //, BlockType blockType)
  // ---------------------------------------------------------------------------------//
  {
    this (fs, buffer[offset] & 0xFF, buffer[offset + 1] & 0xFF);    //, blockType);
  }
}
