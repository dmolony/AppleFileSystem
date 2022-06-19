package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public interface AppleFileSystem extends AppleFile
// -----------------------------------------------------------------------------------//
{
  byte[] getBuffer ();

  int getOffset ();

  BlockReader getBlockReader ();

  AppleBlock allocate ();

  public void readCatalog ();

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
}
