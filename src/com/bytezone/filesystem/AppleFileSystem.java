package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public interface AppleFileSystem extends AppleContainer
// -----------------------------------------------------------------------------------//
{
  FileSystemType getFileSystemType ();

  String getFileName ();

  byte[] getDiskBuffer ();

  int getDiskOffset ();

  int getDiskLength ();

  boolean isHybridComponent ();

  BlockReader getBlockReader ();

  AppleBlock allocate ();

  // passed through to BlockReader

  AddressType getAddressType ();             // BLOCK, SECTOR

  int getBlocksPerTrack ();

  int getTotalBlocks ();

  int getBlockSize ();

  int getFreeBlocks ();

  boolean isValidAddress (int blockNo);

  boolean isValidAddress (int trackNo, int sectorNo);

  AppleBlock getBlock (int blockNo);

  AppleBlock getBlock (int blockNo, BlockType blockType);

  AppleBlock getSector (int track, int sector);

  AppleBlock getSector (byte[] buffer, int offset);

  AppleBlock getSector (int track, int sector, BlockType blockType);

  AppleBlock getSector (byte[] buffer, int offset, BlockType blockType);

  byte[] readBlock (AppleBlock block);

  byte[] readBlocks (List<AppleBlock> blocks);

  void writeBlock (AppleBlock block, byte[] buffer);

  void writeBlocks (List<AppleBlock> blocks, byte[] buffer);

  void setErrorMessage (String errorMessage);    // if file can't be read

  String getErrorMessage ();                     // if file can't be read

  enum FileSystemType
  {
    DOS3, PRODOS, PASCAL, CPM, NUFX, IMG2, NIB, DOS4, UNIDOS, ZIP, GZIP, HYBRID, DATA,
    BIN2, WOZ1, WOZ2, LBR, PASCAL_CODE
  }
}
