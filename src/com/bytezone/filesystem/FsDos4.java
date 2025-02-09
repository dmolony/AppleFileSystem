package com.bytezone.filesystem;

import java.time.LocalDateTime;

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

  // ---------------------------------------------------------------------------------//
  FsDos4 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.DOS4);

    int catalogBlocks = 0;

    AppleBlock vtoc = getSector (17, 0, BlockType.FS_DATA);
    vtoc.setBlockSubType ("VTOC");
    byte[] buffer = vtoc.getBuffer ();

    createVolumeBitMap (buffer);

    int version = buffer[3] & 0xFF;
    if (version < 65 || version > 69)         // 0x41 .. 0x45
      return;

    vtocStructureBlock = buffer[0] & 0xFF;
    dosVersion = buffer[3] & 0xFF;
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

    while (true)
    {
      int track = buffer[1] & 0xFF;
      int sector = buffer[2] & 0xFF;

      if (track == 0)
        break;

      track &= 0x3F;
      sector &= 0x1F;

      AppleBlock catalogSector = getSector (track, sector, BlockType.FS_DATA);
      if (catalogSector == null)
        return;
      catalogSector.setBlockSubType ("CATALOG");

      buffer = catalogSector.getBuffer ();

      int ptr = 11;
      int slot = 0;

      while (ptr < buffer.length && buffer[ptr] != 0)
      {
        if ((buffer[ptr] & 0x80) != 0)        // deleted file
        {
          String fileName = Utility.string (buffer, ptr + 3, 24).trim ();
          deletedFiles.add (fileName);
        }
        else
        {
          try
          {
            FileDos4 file = new FileDos4 (this, buffer, ptr, slot);
            addFile (file);
          }
          catch (FileFormatException e)
          {
            System.out.println (e);
            String fileName = Utility.string (buffer, ptr + 3, 24).trim ();
            failedFiles.add (fileName);
            break;
          }
        }

        ptr += ENTRY_SIZE;
      }
      ++catalogBlocks;
    }

    setTotalCatalogBlocks (catalogBlocks);
    if (volumeType == 'B')
      flagDosSectors ();
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
