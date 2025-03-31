package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// https://www.discferret.com/wiki/Apple_DiskCopy_4.2
// Apple II File Type Notes $E0/0005 (macintosh)
// -----------------------------------------------------------------------------------//
public class HeaderDiskCopy
{
  private final String name;
  private final int dataSize;
  private final int tagSize;
  private final int dataChecksum;
  private final int tagChecksum;
  private final int diskFormat;
  private final int format;
  private final int id;             // should be 0x0100

  // ---------------------------------------------------------------------------------//
  public HeaderDiskCopy (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = blockReader.getDiskBuffer ().data ();
    int ptr = blockReader.getDiskBuffer ().offset ();

    int nameLength = buffer[ptr] * 0xFF;
    if (nameLength < 1 || nameLength > 0x3F)
      name = Utility.getPascalString (buffer, ptr);
    else
      name = "*** INVALID ***";

    dataSize = Utility.unsignedIntBigEndian (buffer, ptr + 0x40);
    tagSize = Utility.unsignedIntBigEndian (buffer, ptr + 0x44);
    dataChecksum = Utility.unsignedIntBigEndian (buffer, ptr + 0x48);
    tagChecksum = Utility.unsignedIntBigEndian (buffer, ptr + 0x4C);
    diskFormat = buffer[ptr + 0x50] & 0xFF;
    format = buffer[ptr + 0x51] & 0xFF;
    id = Utility.unsignedShortBigEndian (buffer, ptr + 0x52);
  }

  // ---------------------------------------------------------------------------------//
  int getBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return dataSize / 512;
  }

  // ---------------------------------------------------------------------------------//
  int getId ()
  // ---------------------------------------------------------------------------------//
  {
    return id;
  }

  // ---------------------------------------------------------------------------------//
  int getDataSize ()
  // ---------------------------------------------------------------------------------//
  {
    return dataSize;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Name .................. %s%n", name));
    text.append (String.format ("Data size ............. %08X %<,9d%n", dataSize));
    text.append (String.format ("Tag size .............. %08X %<,9d%n", tagSize));
    text.append (String.format ("Data checksum ......... %08X %n", dataChecksum));
    text.append (String.format ("Tag checksum .......... %08X %n", tagChecksum));
    text.append (String.format ("Disk format ........... %02X%n", diskFormat));
    text.append (String.format ("Format byte ........... %02X%n", format));
    text.append (String.format ("ID .................... %04X%n", id));

    return Utility.rtrim (text);
  }
}
