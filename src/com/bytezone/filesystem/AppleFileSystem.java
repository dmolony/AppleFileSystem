package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public interface AppleFileSystem extends AppleFile
// -----------------------------------------------------------------------------------//
{
  byte[] getDiskBuffer ();

  int getDiskOffset ();

  int getDiskLength ();

  boolean isHybrid ();

  AppleFile getAppleFile ();          // if is an embedded FileSystem

  BlockReader getBlockReader ();

  AppleBlock allocate ();

  // passed through to BlockReader

  AddressType getType ();             // BLOCK, SECTOR

  int getBlocksPerTrack ();

  int getFreeBlocks ();

  AppleBlock getBlock (int blockNo);

  AppleBlock getSector (int track, int sector);

  AppleBlock getSector (byte[] buffer, int offset);

  byte[] readBlock (AppleBlock block);

  byte[] readBlocks (List<AppleBlock> blocks);

  void writeBlock (AppleBlock block, byte[] buffer);

  void writeBlocks (List<AppleBlock> blocks, byte[] buffer);

  String toText ();

  enum FileSystemType
  {
    DOS, PRODOS, PASCAL, CPM, NUFX, IMG2, NIB, DOS4, UNIDOS, ZIP, GZIP, HYBRID, DATA,
    BIN2, WOZ1, WOZ2, LBR
  }
}
