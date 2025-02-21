package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public interface AppleBlock         // this could be renamed to Address
// -----------------------------------------------------------------------------------//
{
  AppleFileSystem getFileSystem ();

  int getBlockNo ();

  int getTrackNo ();

  int getSectorNo ();

  byte[] getBuffer ();

  void setBuffer (byte[] buffer);

  void setBlockType (BlockType blockType);

  BlockType getBlockType ();

  void setBlockSubType (String blockSubType);

  String getBlockSubType ();

  void setFileOwner (AppleFile appleFile);

  AppleFile getFileOwner ();

  boolean isFree ();

  boolean isDirty ();

  void markDirty ();

  void markClean ();

  void setUserData (Object userData);

  Object getUserData ();

  void dump ();

  public enum BlockType
  {
    EMPTY, FS_DATA, FILE_DATA, ORPHAN
  }
}
