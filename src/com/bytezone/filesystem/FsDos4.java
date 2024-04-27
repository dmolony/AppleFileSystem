package com.bytezone.filesystem;

import java.time.LocalDateTime;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class FsDos4 extends FsDos
// -----------------------------------------------------------------------------------//
{
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
    byte[] buffer = vtoc.read ();

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

      buffer = catalogSector.read ();

      int ptr = 11;

      while (ptr < buffer.length && buffer[ptr] != 0)
      {
        if ((buffer[ptr] & 0x80) != 0)        // deleted file
        {
          String fileName = Utility.string (buffer, ptr + 3, 24).trim ();
          System.out.printf ("deleted : %s%n", fileName);
        }
        else
        {
          try
          {
            FileDos4 file = new FileDos4 (this, buffer, ptr);
            addFile (file);
          }
          catch (FileFormatException e)
          {
            System.out.println (e);
            break;
          }
        }

        ptr += ENTRY_SIZE;
      }
      ++catalogBlocks;
    }

    setTotalCatalogBlocks (catalogBlocks);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    for (AppleFile file : getFiles ())
      text.append (
          String.format ("%-15s %s%n", file.getFileName (), file.getFileSystemType ()));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("----- DOS4 Header -----\n");
    text.append (String.format ("VTOC structure block .. %02X%n", vtocStructureBlock));
    text.append (String.format ("Dos version ........... %02X%n", dosVersion));
    text.append (String.format ("Build number .......... %02X%n", buildNumber));
    text.append (String.format ("RAM DOS ............... %s%n", ramDos));
    text.append (String.format ("Volume number ......... %02X%n", volumeNumber));
    text.append (String.format ("Volume type ........... %s%n", volumeType));
    text.append (String.format ("Volume name ........... %s%n", volumeName));
    text.append (String.format ("Initialised ........... %s%n", initTime));
    text.append (String.format ("Max TS ................ %02X%n", maxTSpairs));
    text.append (String.format ("Volume library ........ %04X%n", volumeLibrary));
    text.append (String.format ("Modified .............. %s%n", vtocTime));
    text.append (String.format ("Last allocated ........ %02X%n", lastTrackAllocated));
    text.append (String.format ("Direction ............. %02X%n", direction));
    text.append (String.format ("Total tracks .......... %02X%n", tracksPerDisk));
    text.append (String.format ("Total sectors ......... %02X%n", sectorsPerTrack));
    text.append (String.format ("Sector size ........... %02X%n", bytesPerSector));

    return Utility.rtrim (text);
  }
}
