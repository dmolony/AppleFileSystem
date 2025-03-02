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

  public static final int FILE_TYPE_TEXT = 0x00;
  public static final int FILE_TYPE_INTEGER_BASIC = 0x01;
  public static final int FILE_TYPE_APPLESOFT = 0x02;
  public static final int FILE_TYPE_BINARY = 0x04;
  public static final int FILE_TYPE_S = 0x08;
  public static final int FILE_TYPE_R = 0x10;
  public static final int FILE_TYPE_BINARY_B = 0x20;
  public static final int FILE_TYPE_BINARY_L = 0x40;

  static final int ENTRY_SIZE = 35;
  static final int HEADER_SIZE = 0x0B;

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
  protected boolean checkDuplicate (List<AppleBlock> catalogSectors,
      AppleBlock testSector)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleBlock catalogSector : catalogSectors)
      if (catalogSector.getBlockNo () == testSector.getBlockNo ())
        return true;

    return false;
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
      case FILE_TYPE_TEXT -> "T";
      case FILE_TYPE_INTEGER_BASIC -> "I";
      case FILE_TYPE_APPLESOFT -> "A";
      case FILE_TYPE_BINARY -> "B";
      case FILE_TYPE_S -> "S";
      case FILE_TYPE_R -> "R";
      case FILE_TYPE_BINARY_B -> "B";
      case FILE_TYPE_BINARY_L -> "L";       // Dos 4 uses this
      default -> "?";                       // should never happen
    };
  }

  // ---------------------------------------------------------------------------------//
  String getBlockSubTypeText (int fileType)
  // ---------------------------------------------------------------------------------//
  {
    return switch (fileType)
    {
      case FILE_TYPE_TEXT -> "TEXT";
      case FILE_TYPE_INTEGER_BASIC -> "INT BASIC";
      case FILE_TYPE_APPLESOFT -> "APPLESOFT";
      case FILE_TYPE_BINARY -> "BINARY";
      case FILE_TYPE_S -> "S";
      case FILE_TYPE_R -> "R";
      case FILE_TYPE_BINARY_B -> "B";
      case FILE_TYPE_BINARY_L -> "L";
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
  protected void writeVolumeBitMap (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    int blocksPerTrack = blockReader.getBlocksPerTrack ();
    int totalBlocks = blockReader.getTotalBlocks ();
    int totalTracks = totalBlocks / blocksPerTrack;

    int ptr = 0x38;
    for (int track = 0; track < totalTracks; track++)
    {
      int bits = 0;
      int mask = 0x80000000;
      for (int sector = blocksPerTrack - 1; sector >= 0; sector--)
      {
        if (volumeBitMap.get (track * blocksPerTrack + sector))
          bits |= mask;
        mask >>>= 1;
      }

      Utility.writeIntBigEndian (buffer, ptr, bits);
      ptr += 4;
    }
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
  protected void addDeletedFile (byte[] buffer, int ptr, String fileName)
  // ---------------------------------------------------------------------------------//
  {
    int sectorCount = Utility.unsignedShort (buffer, ptr + 33);
    int fileType = buffer[ptr + 2] & 0x7F;
    boolean isLocked = (buffer[ptr + 2] & 0x80) != 0;

    deletedFiles.add (String.format ("%s  %s  %03d  %s", isLocked ? "*" : " ",
        getFileTypeText (fileType), sectorCount, fileName));
  }

  // ---------------------------------------------------------------------------------//
  protected void addFailedFile (byte[] buffer, int ptr, String fileName)
  // ---------------------------------------------------------------------------------//
  {
    int sectorCount = Utility.unsignedShort (buffer, ptr + 33);
    int fileType = buffer[ptr + 2] & 0x7F;
    boolean isLocked = (buffer[ptr + 2] & 0x80) != 0;

    failedFiles.add (String.format ("%s  %s  %03d  %s", isLocked ? "*" : " ",
        getFileTypeText (fileType), sectorCount, fileName));
  }

  // ---------------------------------------------------------------------------------//
  void remove (FileDos file, boolean force)
  // ---------------------------------------------------------------------------------//
  {
    if (file.getParentFileSystem () != this)
      throw new InvalidParentFileSystemException ("file not part of this File System");

    if (file.isLocked () && !force)
      throw new FileLockedException (String.format ("%s is locked", file.getFileName ()));

    // mark file's sectors as free in the vtoc
    for (AppleBlock block : file.getDataBlocks ())      // index and data blocks
      volumeBitMap.set (block.getBlockNo ());
    freeBlocks = volumeBitMap.cardinality ();

    AppleBlock vtoc = getSector (17, 0);
    byte[] buffer = vtoc.getBuffer ();
    writeVolumeBitMap (buffer);
    vtoc.markDirty ();

    files.remove (file);
    deletedFiles.add (file.getFileName ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void cleanDisk ()
  // ---------------------------------------------------------------------------------//
  {
    super.cleanDisk ();

    direction = 1;
    lastTrackAllocated = 0x11;

    AppleBlock vtoc = getSector (17, 0);
    byte[] buffer = vtoc.getBuffer ();

    buffer[0x30] = (byte) lastTrackAllocated;
    buffer[0x31] = direction;

    vtoc.markDirty ();
  }

  // ---------------------------------------------------------------------------------//
  protected StringBuilder addCatalogLines (StringBuilder text, String underline)
  // ---------------------------------------------------------------------------------//
  {
    text.append (underline);

    for (AppleFile file : getFiles ())
    {
      text.append (file.getCatalogLine ());
      text.append ("\n");
    }

    int totalSectors = getTotalBlocks ();
    int freeSectors = getTotalFreeBlocks ();

    text.append (underline);
    text.append (String.format (
        "           Free sectors: %3d    " + "Used sectors: %3d    Total sectors: %3d",
        freeSectors, totalSectors - freeSectors, totalSectors));

    if (deletedFiles.size () > 0)
    {
      text.append ("\n\nDeleted files\n");
      text.append ("-------------\n");
      for (String name : deletedFiles)
        text.append (String.format ("%s%n", name));
    }

    if (failedFiles.size () > 0)
    {
      text.append ("\n\nFailed files\n");
      text.append ("------------\n");
      for (String name : failedFiles)
        text.append (String.format ("%s%n", name));
    }

    return text;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    String dosVersionText = switch (dosVersion)
    {
      case 1 -> "3.1";
      case 2 -> "3.2";
      case 3 -> "3.3";
      case 0x41 -> "4.1";
      default -> "??";
    };

    text.append (String.format ("----- DOS Header ------%n"));
    text.append (String.format ("Dos version ........... %02X                 %s%n",
        dosVersion, dosVersionText));
    text.append (
        String.format ("Volume number ......... %02X      %<,9d%n", volumeNumber));
    text.append (String.format ("Max TS pairs .......... %02X      %<,9d%n", maxTSpairs));
    text.append (
        String.format ("Last track allocated .. %02X      %<,9d%n", lastTrackAllocated));
    text.append (String.format ("Direction ............. %02X      %<,9d%n", direction));
    text.append (
        String.format ("Tracks per disk ....... %02X      %<,9d%n", tracksPerDisk));
    text.append (
        String.format ("Sectors per track ..... %02X      %<,9d%n", sectorsPerTrack));
    text.append (
        String.format ("Bytes per sector ...... %04X    %<,9d%n", bytesPerSector));

    return Utility.rtrim (text);
  }
}
