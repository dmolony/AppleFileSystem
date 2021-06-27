package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// -----------------------------------------------------------------------------------//
public class FolderProdos extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter df = DateTimeFormatter.ofPattern ("d-LLL-yy");
  private static final DateTimeFormatter tf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  // Common to both
  int blockType;
  LocalDateTime created;
  String dateCreated, timeCreated;
  int version;
  int minVersion;
  int access;
  int size;

  // Subdirectory (size contains blocks used)
  int fileType;
  int keyPtr;
  int eof;
  int auxType;
  int headerPtr;
  LocalDateTime modified;
  String dateModified, timeModified;

  // ---------------------------------------------------------------------------------//
  FolderProdos (FsProdos fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFolder = true;

    blockType = (buffer[ptr] & 0xF0) >>> 4;
    int nameLength = buffer[ptr] & 0x0F;
    if (nameLength > 0)
      name = AbstractFileSystem.string (buffer, ptr + 1, nameLength);

    created = AbstractFileSystem.getAppleDate (buffer, ptr + 0x18);
    dateCreated = created == null ? NO_DATE : created.format (df);
    timeCreated = created == null ? "" : created.format (tf);

    version = buffer[ptr + 0x1C] & 0xFF;
    minVersion = buffer[ptr + 0x1D] & 0xFF;
    access = buffer[ptr + 0x1E] & 0xFF;

    switch (blockType)
    {

      case FsProdos.SUBDIRECTORY:
        fileType = buffer[ptr + 0x10] & 0xFF;
        keyPtr = AbstractFileSystem.unsignedShort (buffer, ptr + 0x11);
        size = AbstractFileSystem.unsignedShort (buffer, ptr + 0x13);
        eof = AbstractFileSystem.unsignedTriple (buffer, ptr + 0x15);
        auxType = AbstractFileSystem.unsignedShort (buffer, ptr + 0x1F);   // pointless ?

        modified = AbstractFileSystem.getAppleDate (buffer, ptr + 0x21);
        dateModified = modified == null ? NO_DATE : modified.format (df);
        timeModified = modified == null ? "" : modified.format (tf);

        headerPtr = AbstractFileSystem.unsignedShort (buffer, ptr + 0x25);
        break;
    }
  }
}
