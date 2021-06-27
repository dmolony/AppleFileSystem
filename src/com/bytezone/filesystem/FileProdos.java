package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// -----------------------------------------------------------------------------------//
public class FileProdos extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern ("d-LLL-yy");
  private static final DateTimeFormatter stf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  int storageType;
  int fileType;
  int keyPtr;
  int size;
  int eof;
  int auxType;
  int headerPtr;
  LocalDateTime created;
  LocalDateTime modified;
  String dateC, timeC, dateM, timeM;

  // ---------------------------------------------------------------------------------//
  FileProdos (FsProdos fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFile = true;

    storageType = (buffer[ptr] & 0xF0) >> 4;
    int nameLength = buffer[ptr] & 0x0F;
    if (nameLength > 0)
      name = AbstractFileSystem.string (buffer, ptr + 1, nameLength);

    fileType = buffer[ptr + 0x10] & 0xFF;
    keyPtr = AbstractFileSystem.unsignedShort (buffer, ptr + 0x11);
    size = AbstractFileSystem.unsignedShort (buffer, ptr + 0x13);
    eof = AbstractFileSystem.unsignedTriple (buffer, ptr + 0x15);
    auxType = AbstractFileSystem.unsignedShort (buffer, ptr + 0x1F);
    headerPtr = AbstractFileSystem.unsignedShort (buffer, ptr + 0x25);

    created = AbstractFileSystem.getAppleDate (buffer, ptr + 0x18);
    dateC = created == null ? NO_DATE : created.format (sdf);
    timeC = created == null ? "" : created.format (stf);

    modified = AbstractFileSystem.getAppleDate (buffer, ptr + 0x21);
    dateM = modified == null ? NO_DATE : modified.format (sdf);
    timeM = modified == null ? "" : modified.format (stf);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return eof;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return size;
  }
}
