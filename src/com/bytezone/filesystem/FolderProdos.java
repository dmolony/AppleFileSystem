package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FolderProdos extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter df = DateTimeFormatter.ofPattern ("d-LLL-yy");
  private static final DateTimeFormatter tf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  int storageType;
  int version;
  int minVersion;
  int access;
  int size;

  int fileType;
  int keyPtr;
  int eof;
  int auxType;
  int headerPtr;

  LocalDateTime created;
  String dateCreated, timeCreated;
  LocalDateTime modified;
  String dateModified, timeModified;

  // ---------------------------------------------------------------------------------//
  FolderProdos (FsProdos fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFolder = true;

    storageType = (buffer[ptr] & 0xF0) >>> 4;
    int nameLength = buffer[ptr] & 0x0F;
    if (nameLength > 0)
      name = Utility.string (buffer, ptr + 1, nameLength);

    created = Utility.getAppleDate (buffer, ptr + 0x18);
    dateCreated = created == null ? NO_DATE : created.format (df);
    timeCreated = created == null ? "" : created.format (tf);

    version = buffer[ptr + 0x1C] & 0xFF;
    minVersion = buffer[ptr + 0x1D] & 0xFF;
    access = buffer[ptr + 0x1E] & 0xFF;

    fileType = buffer[ptr + 0x10] & 0xFF;
    keyPtr = Utility.unsignedShort (buffer, ptr + 0x11);
    size = Utility.unsignedShort (buffer, ptr + 0x13);
    eof = Utility.unsignedTriple (buffer, ptr + 0x15);
    auxType = Utility.unsignedShort (buffer, ptr + 0x1F);   // pointless ?

    modified = Utility.getAppleDate (buffer, ptr + 0x21);
    dateModified = modified == null ? NO_DATE : modified.format (df);
    timeModified = modified == null ? "" : modified.format (tf);

    headerPtr = Utility.unsignedShort (buffer, ptr + 0x25);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-30s %-3s  %04X %4d %,8d", name, ProdosConstants.fileTypes[fileType],
        keyPtr, size, eof);
  }
}
