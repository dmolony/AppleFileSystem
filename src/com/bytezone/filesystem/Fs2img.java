package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class Fs2img extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] TWO_IMG = { 0x32, 0x49, 0x4D, 0x47 };
  private static String[] twoIMGFormats = { "Dos", "Prodos", "NIB" };

  String creator;
  int offset;
  int length;
  int headerSize;
  int version;
  int format;
  int prodosBlocks;
  int flags;
  int commentOffset;
  int commentLength;
  int creatorDataOffset;
  int creatorDataLength;
  String comment;

  // ---------------------------------------------------------------------------------//
  public Fs2img (String name, byte[] buffer, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public Fs2img (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);

    setFileSystemName ("2img");

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
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    addFileSystem (this, getName (), diskBuffer, offset, length);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Creator ............... %s%n", creator));
    text.append (String.format ("Header size ........... %d%n", headerSize));
    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Format ................ %d  %s%n", format, twoIMGFormats[format]));
    text.append (String.format ("Flags ................. %08X%n", flags));
    text.append (String.format ("Blocks ................ %,d%n", prodosBlocks));
    text.append (String.format ("Data offset ........... %d%n", offset));
    text.append (String.format ("Data size ............. %,d%n", length));
    text.append (String.format ("Comment offset ........ %d%n", commentOffset));
    text.append (String.format ("Comment length ........ %,d%n", commentLength));
    text.append (String.format ("Comment ............... %s%n", comment));
    text.append (String.format ("Creator Data offset ... %d%n", creatorDataOffset));
    text.append (String.format ("Creator Data length ... %,d", creatorDataLength));

    return text.toString ();
  }
}
