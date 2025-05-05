package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatMeta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class CatalogEntryDos4 extends CatalogEntryDos
// -----------------------------------------------------------------------------------//
{
  private static Locale US = Locale.US;                 // to force 3 character months
  private static final DateTimeFormatter sdf1 =
      DateTimeFormatter.ofPattern ("dd-LLL-yy HH:mm", US);
  private static final DateTimeFormatter sdf2 =
      DateTimeFormatter.ofPattern ("dd-LLL-yy HH:mm:ss", US);

  boolean tsListZero;
  LocalDateTime modified;
  boolean deleted;

  // ---------------------------------------------------------------------------------//
  public CatalogEntryDos4 (AppleBlock catalogBlock, int slot)
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

    deleted = (buffer[ptr] & 0x80) != 0;
    tsListZero = (buffer[ptr] & 0x40) != 0;

    fileName = Utility.string (buffer, ptr + 3, 24).trim ();
    checkName (buffer, ptr + 3, 24);                 // check for invalid characters

    modified = Utility.getDos4LocalDateTime (buffer, ptr + 27);
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

    Utility.writeString (String.format ("%-24s", fileName), buffer, ptr + 3);
    Utility.writeDos4LocalDateTime (buffer, ptr + 27, modified);
    Utility.writeShort (buffer, ptr + 33, sectorCount);

    catalogBlock.markDirty ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void delete ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = HEADER_SIZE + slot * ENTRY_SIZE;

    AppleBlock catalogSector = getCatalogBlock ();
    byte[] buffer = catalogSector.getBuffer ();

    // update the access byte?
    // update the modified date?

    firstTrack &= 0x80;
    buffer[ptr] = (byte) firstTrack;

    catalogSector.markDirty ();

    System.out.println ("Write me");
  }

  // ---------------------------------------------------------------------------------//
  public String getModified1 ()
  // ---------------------------------------------------------------------------------//
  {
    return modified == null ? "x" : modified.format (sdf1);
  }

  // ---------------------------------------------------------------------------------//
  public String getModified2 ()
  // ---------------------------------------------------------------------------------//
  {
    return modified == null ? "x" : modified.format (sdf2);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    formatMeta (text, "\nZero flag", tsListZero);
    formatMeta (text, "Modified", getModified2 ());

    return text.toString ();
  }
}
