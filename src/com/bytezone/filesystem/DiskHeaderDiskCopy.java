package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// https://www.discferret.com/wiki/Apple_DiskCopy_4.2
// Apple II File Type Notes $E0/0005 (macintosh)
// -----------------------------------------------------------------------------------//
public class DiskHeaderDiskCopy extends DiskHeader
// -----------------------------------------------------------------------------------//
{
  private static String[] formatTypes =
      { "GCR CLV 400K", "GCR CLV 800K", "MFM CAV 400K", "MFM CAV 800K" };
  private static byte[] diskCopySize400 = { 0x00, 0x06, 0x40, 0x00 };
  private static byte[] diskCopySize800 = { 0x00, 0x0C, (byte) 0x80, 0x00 };

  private final String name;
  private final int dataSize;
  private final int tagSize;
  private final int dataChecksum;
  private final int tagChecksum;
  private final int diskFormat;
  private final int encoding;
  private final int id;             // must be 0x0100

  // ---------------------------------------------------------------------------------//
  public DiskHeaderDiskCopy (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, DiskHeaderType.DISK_COPY);

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
    encoding = buffer[ptr + 0x51] & 0xFF;
    id = Utility.unsignedShortBigEndian (buffer, ptr + 0x52);
  }

  // ---------------------------------------------------------------------------------//
  public static boolean isValid (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = blockReader.getDiskBuffer ().data ();
    int ptr = blockReader.getDiskBuffer ().offset ();
    int id = Utility.unsignedShortBigEndian (buffer, ptr + 0x52);

    return (blockReader.isMagic (0x40, diskCopySize800)
        || blockReader.isMagic (0x40, diskCopySize400)) && id == 0x100;
  }

  // dataSize should be one of: 00 06 40 00 / 00 0C 80 00 / 00 0B 40 00 / 00 16 80 00
  // tagSize should be one of:  00 00 00 00 / 00 00 25 80 / 00 00 4B 00

  // ---------------------------------------------------------------------------------//
  int getId ()
  // ---------------------------------------------------------------------------------//
  {
    return id;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public BlockReader getBlockReader ()
  // ---------------------------------------------------------------------------------//
  {
    Buffer diskBuffer = blockReader.getDiskBuffer ();

    return new BlockReader (blockReader.getName (), diskBuffer.data (),
        diskBuffer.offset () + 0x54, dataSize);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    String encodingText = switch (encoding)
    {
      case 0x02 -> "Mac 400K";
      case 0x12 -> "Lisa 400K";
      case 0x22 -> "Mac 800K";
      case 0x24 -> "Prodos 800K";
      case 0x96 -> "Invalid";
      default -> "???";
    };

    text.append (String.format ("Name .................. %s%n", name));
    text.append (String.format ("Data size ............. %08X %<,9d%n", dataSize));
    text.append (String.format ("Tag size .............. %08X %<,9d%n", tagSize));
    text.append (String.format ("Data checksum ......... %08X %n", dataChecksum));
    text.append (String.format ("Tag checksum .......... %08X %n", tagChecksum));
    text.append (String.format ("Disk format ........... %02X        %s%n", diskFormat,
        formatTypes[diskFormat]));
    text.append (String.format ("Encoding byte ......... %02X        %s%n", encoding,
        encodingText));
    text.append (String.format ("ID .................... %04X%n", id));

    return Utility.rtrim (text);
  }
}
