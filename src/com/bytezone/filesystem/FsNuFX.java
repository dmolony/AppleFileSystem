package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import com.bytezone.utility.DateTime;
import com.bytezone.utility.Utility;

// Shrinkit archive (.SHK - NuFX archive, .SDK - disk images)
// -----------------------------------------------------------------------------------//
class FsNuFX extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] NuFile =
      { 0x4E, (byte) 0xF5, 0x46, (byte) 0xE9, 0x6C, (byte) 0xE5 };
  private static final String UNDERLINE_NUFX =
      "------------------------------------------------------"
          + "-----------------------";

  private int crc;
  private int totalRecords;
  private DateTime created;
  private DateTime modified;
  private int version;
  private int reserved1;            // v1 stores file type (0xE0 - LBR)
  private int reserved2;            // v1 stores aux (0x8002)
  private int reserved3;
  private int reserved4;
  private int eof;

  private boolean crcPassed;

  // ---------------------------------------------------------------------------------//
  FsNuFX (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.NUFX);           // reader not used

    if (!Utility.isMagic (blockReader.getDiskBuffer (), 0, NuFile))
      throw new FileFormatException ("File not NuFX format");

    byte[] buffer = blockReader.getDiskBuffer ().data ();
    int diskOffset = blockReader.getDiskBuffer ().offset ();

    crc = Utility.unsignedShort (buffer, diskOffset + 6);
    totalRecords = Utility.unsignedInt (buffer, diskOffset + 8);        // no of FileNuFX
    created = new DateTime (buffer, diskOffset + 12);
    modified = new DateTime (buffer, diskOffset + 20);
    version = Utility.unsignedShort (buffer, diskOffset + 28);
    reserved1 = Utility.unsignedInt (buffer, diskOffset + 30);
    reserved2 = Utility.unsignedInt (buffer, diskOffset + 34);
    eof = Utility.unsignedInt (buffer, diskOffset + 38);
    reserved3 = Utility.unsignedInt (buffer, diskOffset + 42);
    reserved4 = Utility.unsignedShort (buffer, diskOffset + 46);

    byte[] crcBuffer = new byte[40];
    System.arraycopy (buffer, diskOffset + 8, crcBuffer, 0, crcBuffer.length);
    crcPassed = crc == Utility.crc16 (crcBuffer, crcBuffer.length, 0);
    if (!crcPassed)
      throw new FileFormatException ("Master CRC failed");

    int ptr = 48;

    for (int i = 0; i < totalRecords; i++)
    {
      FileNuFX file = new FileNuFX (this, getDiskBuffer ().data (), ptr);

      if (file.hasDisk () || file.isLibrary ())
        addEmbeddedFileSystem (file, 0);

      addFile (file);         // never uses fileSystems<>

      ptr += file.rawLength;
    }
  }

  // ---------------------------------------------------------------------------------//
  public DateTime getCreated ()
  // ---------------------------------------------------------------------------------//
  {
    return created;
  }

  // ---------------------------------------------------------------------------------//
  public DateTime getModified ()
  // ---------------------------------------------------------------------------------//
  {
    return modified;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    //    if (appleFile instanceof FileNuFX)          // forked file
    //    {
    //      for (AppleFile file2 : appleFile.getFiles ())
    //        text.append (file2.getFileName () + "(fork) \n");
    //      return text.toString ();
    //    }

    //    if (appleFile instanceof Folder)
    //    {
    //      for (AppleFile file2 : appleFile.getFiles ())
    //        text.append (getNuFXLine ((FileNuFX) file2));
    //      return text.toString ();
    //    }

    text.append (
        String.format (" %-15.15s Created:%-17s Mod:%-17s   Recs:%5d\n\n", getFileName (),
            getCreated ().format (), getModified ().format (), getFiles ().size ()));

    text.append (" Name                        Type Auxtyp Archived"
        + "         Fmat Size Un-Length\n");

    text.append (String.format ("%s\n", UNDERLINE_NUFX));

    int totalUncompressedSize = 0;
    int totalCompressedSize = 0;

    for (AppleFile file : getFiles ())
      if (file instanceof FileNuFX file2)
      {
        text.append (file2.getCatalogLine ());
        text.append ("\n");

        totalUncompressedSize += file2.getUncompressedSize ();
        totalCompressedSize += file2.getCompressedSize ();
      }

    text.append (String.format ("%s\n", UNDERLINE_NUFX));

    float pct = 0;
    if (totalUncompressedSize > 0)
      pct = totalCompressedSize * 100 / totalUncompressedSize;
    text.append (String.format (" Uncomp:%7d  Comp:%7d  %%of orig:%3.0f%%\n\n",
        totalUncompressedSize, totalCompressedSize, pct));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("----- NuFX Header -----\n");
    formatText (text, "Master CRC", 4, crc, crcPassed ? "Passed" : "** Failed **");
    formatText (text, "Records", 2, totalRecords);
    formatText (text, "Created", created.format ());
    formatText (text, "Modified", modified.format ());
    formatText (text, "Version", 2, version);
    formatText (text, "Reserved", 6, reserved1);
    formatText (text, "Reserved", 6, reserved2);
    formatText (text, "Master EOF", 6, eof);
    formatText (text, "Reserved", 6, reserved3);
    formatText (text, "Reserved", 4, reserved4);

    return Utility.rtrim (text);
  }
}
