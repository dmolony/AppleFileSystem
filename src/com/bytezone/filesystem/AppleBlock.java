package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public interface AppleBlock         // this could be renamed to Address
// -----------------------------------------------------------------------------------//
{
  int getBlockNo ();

  int getTrackNo ();

  int getSectorNo ();

  boolean isValid ();

  byte[] read ();

  void setBlockType (BlockType blockType);

  BlockType getBlockType ();

  void setBlockSubType (String blockSubType);

  String getBlockSubType ();

  public enum BlockType
  {
    EMPTY, FS_DATA, FILE_DATA, ORPHAN
  }
}
