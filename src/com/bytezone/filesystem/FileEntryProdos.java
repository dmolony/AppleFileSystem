package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileEntryProdos
// -----------------------------------------------------------------------------------//
{
  private static Locale US = Locale.US;                 // to force 3 character months
  private static final DateTimeFormatter df =
      DateTimeFormatter.ofPattern ("d-LLL-yy", US);
  private static final DateTimeFormatter tf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  final String fileName;
  final int fileType;
  final boolean isLocked;

  final int storageType;
  final int keyPtr;
  final int blocksUsed;
  final int eof;
  final int auxType;
  final int headerPtr;

  final int version;
  final int minVersion;
  final int access;

  final LocalDateTime created;
  final LocalDateTime modified;
  final String dateCreated, timeCreated;
  final String dateModified, timeModified;

  final String fileTypeText;
  final String storageTypeText;

  // ---------------------------------------------------------------------------------//
  public FileEntryProdos (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    storageType = (buffer[ptr] & 0xF0) >>> 4;

    int nameLength = buffer[ptr] & 0x0F;
    fileName = nameLength > 0 ? Utility.string (buffer, ptr + 1, nameLength) : "";

    fileType = buffer[ptr + 0x10] & 0xFF;
    keyPtr = Utility.unsignedShort (buffer, ptr + 0x11);
    blocksUsed = Utility.unsignedShort (buffer, ptr + 0x13);
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

    fileTypeText = ProdosConstants.fileTypes[fileType];
    storageTypeText = ProdosConstants.storageTypes[storageType];

    dateCreated = created == null ? NO_DATE : created.format (df);
    timeCreated = created == null ? "" : created.format (tf);

    dateModified = modified == null ? NO_DATE : modified.format (df);
    timeModified = modified == null ? "" : modified.format (tf);
  }

  // ---------------------------------------------------------------------------------//
  // According to Inside Prodos:
  // BLOCKS_USED: The total number of blocks used by this file including index blocks
  //              and data blocks.
  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    //    String message = "";
    //    if (dataBlocks.size () * 512 < eof)
    //      message = "<-- past data blocks";

    text.append (String.format ("Storage type .......... %02X  %s%n", storageType,
        storageTypeText));
    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (
        String.format ("File type ............. %02X  %s%n", fileType, fileTypeText));
    text.append (String.format ("Key ptr ............... %04X    %<,7d%n", keyPtr));
    text.append (String.format ("Blocks used ........... %04X    %<,7d%n", blocksUsed));
    text.append (String.format ("Eof ................... %06X %<,8d%n", eof));
    text.append (
        String.format ("Created ............... %9s %-5s%n", dateCreated, timeCreated));
    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Min version ........... %d%n", minVersion));
    text.append (String.format ("Access ................ %02X      %<7d%n", access));
    text.append (String.format ("Auxtype ............... %04X    %<,7d%n", auxType));
    text.append (
        String.format ("Modified .............. %9s %-5s%n", dateModified, timeModified));
    text.append (String.format ("Header ptr ............ %04X    %<,7d%n", headerPtr));

    return Utility.rtrim (text);
  }
}
