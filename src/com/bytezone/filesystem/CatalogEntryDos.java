package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public abstract class CatalogEntryDos extends CatalogEntry
// -----------------------------------------------------------------------------------//
{
  static final int ENTRY_SIZE = 0x23;
  static final int HEADER_SIZE = 0x0B;

  int firstTrack;
  int firstSector;

  int fileType;
  boolean isLocked;
  int sectorCount;

  String fileName;
  boolean isNameValid;

  // ---------------------------------------------------------------------------------//
  public CatalogEntryDos (AppleBlock catalogBlock, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (catalogBlock, slot);

    read ();
  }

  // ---------------------------------------------------------------------------------//
  protected void readCommon ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = HEADER_SIZE + slot * ENTRY_SIZE;

    firstTrack = buffer[ptr] & 0xFF;
    firstSector = buffer[ptr + 1] & 0xFF;

    isLocked = (buffer[ptr + 2] & 0x80) != 0;
    fileType = buffer[ptr + 2] & 0x7F;
    sectorCount = Utility.unsignedShort (buffer, ptr + 33);
  }

  // ---------------------------------------------------------------------------------//
  protected void writeCommon ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = HEADER_SIZE + slot * ENTRY_SIZE;

    buffer[ptr] = (byte) firstTrack;
    buffer[ptr + 1] = (byte) firstSector;

    buffer[ptr + 2] = (byte) fileType;
    if (isLocked)
      buffer[ptr + 2] |= 0x80;

    Utility.writeShort (buffer, ptr + 33, sectorCount);
  }

  // ---------------------------------------------------------------------------------//
  protected void checkName ()
  // ---------------------------------------------------------------------------------//
  {
    isNameValid = false;

    for (byte b : fileName.getBytes ())
      if ((b & 0x80) != 0)
        return;

    isNameValid = true;
  }
}
