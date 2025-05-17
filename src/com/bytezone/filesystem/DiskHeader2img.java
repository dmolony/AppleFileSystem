package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatMeta;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class DiskHeader2img extends DiskHeader
// -----------------------------------------------------------------------------------//
{
  static final byte[] TWO_IMG_MAGIC = { 0x32, 0x49, 0x4D, 0x47 };
  private static String[] twoIMGFormats = { "Dos", "Prodos", "NIB" };
  private static String[] creatorCodes =
      { "!nfc", "APSX", "B2TR", "CTKG", "CdrP", "CPII", "pdos", "SHEP", "ShIm", "WOOF",
          "XGS!", "RVLW", "|BD<", "Vi][", "PRFS", "FISH", "RVLW" };
  private static String[] creatorNames =
      { "ASIMOV2", "?", "Bernie ][ the Rescue", "Catakig", "CiderPress", "CiderPress II",
          "?", "Sheppy's ImageMaker", "Sheppy's ImageMaker", "Sweet 16", "XGS", "?",
          "Cadius", "Virtual ][", "ProFUSE", "FishWings", "Revival for Windows" };

  private String creator;
  final int offset;
  final int length;
  final int originalLength;
  final int headerSize;
  private int version;
  private int format;
  private int prodosBlocks;
  private int flags;
  private int commentOffset;
  private int commentLength;
  private int creatorDataOffset;
  private int creatorDataLength;
  private String comment;

  private boolean locked;
  private boolean hasDosVolumeNumber;
  private int volumeNumber;

  // ---------------------------------------------------------------------------------//
  public DiskHeader2img (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, DiskHeaderType.TWO_IMG);

    byte[] buffer = blockReader.getDiskBuffer ().data ();
    int diskOffset = blockReader.getDiskBuffer ().offset ();

    assert blockReader.isMagic (0, TWO_IMG_MAGIC);

    creator = new String (buffer, diskOffset + 4, 4);
    headerSize = Utility.unsignedShort (buffer, diskOffset + 8);
    version = Utility.unsignedShort (buffer, diskOffset + 10);
    format = Utility.unsignedInt (buffer, diskOffset + 12);
    flags = Utility.unsignedInt (buffer, diskOffset + 16);
    prodosBlocks = Utility.unsignedInt (buffer, diskOffset + 20);

    offset = Utility.unsignedInt (buffer, diskOffset + 24);
    originalLength = Utility.unsignedInt (buffer, diskOffset + 28);

    length = originalLength == 0 ? prodosBlocks * 512 : originalLength; // see Fantavision.2mg

    commentOffset = Utility.unsignedInt (buffer, diskOffset + 32);
    commentLength = Utility.unsignedInt (buffer, diskOffset + 36);
    creatorDataOffset = Utility.unsignedInt (buffer, diskOffset + 40);
    creatorDataLength = Utility.unsignedInt (buffer, diskOffset + 44);
    comment = commentOffset == 0 ? ""
        : new String (buffer, diskOffset + commentOffset, commentLength);

    locked = (flags & 0x8000) != 0;
    hasDosVolumeNumber = (flags & 0x0100) != 0;
    volumeNumber = flags & 0x00FF;
  }

  // ---------------------------------------------------------------------------------//
  public static boolean isValid (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.isMagic (0, TWO_IMG_MAGIC);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public BlockReader getBlockReader ()
  // ---------------------------------------------------------------------------------//
  {
    Buffer diskBuffer = blockReader.getDiskBuffer ();

    return new BlockReader (blockReader.getName (), diskBuffer.data (),
        diskBuffer.offset () + offset, length);
  }

  // ---------------------------------------------------------------------------------//
  private String getCreator (String code)
  // ---------------------------------------------------------------------------------//
  {
    for (int i = 0; i < creatorCodes.length; i++)
      if (creatorCodes[i].equals (code))
        return creatorNames[i];

    return "";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    String message = originalLength == 0 ? "   <-- wrong!" : "";

    formatMeta (text, "Creator", creator, getCreator (creator));
    formatMeta (text, "Header size", 2, headerSize);
    formatMeta (text, "Version", 2, version);
    formatMeta (text, "Format", 2, format, twoIMGFormats[format]);
    formatMeta (text, "Flags", 8, flags);
    formatMeta (text, "  locked", locked);
    formatMeta (text, "  has Dos Volume no", hasDosVolumeNumber);
    formatMeta (text, "  Dos Volume no", 4, volumeNumber);
    formatMeta (text, "Blocks", 6, prodosBlocks);
    formatMeta (text, "Data offset", 2, offset);
    formatMeta (text, "Data size", 8, originalLength, message);
    formatMeta (text, "Comment offset", 6, commentOffset);
    formatMeta (text, "Comment length", 6, commentLength);
    formatMeta (text, "Comment", comment);
    formatMeta (text, "Creator Data offset", 6, creatorDataOffset);
    formatMeta (text, "Creator Data length", 6, creatorDataLength);

    return text.toString ();
  }
}
