package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public interface AppleBlock
// -----------------------------------------------------------------------------------//
{
  int getBlockNo ();

  int getTrackNo ();

  int getSectorNo ();

  boolean isValid ();

  byte[] read ();

  BlockType getBlockType ();

  public enum BlockType
  {
    EMPTY, OS_DATA, FILE_DATA, ORPHAN
  }
}
