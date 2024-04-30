package com.bytezone.filesystem;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsDos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static Locale US = Locale.US;                 // to force 3 character months
  protected static final DateTimeFormatter sdf =
      DateTimeFormatter.ofPattern ("dd-LLL-yy HH:mm:ss", US);

  static final int ENTRY_SIZE = 35;

  protected BitSet volumeBitMap;
  protected int dosVersion;
  protected int volumeNumber;
  protected int maxTSpairs;
  protected byte direction;
  protected int lastTrackAllocated;
  protected int tracksPerDisk;
  protected int sectorsPerTrack;
  protected int bytesPerSector;

  protected List<String> deletedFiles = new ArrayList<> ();
  protected List<String> failedFiles = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  FsDos (BlockReader blockReader, FileSystemType fileSystemType)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, fileSystemType);
  }

  // ---------------------------------------------------------------------------------//
  public int getTracksPerDisk ()
  // ---------------------------------------------------------------------------------//
  {
    return tracksPerDisk;
  }

  // ---------------------------------------------------------------------------------//
  public int getSectorsPerTrack ()
  // ---------------------------------------------------------------------------------//
  {
    return sectorsPerTrack;
  }

  // ---------------------------------------------------------------------------------//
  String getFileTypeText (int fileType)
  // ---------------------------------------------------------------------------------//
  {
    return switch (fileType)
    {
      case 0x00 -> "T";
      case 0x01 -> "I";
      case 0x02 -> "A";
      case 0x04 -> "B";
      case 0x08 -> "S";
      case 0x10 -> "R";
      case 0x20 -> "B";
      case 0x40 -> "B";
      default -> "?";                       // should never happen
    };
  }

  // ---------------------------------------------------------------------------------//
  String getBlockSubTypeText (int fileType)
  // ---------------------------------------------------------------------------------//
  {
    return switch (fileType)
    {
      case 0x00 -> "TEXT";
      case 0x01 -> "INT BASIC";
      case 0x02 -> "APPLESOFT";
      case 0x04 -> "BINARY";
      case 0x08 -> "S";
      case 0x10 -> "R";
      case 0x20 -> "B";
      case 0x40 -> "B";
      default -> "?";                       // should never happen
    };
  }

  // ---------------------------------------------------------------------------------//
  protected void createVolumeBitMap (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    int blocksPerTrack = blockReader.getBlocksPerTrack ();
    int totalBlocks = blockReader.getTotalBlocks ();
    int totalTracks = totalBlocks / blocksPerTrack;

    volumeBitMap = new BitSet (totalBlocks);

    int ptr = 0x38;
    for (int track = 0; track < totalTracks; track++)
    {
      int bits = Utility.unsignedIntBigEndian (buffer, ptr);
      for (int sector = blocksPerTrack - 1; sector >= 0; sector--)
      {
        if ((bits & 0x80000000) != 0)
          volumeBitMap.set (track * blocksPerTrack + sector);
        bits <<= 1;
      }
      ptr += 4;
    }

    freeBlocks = volumeBitMap.cardinality ();
  }

  // ---------------------------------------------------------------------------------//
  void flagDosSectors ()
  // ---------------------------------------------------------------------------------//
  {
    for (int blockNo = 0; blockNo < 48; blockNo++)
    {
      BlockType blockType = getBlock (blockNo).getBlockType ();
      if (blockType != BlockType.EMPTY            // has data AND
          && (blockType != BlockType.ORPHAN       //   is owned by a data file OR
              || volumeBitMap.get (blockNo)))     //   is allocated
        return;
    }

    for (int blockNo = 0; blockNo < 48; blockNo++)
    {
      AppleBlock block = getBlock (blockNo);
      if (block.getBlockType () == BlockType.ORPHAN)
      {
        block.setBlockType (BlockType.FS_DATA);
        block.setBlockSubType ("DOS");
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("----- DOS Header ------%n"));
    text.append (String.format ("Dos version ........... %02X%n", dosVersion));
    text.append (String.format ("Volume number ......... %02X  %<,7d%n", volumeNumber));
    text.append (String.format ("Max TS pairs .......... %02X  %<,7d%n", maxTSpairs));
    text.append (
        String.format ("Last track allocated .. %02X  %<,7d%n", lastTrackAllocated));
    text.append (String.format ("Direction ............. %02X  %<,7d%n", direction));
    text.append (String.format ("Tracks per disk ....... %02X  %<,7d%n", tracksPerDisk));
    text.append (
        String.format ("Sectors per track ..... %02X  %<,7d%n", sectorsPerTrack));
    text.append (String.format ("Bytes per sector ...... %03X  %<,6d%n", bytesPerSector));

    return Utility.rtrim (text);
  }
}
