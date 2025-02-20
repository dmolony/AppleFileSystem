package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class CatalogEntryDos3 extends CatalogEntryDos
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  public CatalogEntryDos3 (AppleBlock catalogBlock, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (catalogBlock, slot);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void read ()
  // ---------------------------------------------------------------------------------//
  {
    super.readCommon ();

    int ptr = HEADER_SIZE + slot * ENTRY_SIZE;

    fileName = Utility.string (buffer, ptr + 3, 30).trim ();
    checkName ();                   // check for invalid characters
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void write ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = HEADER_SIZE + slot * ENTRY_SIZE;

    buffer[ptr] = (byte) firstTrack;
    buffer[ptr + 1] = (byte) firstSector;
    buffer[ptr + 2] = (byte) (fileType | (isLocked ? 0x80 : 0x00));

    Utility.writeString (String.format ("%-30s", fileName), buffer, ptr + 3);
    Utility.writeShort (buffer, ptr + 33, sectorCount);

    catalogBlock.markDirty ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void delete ()
  // ---------------------------------------------------------------------------------//
  {

  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("\n");
    text.append (String.format ("First track/sector ..... %02X / %02X%n", firstTrack,
        firstSector));

    return text.toString ();
  }
}
