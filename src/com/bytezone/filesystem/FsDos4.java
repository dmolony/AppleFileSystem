package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class FsDos4 extends FsDos
// -----------------------------------------------------------------------------------//
{
  private static final String underline =
      "- --- ---  ------------------------  ---------------  -----"
          + "  -------------  -- ---  -------------------\n";

  private int vtocStructureBlock;
  private int buildNumber;
  private char ramDos;
  private char volumeType;
  private String volumeName;
  private LocalDateTime initTime;
  private int volumeLibrary;
  private LocalDateTime vtocTime;

  List<AppleBlock> catalogSectors = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  FsDos4 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.DOS4);

    AppleBlock vtoc = getSector (17, 0, BlockType.FS_DATA);
    if (vtoc == null)
      throw new FileFormatException ("Dos: Invalid VTOC");

    vtoc.setBlockSubType ("VTOC");

    byte[] buffer = vtoc.getBuffer ();

    int track = buffer[1] & 0xFF;
    int sector = buffer[2] & 0xFF;

    dosVersion = buffer[3] & 0xFF;
    if (dosVersion < 0x41 || dosVersion > 0x45)
      throw new FileFormatException (
          String.format ("Dos: version byte invalid: %02X", dosVersion));

    vtocStructureBlock = buffer[0] & 0xFF;
    buildNumber = buffer[4] & 0xFF;
    ramDos = (char) (buffer[5] & 0x7F);
    volumeNumber = buffer[6] & 0xFF;
    volumeType = (char) (buffer[7] & 0x7F);
    volumeName = Utility.string (buffer, 8, 24);

    initTime = Utility.getDos4LocalDateTime (buffer, 32);
    maxTSpairs = buffer[39] & 0xFF;
    volumeLibrary = Utility.unsignedShort (buffer, 40);
    vtocTime = Utility.getDos4LocalDateTime (buffer, 42);

    lastTrackAllocated = buffer[48] & 0xFF;
    direction = buffer[49];
    tracksPerDisk = buffer[52] & 0xFF;
    sectorsPerTrack = buffer[53] & 0xFF;
    bytesPerSector = Utility.unsignedShort (buffer, 54);

    createVolumeBitMap (buffer);

    while (track > 0)           // track needs zero flag for loop to work
    {
      track &= 0x3F;            // remove deleted (0x80) and track zero (0x40) flags
      sector &= 0x1F;

      AppleBlock catalogSector = getSector (track, sector, BlockType.FS_DATA);

      if (catalogSector == null)
        throw new FileFormatException ("Dos: Invalid catalog sector");
      if (checkDuplicate (catalogSectors, catalogSector))
        throw new FileFormatException ("Dos: Duplicate catalog sector (looping)");

      catalogSector.setBlockSubType ("CATALOG");
      catalogSectors.add (catalogSector);

      readCatalogBlock (catalogSector);

      buffer = catalogSector.getBuffer ();
      track = buffer[1] & 0xFF;
      sector = buffer[2] & 0xFF;
    }

    setTotalCatalogBlocks (catalogSectors.size ());

    if (volumeType == 'B')
      flagDosSectors ();
  }

  // ---------------------------------------------------------------------------------//
  private void readCatalogBlock (AppleBlock catalogSector)
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = catalogSector.getBuffer ();

    int ptr = HEADER_SIZE;
    int slot = 0;

    while (ptr < buffer.length && buffer[ptr] != 0)
    {
      if ((buffer[ptr] & 0x80) != 0)         // deleted file
      {
        String fileName = Utility.string (buffer, ptr + 3, 24).trim ();
        addDeletedFile (buffer, ptr, fileName);
      }
      else
        try
        {
          FileDos4 file = new FileDos4 (this, catalogSector, slot);
          addFile (file);
        }
        catch (FileFormatException e)
        {
          String fileName = Utility.string (buffer, ptr + 3, 24).trim ();
          addFailedFile (buffer, ptr, fileName);
        }

      ptr += ENTRY_SIZE;
      slot++;
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Volume : %03d  %s%n%n", volumeNumber, volumeName));
    text.append ("L Typ Len  Name                      Modified         Addr"
        + "   Length         TS DAT  Comment\n");

    super.addCatalogLines (text, underline);

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  private String getVolumeTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return switch (volumeType)
    {
      case 'D' -> "Data disk";
      case 'B' -> "Bootable disk";
      default -> "Unknown : " + volumeType;
    };
  }

  // ---------------------------------------------------------------------------------//
  private String getRamTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return switch (ramDos)
    {
      case 'H' -> "High";
      case 'L' -> "Low";
      default -> "Unknown : " + ramDos;
    };
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("\n\n----- DOS4 Header -----\n");
    text.append (String.format ("VTOC structure block .. %02X%n", vtocStructureBlock));
    text.append (String.format ("Build number .......... %02X%n", buildNumber));
    text.append (
        String.format ("RAM DOS ............... %s  %s%n", ramDos, getRamTypeText ()));
    text.append (String.format ("Volume type ........... %s  %s%n", volumeType,
        getVolumeTypeText ()));
    text.append (String.format ("Volume name ........... %s%n", volumeName));
    text.append (String.format ("Volume library ........ %04X%n", volumeLibrary));
    text.append (String.format ("Initialised ........... %s%n", initTime.format (sdf)));
    text.append (String.format ("Modified .............. %s%n", vtocTime.format (sdf)));

    return Utility.rtrim (text);
  }
}
