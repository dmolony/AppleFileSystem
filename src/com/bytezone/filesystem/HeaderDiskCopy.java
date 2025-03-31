package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// https://www.discferret.com/wiki/Apple_DiskCopy_4.2
// Apple II File Type Notes $E0/0005 (macintosh)
// -----------------------------------------------------------------------------------//
public class HeaderDiskCopy
{
  private String name;
  private int dataSize;
  private int tagSize;
  private int dataChecksum;
  private int tagChecksum;
  private int diskFormat;
  private int format;
  private int id;             // should be 0x0100

  // ---------------------------------------------------------------------------------//
  public HeaderDiskCopy (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = blockReader.getDiskBuffer ().data ();
    int diskOffset = blockReader.getDiskBuffer ().offset ();

    //    assert blockReader.isMagic (0, TWO_IMG);

    int nameLength = buffer[0] * 0xFF;
    if (nameLength < 1 || nameLength > 0x3F)
      name = Utility.getPascalString (buffer, 0);
    dataSize = Utility.unsignedIntBigEndian (buffer, 0x40);
    tagSize = Utility.unsignedIntBigEndian (buffer, 0x44);
    dataChecksum = Utility.unsignedIntBigEndian (buffer, 0x48);
    tagChecksum = Utility.unsignedIntBigEndian (buffer, 0x4C);
    diskFormat = buffer[0x50] & 0xFF;
    format = buffer[0x51] & 0xFF;
    id = Utility.unsignedShortBigEndian (buffer, 0x52);
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
    text.append (String.format ("Data size ............. %08X (%<,d)%n", dataSize));
    text.append (String.format ("Tag size .............. %08X (%<,d)%n", tagSize));
    text.append (String.format ("Data checksum ......... %08X (%<,d)%n", dataChecksum));
    text.append (String.format ("Tag checksum .......... %08X (%<,d)%n", tagChecksum));
    text.append (String.format ("Disk format ........... %02X%n", diskFormat));
    text.append (String.format ("Format byte ........... %02X%n", format));
    text.append (String.format ("ID .................... %04X%n", id));

    return Utility.rtrim (text);
  }
}
