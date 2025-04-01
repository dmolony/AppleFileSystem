package com.bytezone.filesystem;

import com.bytezone.filesystem.AppleFileSystem.FileSystemType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class DiskHeader2img extends DiskHeader
// -----------------------------------------------------------------------------------//
{
  static final byte[] TWO_IMG_MAGIC = { 0x32, 0x49, 0x4D, 0x47 };
  private static String[] twoIMGFormats = { "Dos", "Prodos", "NIB" };
  private static FileSystemType[] fileSystemTypes =
      { FileSystemType.DOS3, FileSystemType.PRODOS, FileSystemType.NIB };
  private static String[] creatorCodes =
      { "!nfc", "B2TR", "CTKG", "ShIm", "WOOF", "XGS!", "CdrP" };
  private static String[] creatorNames = { "ASIMOV2", "Bernie ][ the Rescue", "Catakig",
      "Sheppy's ImageMaker", "Sweet 16", "XGS", "CiderPress" };

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

  private String displayMessage = "";

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

    text.append (String.format ("Creator ............... %s  %s%n", creator,
        getCreator (creator)));
    text.append (String.format ("Header size ........... %d%n", headerSize));
    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Format ................ %d  %s  %s%n", format,
        twoIMGFormats[format], displayMessage));
    text.append (String.format ("Flags ................. %08X%n", flags));
    text.append (String.format ("  locked .............. %s%n", locked));
    text.append (String.format ("  has Dos Volume no ... %s%n", hasDosVolumeNumber));
    text.append (String.format ("  Dos Volume no ....... %d%n", volumeNumber));
    text.append (String.format ("Blocks ................ %,d%n", prodosBlocks));
    text.append (String.format ("Data offset ........... %d%n", offset));
    text.append (
        String.format ("Data size ............. %,d%s%n", originalLength, message));
    text.append (String.format ("Comment offset ........ %,d%n", commentOffset));
    text.append (String.format ("Comment length ........ %,d%n", commentLength));
    text.append (String.format ("Comment ............... %s%n", comment));
    text.append (String.format ("Creator Data offset ... %,d%n", creatorDataOffset));
    text.append (String.format ("Creator Data length ... %,d", creatorDataLength));

    return text.toString ();
  }
}
