package com.bytezone.filesystem;

import java.time.LocalDateTime;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileEntryProdos
// -----------------------------------------------------------------------------------//
{
  final String fileName;
  final int fileType;
  final boolean isLocked;

  final int storageType;
  final int keyPtr;
  final int size;
  final int eof;
  final int auxType;
  final int headerPtr;

  final int version;
  final int minVersion;
  final int access;

  final LocalDateTime created;
  final LocalDateTime modified;

  // ---------------------------------------------------------------------------------//
  public FileEntryProdos (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    storageType = (buffer[ptr] & 0xF0) >>> 4;

    int nameLength = buffer[ptr] & 0x0F;
    fileName = nameLength > 0 ? Utility.string (buffer, ptr + 1, nameLength) : "";

    fileType = buffer[ptr + 0x10] & 0xFF;
    keyPtr = Utility.unsignedShort (buffer, ptr + 0x11);
    size = Utility.unsignedShort (buffer, ptr + 0x13);
    eof = Utility.unsignedTriple (buffer, ptr + 0x15);

    created = Utility.getAppleDate (buffer, ptr + 0x18);
    version = buffer[ptr + 0x1C] & 0xFF;
    minVersion = buffer[ptr + 0x1D] & 0xFF;
    access = buffer[ptr + 0x1E] & 0xFF;

    auxType = Utility.unsignedShort (buffer, ptr + 0x1F);
    modified = Utility.getAppleDate (buffer, ptr + 0x21);
    headerPtr = Utility.unsignedShort (buffer, ptr + 0x25);

    //    isLocked = (access & 0xE0) == (byte) 0xE0;
    isLocked = access == 0x01;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Min version ........... %d%n", minVersion));
    text.append (String.format ("Access ................ %02X      %<7d%n", access));
    text.append (String.format ("Size (blocks) ......... %04X    %<,7d%n", size));
    text.append (String.format ("Eof ................... %06X %<,8d%n", eof));
    text.append (String.format ("Auxtype ............... %04X    %<,7d%n", auxType));
    text.append (String.format ("Header ptr ............ %04X    %<,7d%n", headerPtr));
    text.append (String.format ("Key ptr ............... %04X    %<,7d%n", keyPtr));
    text.append (String.format ("Created ............... %9s%n", created));
    text.append (String.format ("Modified .............. %9s", modified));

    return Utility.rtrim (text);
  }
}
