package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class Fs2img extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] TWO_IMG = { 0x32, 0x49, 0x4D, 0x47 };
  private static String[] twoIMGFormats = { "Dos", "Prodos", "NIB" };

  int offset;
  int length;
  int headerSize;
  int version;
  int format;
  int prodosBlocks;

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

    headerSize = Utility.unsignedShort (buffer, offset + 8);
    version = Utility.unsignedShort (buffer, offset + 10);
    format = Utility.unsignedLong (buffer, offset + 12);
    prodosBlocks = Utility.unsignedLong (buffer, offset + 20);
    this.offset = Utility.unsignedLong (buffer, offset + 24);
    this.length = Utility.unsignedLong (buffer, offset + 28);
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

    System.out.println ();
    System.out.printf ("Header size ... %d%n", headerSize);
    System.out.printf ("Version ....... %d%n", version);
    System.out.printf ("Format ........ %d  %s%n", format, twoIMGFormats[format]);
    System.out.printf ("Blocks ........ %,d%n", prodosBlocks);
    System.out.printf ("Data offset ... %d%n", offset);
    System.out.printf ("Data size ..... %,d%n", length);

    return text.toString ();
  }
}
