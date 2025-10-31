package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

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
  boolean isDeleted;

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

    if (buffer[ptr] == (byte) 0xFF)
      isDeleted = true;
    else
    {
      firstTrack = buffer[ptr] & 0xFF;
      firstSector = buffer[ptr + 1] & 0xFF;

      isLocked = (buffer[ptr + 2] & 0x80) != 0;
      fileType = buffer[ptr + 2] & 0x7F;
      sectorCount = Utility.unsignedShort (buffer, ptr + 33);
    }
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
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    formatText (text, "Deleted?", isDeleted);

    if (!isDeleted)
    {
      formatText (text, "First track", 2, firstTrack);
      formatText (text, "First sector", 2, firstSector);
      formatText (text, "File type", 2, fileType);
      formatText (text, "Locked", isLocked);
      formatText (text, "Sector count", 2, sectorCount);
    }

    return text.toString ();
  }
}
