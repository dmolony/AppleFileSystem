package com.bytezone.filesystem;

import java.nio.file.Path;

import com.bytezone.utility.DateTime;
import com.bytezone.utility.Utility;

// Shrinkit archive
// -----------------------------------------------------------------------------------//
public class FsNuFX extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] NuFile = { 0x4E, (byte) 0xF5, 0x46, (byte) 0xE9, 0x6C, (byte) 0xE5 };

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
  public FsNuFX (Path path, BlockReader reader)
  // ---------------------------------------------------------------------------------//
  {
    super (path, reader);           // reader not used

    init ();
  }

  // ---------------------------------------------------------------------------------//
  public FsNuFX (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader);           // reader not used

    init ();
  }

  // ---------------------------------------------------------------------------------//
  private void init ()
  // ---------------------------------------------------------------------------------//
  {
    setFileSystemName ("NuFX");

    byte[] buffer = blockReader.diskBuffer;
    assert Utility.isMagic (buffer, 0, NuFile);

    crc = Utility.unsignedShort (buffer, 6);
    totalRecords = Utility.unsignedLong (buffer, 8);        // no of FileNuFX
    created = new DateTime (buffer, 12);
    modified = new DateTime (buffer, 20);
    version = Utility.unsignedShort (buffer, 28);
    reserved1 = Utility.unsignedLong (buffer, 30);
    reserved2 = Utility.unsignedLong (buffer, 34);
    eof = Utility.unsignedLong (buffer, 38);
    reserved3 = Utility.unsignedLong (buffer, 42);
    reserved4 = Utility.unsignedShort (buffer, 46);

    byte[] crcBuffer = new byte[40];
    System.arraycopy (buffer, 8, crcBuffer, 0, crcBuffer.length);
    crcPassed = crc == Utility.crc16 (crcBuffer, crcBuffer.length, 0);
    if (!crcPassed)
      throw new FileFormatException ("Master CRC failed");

    readCatalog ();
  }

  // ---------------------------------------------------------------------------------//
  private void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = 48;

    for (int i = 0; i < totalRecords; i++)
    {
      FileNuFX file = new FileNuFX (this, getBuffer (), ptr);

      if (file.hasDiskImage () || file.isLibrary ())
        addFileSystem (this, file);
      else
        addFile (file);

      ptr += file.rawLength;
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toText () + "\n\n");

    text.append (String.format ("Master CRC ............ %04X   %s%n", crc,
        crcPassed ? "Passed" : "** Failed **"));
    text.append (String.format ("Records ............... %,d%n", totalRecords));
    text.append (String.format ("Created ............... %s%n", created.format ()));
    text.append (String.format ("Modified .............. %s%n", modified.format ()));
    text.append (String.format ("Version ............... %,d%n", version));
    text.append (String.format ("Reserved .............. %08X%n", reserved1));
    text.append (String.format ("Reserved .............. %08X%n", reserved2));
    text.append (String.format ("Master EOF ............ %,d%n", eof));
    text.append (String.format ("Reserved .............. %08X%n", reserved3));
    text.append (String.format ("Reserved .............. %04X", reserved4));

    return text.toString ();
  }
}
