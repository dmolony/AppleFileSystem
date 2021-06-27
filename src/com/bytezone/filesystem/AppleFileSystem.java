package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public interface AppleFileSystem extends AppleFile
// -----------------------------------------------------------------------------------//
{
  void setBlockReader (BlockReader blockReader);

  AppleBlock allocate ();

  // passed through to BlockReader

  AddressType getType ();

  int getBlocksPerTrack ();

  AppleBlock getBlock (int blockNo);

  AppleBlock getSector (int track, int sector);

  byte[] readBlock (AppleBlock block);

  byte[] readBlocks (List<AppleBlock> blocks);

  String toText ();
}
