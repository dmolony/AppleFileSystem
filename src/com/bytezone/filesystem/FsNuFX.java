package com.bytezone.filesystem;

import com.bytezone.utility.DateTime;
import com.bytezone.utility.Utility;

// Shrinkit archive
// -----------------------------------------------------------------------------------//
public class FsNuFX extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] NuFile =
      { 0x4E, (byte) 0xF5, 0x46, (byte) 0xE9, 0x6C, (byte) 0xE5 };

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
  public FsNuFX (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.NUFX);           // reader not used

    byte[] buffer = blockReader.getDiskBuffer ();
    int diskOffset = blockReader.getDiskOffset ();

    assert blockReader.isMagic (0, NuFile);

    crc = Utility.unsignedShort (buffer, diskOffset + 6);
    totalRecords = Utility.unsignedLong (buffer, diskOffset + 8);        // no of FileNuFX
    created = new DateTime (buffer, diskOffset + 12);
    modified = new DateTime (buffer, diskOffset + 20);
    version = Utility.unsignedShort (buffer, diskOffset + 28);
    reserved1 = Utility.unsignedLong (buffer, diskOffset + 30);
    reserved2 = Utility.unsignedLong (buffer, diskOffset + 34);
    eof = Utility.unsignedLong (buffer, diskOffset + 38);
    reserved3 = Utility.unsignedLong (buffer, diskOffset + 42);
    reserved4 = Utility.unsignedShort (buffer, diskOffset + 46);

    byte[] crcBuffer = new byte[40];
    System.arraycopy (buffer, diskOffset + 8, crcBuffer, 0, crcBuffer.length);
    crcPassed = crc == Utility.crc16 (crcBuffer, crcBuffer.length, 0);
    if (!crcPassed)
      throw new FileFormatException ("Master CRC failed");

    int ptr = 48;

    for (int i = 0; i < totalRecords; i++)
    {
      FileNuFX file = new FileNuFX (this, getDiskBuffer (), ptr);

      String fileName = file.getFullFileName ();
      int pos;
      char separator = file.getSeparator ();
      AppleFile container = this;

      while ((pos = fileName.indexOf (separator)) >= 0)
      {
        container = getFolder (container, fileName.substring (0, pos));
        fileName = fileName.substring (pos + 1);
      }

      if (file.hasDiskImage () || file.isLibrary ())
        addFileSystem (container, file);
      else
        container.addFile (file);

      ptr += file.rawLength;
    }
  }

  // ---------------------------------------------------------------------------------//
  private FolderNuFX getFolder (AppleFile container, String folderName)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleFile appleFile : container.getFiles ())
      if (appleFile.getFileName ().equals (folderName) && appleFile.isFolder ())
        return (FolderNuFX) appleFile;

    FolderNuFX folder = new FolderNuFX (this, folderName);
    container.addFile (folder);

    return folder;
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
