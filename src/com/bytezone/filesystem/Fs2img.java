package com.bytezone.filesystem;

import java.nio.file.Path;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class Fs2img extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] TWO_IMG = { 0x32, 0x49, 0x4D, 0x47 };
  private static String[] twoIMGFormats = { "Dos", "Prodos", "NIB" };
  private static String[] creatorCodes = { "!nfc", "B2TR", "CTKG", "ShIm", "WOOF", "XGS!", "CdrP" };
  private static String[] creatorNames = { "ASIMOV2", "Bernie ][ the Rescue", "Catakig",
      "Sheppy's ImageMaker", "Sweet 16", "XGS", "CiderPress" };

  private String creator;
  private int offset;
  private int length;
  private int headerSize;
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

  private AppleFileSystem fileSystem;

  // ---------------------------------------------------------------------------------//
  public Fs2img (Path path, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (path, blockReader);

    readCatalog ();
  }

  // ---------------------------------------------------------------------------------//
  public Fs2img (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader);

    readCatalog ();
  }

  // ---------------------------------------------------------------------------------//
  private void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    setFileSystemName ("2img");

    byte[] buffer = blockReader.diskBuffer;
    int offset = blockReader.diskOffset;

    assert Utility.isMagic (buffer, offset, TWO_IMG);

    creator = new String (buffer, offset + 4, 4);
    headerSize = Utility.unsignedShort (buffer, offset + 8);
    version = Utility.unsignedShort (buffer, offset + 10);
    format = Utility.unsignedLong (buffer, offset + 12);
    flags = Utility.unsignedLong (buffer, offset + 16);
    prodosBlocks = Utility.unsignedLong (buffer, offset + 20);

    this.offset = Utility.unsignedLong (buffer, offset + 24);
    this.length = Utility.unsignedLong (buffer, offset + 28);

    commentOffset = Utility.unsignedLong (buffer, offset + 32);
    commentLength = Utility.unsignedLong (buffer, offset + 36);
    creatorDataOffset = Utility.unsignedLong (buffer, offset + 40);
    creatorDataLength = Utility.unsignedLong (buffer, offset + 44);
    comment = commentOffset == 0 ? "" : new String (buffer, offset + commentOffset, commentLength);

    locked = (flags & 0x8000) != 0;
    hasDosVolumeNumber = (flags & 0x0100) != 0;
    volumeNumber = flags & 0x00FF;

    BlockReader reader =
        new BlockReader (blockReader.diskBuffer, blockReader.diskOffset + offset, length);
    fileSystem = addFileSystem (this, reader);
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
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toText () + "\n\n");

    text.append (String.format ("Creator ............... %s  %s%n", creator, getCreator (creator)));
    text.append (String.format ("Header size ........... %d%n", headerSize));
    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Format ................ %d  %s%n", format, twoIMGFormats[format]));
    text.append (String.format ("Flags ................. %08X%n", flags));
    text.append (String.format ("  locked .............. %s%n", locked));
    text.append (String.format ("  has Dos Volume no ... %s%n", hasDosVolumeNumber));
    text.append (String.format ("  Dos Volume no ....... %d%n", volumeNumber));
    text.append (String.format ("Blocks ................ %,d%n", prodosBlocks));
    text.append (String.format ("Data offset ........... %d%n", offset));
    text.append (String.format ("Data size ............. %,d%n", length));
    text.append (String.format ("Comment offset ........ %,d%n", commentOffset));
    text.append (String.format ("Comment length ........ %,d%n", commentLength));
    text.append (String.format ("Comment ............... %s%n", comment));
    text.append (String.format ("Creator Data offset ... %,d%n", creatorDataOffset));
    text.append (String.format ("Creator Data length ... %,d%n%n", creatorDataLength));

    text.append (fileSystem.toText ());

    return text.toString ();
  }
}
