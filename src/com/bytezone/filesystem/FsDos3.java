package com.bytezone.filesystem;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsDos3 extends FsDos
// -----------------------------------------------------------------------------------//
{
  private static final String underline =
      "- --- ---  ------------------------------  -----  --------------"
          + "  -- ---  ------------------------\n";
  private boolean debug = false;

  // ---------------------------------------------------------------------------------//
  FsDos3 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.DOS3);

    AppleBlock vtoc = getSector (17, 0, BlockType.FS_DATA);
    if (vtoc == null)
      throw new FileFormatException ("Dos: Invalid VTOC");

    vtoc.setBlockSubType ("VTOC");

    byte[] buffer = vtoc.getBuffer ();

    int track = buffer[1] & 0xFF;
    int sector = buffer[2] & 0xFF;

    dosVersion = buffer[0x03] & 0xFF;
    if (dosVersion < 0x01 || dosVersion > 0x04)     // some disks get this wrong
      throw new FileFormatException (
          String.format ("Dos3: version byte invalid: %02X", dosVersion));

    volumeNumber = buffer[0x06] & 0xFF;
    maxTSpairs = buffer[0x27] & 0xFF;
    lastTrackAllocated = buffer[0x30] & 0xFF;

    direction = buffer[0x31];
    tracksPerDisk = buffer[0x34] & 0xFF;
    sectorsPerTrack = buffer[0x35] & 0xFF;
    bytesPerSector = Utility.signedShort (buffer, 0x36);

    createVolumeBitMap (buffer);

    while (validCatalogSector (track, sector))
    {
      AppleBlock catalogSector = getSector (track, sector, BlockType.FS_DATA);
      if (debug)
      {
        System.out.printf ("track %d  sector %d%n", track, sector);
        System.out.println (Utility.format (catalogSector.getBuffer ()));
      }

      if (catalogSector == null)
        throw new FileFormatException ("Dos: Invalid catalog sector");

      if (checkDuplicate (catalogSectors, catalogSector))
        throw new FileFormatException ("Dos: Duplicate catalog sector (looping)");

      catalogSector.setBlockSubType ("CATALOG");
      catalogSectors.add (catalogSector);

      buffer = catalogSector.getBuffer ();
      track = buffer[1] & 0xFF;
      sector = buffer[2] & 0xFF;
    }

    setTotalCatalogBlocks (catalogSectors.size ());

    if (debug)
      System.out.printf ("found %d catalog sectors%n", catalogSectors.size ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  protected void readCatalogBlock (AppleBlock catalogSector)
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = catalogSector.getBuffer ();

    int ptr = HEADER_SIZE;
    int slot = 0;

    while (ptr < buffer.length && buffer[ptr] != 0)
    {
      if (buffer[ptr] == (byte) 0xFF)        // deleted file
      {
        String fileName = Utility.string (buffer, ptr + 3, 29).trim ();
        addDeletedFile (buffer, ptr, fileName);
        CatalogEntry ce = new CatalogEntryDos3 (catalogSector, slot);
        catalogEntries.add (ce);
      }
      else
        try
        {
          FileDos3 file = new FileDos3 (this, catalogSector, ptr);
          addFile (file);
          catalogEntries.add (file.catalogEntry);
        }
        catch (FileFormatException e)
        {
          String fileName = Utility.string (buffer, ptr + 3, 30).trim ();
          addFailedFile (buffer, ptr, fileName);
        }

      ++slot;
      ptr += ENTRY_SIZE;
    }

    flagDosSectors ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Volume : %03d\n\n", volumeNumber));
    text.append ("L Typ Len  Name                            Addr"
        + "   Length          TS DAT  Comment\n");

    super.addCatalogLines (text, underline);

    return Utility.rtrim (text);
  }
}
