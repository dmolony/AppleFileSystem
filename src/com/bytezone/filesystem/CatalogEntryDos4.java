package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class CatalogEntryDos4 extends CatalogEntryDos
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter sdf2 =
      DateTimeFormatter.ofPattern ("dd-LLL-yy HH:mm:ss", Locale.US);

  boolean tsListZero;
  LocalDateTime modified;
  boolean deleted;

  // ---------------------------------------------------------------------------------//
  public CatalogEntryDos4 (byte[] buffer, int ptr, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (buffer, slot);

    readCatalogEntry ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void readCatalogEntry ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = HEADER_SIZE + slot * ENTRY_SIZE;

    //    int nextTrack = buffer[ptr] & 0xFF;
    //    int nextSector = buffer[ptr + 1] & 0xFF;

    deleted = (buffer[ptr] & 0x80) != 0;
    tsListZero = (buffer[ptr] & 0x40) != 0;

    isLocked = (buffer[ptr + 2] & 0x80) != 0;
    fileType = buffer[ptr + 2] & 0x7F;

    fileName = Utility.string (buffer, ptr + 3, 24).trim ();
    isNameValid = checkName (fileName);                 // check for invalid characters
    modified = Utility.getDos4LocalDateTime (buffer, ptr + 27);
    sectorCount = Utility.unsignedShort (buffer, ptr + 33);
    //    int sectorsLeft = sectorCount;

  }

  // ---------------------------------------------------------------------------------//
  @Override
  void writeCatalogEntry ()
  // ---------------------------------------------------------------------------------//
  {

  }

  // ---------------------------------------------------------------------------------//
  @Override
  protected boolean checkName (String name)
  // ---------------------------------------------------------------------------------//
  {
    for (byte b : name.getBytes ())
      if (b == (byte) 0x88)
        return false;

    return true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("%nZero flag ............. %s%n", tsListZero));
    text.append (String.format ("Modified .............. %s%n", modified.format (sdf2)));

    return text.toString ();
  }
}
