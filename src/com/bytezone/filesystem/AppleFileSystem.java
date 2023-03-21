package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public interface AppleFileSystem extends AppleFileContainer
// -----------------------------------------------------------------------------------//
{
  FileSystemType getFileSystemType ();

  String getFileName ();

  byte[] getDiskBuffer ();

  int getDiskOffset ();

  int getDiskLength ();

  boolean isHybrid ();

  BlockReader getBlockReader ();

  AppleBlock allocate ();

  // passed through to BlockReader

  AddressType getType ();             // BLOCK, SECTOR

  int getBlocksPerTrack ();

  public int getTotalBlocks ();

  public int getBlockSize ();

  int getFreeBlocks ();

  AppleBlock getBlock (int blockNo);

  AppleBlock getSector (int track, int sector);

  AppleBlock getSector (byte[] buffer, int offset);

  byte[] readBlock (AppleBlock block);

  byte[] readBlocks (List<AppleBlock> blocks);

  void writeBlock (AppleBlock block, byte[] buffer);

  void writeBlocks (List<AppleBlock> blocks, byte[] buffer);

  enum FileSystemType
  {
    DOS, PRODOS, PASCAL, CPM, NUFX, IMG2, NIB, DOS4, UNIDOS, ZIP, GZIP, HYBRID, DATA,
    BIN2, WOZ1, WOZ2, LBR
  }
}
