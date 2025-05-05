package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatMeta;

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
    checkName (buffer, ptr + 3, 30);                   // check for invalid characters
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void write ()
  // ---------------------------------------------------------------------------------//
  {
    super.writeCommon ();

    int ptr = HEADER_SIZE + slot * ENTRY_SIZE;

    Utility.writeString (String.format ("%-30s", fileName), buffer, ptr + 3);

    catalogBlock.markDirty ();
  }

  // mark file as deleted in the catalog
  // ---------------------------------------------------------------------------------//
  @Override
  void delete ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = HEADER_SIZE + slot * ENTRY_SIZE;

    AppleBlock catalogSector = getCatalogBlock ();
    byte[] buffer = catalogSector.getBuffer ();

    buffer[ptr + 0x20] = buffer[ptr];     // move first track number to end of filename
    buffer[ptr] = (byte) 0xFF;            // deleted file

    catalogSector.markDirty ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    formatMeta (text, "\nFirst track", 2, firstTrack);
    formatMeta (text, "First sector", 2, firstSector);

    return text.toString ();
  }
}
