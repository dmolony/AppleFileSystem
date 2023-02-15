package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public interface AppleFileSystem extends AppleFile
// -----------------------------------------------------------------------------------//
{
  String getFileSystemName ();

  FileSystemType getFileSystemType ();

  byte[] getBuffer ();

  int getOffset ();

  void setHybrid ();

  boolean isHybrid ();

  BlockReader getBlockReader ();

  AppleBlock allocate ();

  // passed through to BlockReader

  AddressType getType ();             // BLOCK, SECTOR

  int getBlocksPerTrack ();

  AppleBlock getBlock (int blockNo);

  AppleBlock getSector (int track, int sector);

  byte[] readBlock (AppleBlock block);

  byte[] readBlocks (List<AppleBlock> blocks);

  void writeBlock (AppleBlock block, byte[] buffer);

  void writeBlocks (List<AppleBlock> blocks, byte[] buffer);

  String toText ();

  enum FileSystemType
  {
    DOS, PRODOS, PASCAL, CPM, NUFX
  }
}
