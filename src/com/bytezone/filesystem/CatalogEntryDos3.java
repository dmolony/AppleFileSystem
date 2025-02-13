package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class CatalogEntryDos3 extends CatalogEntryDos
// -----------------------------------------------------------------------------------//
{
  int firstTrack;
  int firstSector;

  // ---------------------------------------------------------------------------------//
  public CatalogEntryDos3 (AppleBlock catalogBlock, int ptr, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (catalogBlock, slot);
    //    this.slot = slot;
    //    this.catalogEntryBlock = catalogEntryBlock;

    //    catalogBuffer = catalogEntryBlock.getBuffer ();

    read ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void read ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = HEADER_SIZE + slot * ENTRY_SIZE;

    firstTrack = buffer[ptr] & 0xFF;
    firstSector = buffer[ptr + 1] & 0xFF;

    fileType = buffer[ptr + 2] & 0x7F;
    isLocked = (buffer[ptr + 2] & 0x80) != 0;
    fileName = Utility.string (buffer, ptr + 3, 30).trim ();

    isNameValid = checkName (fileName);                   // check for invalid characters
    sectorCount = Utility.unsignedShort (buffer, ptr + 33);
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

    //    catalogEntryBlock.markDirty ();
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
