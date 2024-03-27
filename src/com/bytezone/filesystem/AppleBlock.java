package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public interface AppleBlock         // this could be renamed to Address
// -----------------------------------------------------------------------------------//
{
  AppleFileSystem getFileSystem ();

  int getBlockNo ();

  int getTrackNo ();

  int getSectorNo ();

  byte[] read ();

  void setBlockType (BlockType blockType);

  BlockType getBlockType ();

  void setBlockSubType (String blockSubType);

  String getBlockSubType ();

  void setFileOwner (AppleFile appleFile);

  AppleFile getFileOwner ();

  void setUserData (Object userData);

  Object getUserData ();

  public enum BlockType
  {
    EMPTY, FS_DATA, FILE_DATA, ORPHAN
  }
}
