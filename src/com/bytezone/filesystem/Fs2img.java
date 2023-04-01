package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class Fs2img extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] TWO_IMG = { 0x32, 0x49, 0x4D, 0x47 };
  private static String[] twoIMGFormats = { "Dos", "Prodos", "NIB" };
  private static FileSystemType[] fileSystemTypes =
      { FileSystemType.DOS, FileSystemType.PRODOS, FileSystemType.NIB };
  private static String[] creatorCodes =
      { "!nfc", "B2TR", "CTKG", "ShIm", "WOOF", "XGS!", "CdrP" };
  private static String[] creatorNames = { "ASIMOV2", "Bernie ][ the Rescue", "Catakig",
      "Sheppy's ImageMaker", "Sweet 16", "XGS", "CiderPress" };

  private String creator;
  private int offset;
  private int length;
  private int originalLength;
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

  private String displayMessage = "";

  private boolean locked;
  private boolean hasDosVolumeNumber;
  private int volumeNumber;

  // ---------------------------------------------------------------------------------//
  public Fs2img (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.IMG2);

    byte[] buffer = blockReader.getDiskBuffer ();
    int diskOffset = blockReader.getDiskOffset ();

    assert blockReader.isMagic (0, TWO_IMG);

    creator = new String (buffer, diskOffset + 4, 4);
    headerSize = Utility.unsignedShort (buffer, diskOffset + 8);
    version = Utility.unsignedShort (buffer, diskOffset + 10);
    format = Utility.unsignedLong (buffer, diskOffset + 12);
    flags = Utility.unsignedLong (buffer, diskOffset + 16);
    prodosBlocks = Utility.unsignedLong (buffer, diskOffset + 20);

    offset = Utility.unsignedLong (buffer, diskOffset + 24);
    originalLength = Utility.unsignedLong (buffer, diskOffset + 28);

    length = originalLength == 0 ? prodosBlocks * 512 : originalLength; // see Fantavision.2mg

    commentOffset = Utility.unsignedLong (buffer, diskOffset + 32);
    commentLength = Utility.unsignedLong (buffer, diskOffset + 36);
    creatorDataOffset = Utility.unsignedLong (buffer, diskOffset + 40);
    creatorDataLength = Utility.unsignedLong (buffer, diskOffset + 44);
    comment = commentOffset == 0 ? ""
        : new String (buffer, diskOffset + commentOffset, commentLength);

    locked = (flags & 0x8000) != 0;
    hasDosVolumeNumber = (flags & 0x0100) != 0;
    volumeNumber = flags & 0x00FF;

    //    File2img file =
    //        new File2img (this, blockReader.getName (), buffer, diskOffset + offset, length);
    //    addFile (file);

    BlockReader blockReader2 =
        new BlockReader (twoIMGFormats[format], buffer, diskOffset + offset, length);
    AppleFileSystem fileSystem = checkFileSystem (blockReader2);

    //    fileSystem = addFileSystem (this,
    //        new BlockReader (twoIMGFormats[format], buffer, diskOffset + offset, length));
    //

    if (fileSystem.getFileSystemType () != fileSystemTypes[format])
      displayMessage =
          String.format ("<-- wrong, actually %s", fileSystem.getFileSystemType ());
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
  public int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return prodosBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    for (AppleFile file : getFiles ())
      text.append (
          String.format ("%-15s %s%n", file.getFileName (), file.getFileSystemType ()));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString () + "\n\n");

    String message = originalLength == 0 ? "   <-- wrong!" : "";

    text.append (String.format ("File system type ...... %s%n", getFileSystemType ()));
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
