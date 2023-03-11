package com.bytezone.filesystem;

import java.util.BitSet;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsDos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final int ENTRY_SIZE = 35;
  int dosVersion;
  private BitSet volumeBitMap;

  // ---------------------------------------------------------------------------------//
  public FsDos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.DOS);

    int catalogBlocks = 0;

    AppleBlock vtoc = getSector (17, 0);
    if (!vtoc.isValid ())
      throw new FileFormatException ("Dos: Invalid VTOC");

    byte[] buffer = vtoc.read ();
    buildFreeSectorList (buffer);

    if (buffer[3] < 0x01 || buffer[3] > 0x03)
      throw new FileFormatException ("Dos: byte 3 invalid");

    dosVersion = buffer[3] & 0xFF;

    while (true)
    {
      int track = buffer[1] & 0xFF;
      int sector = buffer[2] & 0xFF;

      if (track == 0)
        break;

      AppleBlock catalogSector = getSector (track, sector);
      if (!catalogSector.isValid ())
        throw new FileFormatException ("Dos: Invalid catalog sector");

      buffer = catalogSector.read ();

      int ptr = 11;

      while (ptr < buffer.length && buffer[ptr] != 0)
      {
        if ((buffer[ptr] & 0x80) != 0)        // deleted file
        {
          // could make a list for Extras' panel
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
            // could prepare list of failures for Extras' panel
            //            String fileName = Utility.string (buffer, ptr + 3, 30).trim ();
            //            System.out.println (fileName + " failed");
          }
        }

        ptr += ENTRY_SIZE;
      }

      ++catalogBlocks;
    }

    setCatalogBlocks (catalogBlocks);
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
      int bits = Utility.unsignedLongBigEndian (buffer, ptr);
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
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString () + "\n\n");

    text.append (String.format ("Dos version ........... %02X", dosVersion));

    return text.toString ();
  }
}
