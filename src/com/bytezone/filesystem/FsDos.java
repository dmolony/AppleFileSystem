package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsDos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final int ENTRY_SIZE = 35;

  private int dosVersion;
  private int volumeNumber;
  private int maxTSpairs;
  private int lastTrackAllocated;

  private byte direction;
  private int tracksPerDisk;
  private int sectorsPerTrack;
  private int bytesPerSector;

  private BitSet volumeBitMap;

  private int deletedFiles;
  private int failedFiles;

  // ---------------------------------------------------------------------------------//
  FsDos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.DOS);

    assert totalCatalogBlocks == 0;

    AppleBlock vtoc = getSector (17, 0, BlockType.FS_DATA);
    if (vtoc == null)
      throw new FileFormatException ("Dos: Invalid VTOC");

    vtoc.setBlockSubType ("VTOC");

    byte[] buffer = vtoc.read ();
    buildFreeSectorList (buffer);

    if (buffer[3] < 0x01 || buffer[3] > 0x03)
      throw new FileFormatException ("Dos: byte 3 invalid");

    dosVersion = buffer[0x03] & 0xFF;
    volumeNumber = buffer[0x06] & 0xFF;
    maxTSpairs = buffer[0x27] & 0xFF;
    lastTrackAllocated = buffer[0x30] & 0xFF;

    direction = buffer[0x31];
    tracksPerDisk = buffer[0x34] & 0xFF;
    sectorsPerTrack = buffer[0x35] & 0xFF;
    bytesPerSector = Utility.signedShort (buffer, 0x36);

    List<AppleBlock> catalogSectors = new ArrayList<> ();   // to check for looping

    while (true)
    {
      int track = buffer[1] & 0xFF;
      int sector = buffer[2] & 0xFF;

      if (track == 0)
        break;

      AppleBlock catalogSector = getSector (track, sector, BlockType.FS_DATA);
      if (catalogSector == null)
        throw new FileFormatException ("Dos: Invalid catalog sector");

      //      catalogSector.setBlockType (BlockType.FS_DATA);
      catalogSector.setBlockSubType ("CATALOG");

      if (checkDuplicate (catalogSectors, catalogSector))
        throw new FileFormatException ("Dos: Duplicate catalog sector (looping)");

      catalogSectors.add (catalogSector);

      buffer = catalogSector.read ();
      int ptr = 11;

      while (ptr < buffer.length && buffer[ptr] != 0)
      {
        if ((buffer[ptr] & 0x80) != 0)        // deleted file
        {
          // could make a list for Extras' panel
          ++deletedFiles;
        }
        else
        {
          try
          {
            FileDos file = new FileDos (this, buffer, ptr);
            addFile (file);
          }
          catch (FileFormatException e)
          {
            ++failedFiles;
            // could prepare list of failures for Extras' panel
            //            String fileName = Utility.string (buffer, ptr + 3, 30).trim ();
            //            System.out.println (fileName + " failed");
          }
        }

        ptr += ENTRY_SIZE;
      }
    }

    setTotalCatalogBlocks (catalogSectors.size ());

    // flag DOS sectors
    int unused = 0;
    int free = 0;

    for (int blockNo = 0; blockNo < 48; blockNo++)
    {
      BlockType blockType = getBlock (blockNo).getBlockType ();

      if (blockType == BlockType.EMPTY || blockType == BlockType.ORPHAN)
        unused++;
      else
        System.out.printf ("%d %s%n", blockNo, blockType);

      if (volumeBitMap.get (blockNo))
        free++;
    }

    if (unused == 48 && free == 0)
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
  private boolean checkDuplicate (List<AppleBlock> catalogSectors, AppleBlock testSector)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleBlock catalogSector : catalogSectors)
      if (catalogSector.getBlockNo () == testSector.getBlockNo ())
        return true;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  private void buildFreeSectorList (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    int blocksPerTrack = blockReader.blocksPerTrack;
    int totalBlocks = blockReader.totalBlocks;
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
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Volume : %03d%n%n", volumeNumber));

    String underline = "- --- ---  ------------------------------  -----  -------------"
        + "  -- ----  -------------------\n";

    text.append ("L Typ Len  Name                            Addr"
        + "   Length         TS Data  Comment\n");
    text.append (underline);

    for (AppleFile file : getFiles ())
    {
      //      if (countEntries (fileEntry) > 1)
      //        entry += "** duplicate **";
      text.append (file.getCatalogLine ());
      text.append ("\n");
    }

    int totalSectors = getTotalBlocks ();
    int freeSectors = getFreeBlocks ();

    text.append (underline);
    text.append (String.format (
        "           Free sectors: %3d    " + "Used sectors: %3d    Total sectors: %3d",
        freeSectors, totalSectors - freeSectors, totalSectors));

    if (false)
      text.append (String.format (
          "%nActual:    Free sectors: %3d    "
              + "Used sectors: %3d    Total sectors: %3d",
          freeSectors, totalSectors - freeSectors, totalSectors));

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

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
