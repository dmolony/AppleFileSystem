package com.bytezone.nufx;

import static com.bytezone.filesystem.ProdosConstants.fileTypes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bytezone.filesystem.Utility;

// -----------------------------------------------------------------------------------//
public class Binary2Header
// -----------------------------------------------------------------------------------//
{
  static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern ("dd-LLL-yy HH:mm");
  static final String[] osTypes = { "Prodos", "DOS 3.3", "Reserved", "DOS 3.2 or 3.1", "Pascal",
      "Macintosh MFS", "Macintosh HFS", "Lisa", "CPM", "Reserved", "MS-DOS", "High Sierra (CD-ROM)",
      "ISO 9660 (CD-ROM)", "AppleShare" };
  static final String[] storageTypes = { "Deleted", "Seedling", "Sapling", "Tree", "", "", "", "",
      "", "", "", "", "", "Subdirectory", "Subdirectory Header", "Volume Directory Header" };

  int ptr;
  byte[] buffer;

  int accessCode;
  byte fileType;
  int auxType;
  int storageType;
  int totalBlocks;
  LocalDateTime modified;
  LocalDateTime created;
  int id;                        // always 0x02
  int eof;
  String fileName;
  String nativeFileName;
  int prodos16accessCode;
  int prodos16fileType;
  int prodos16storageType;
  int prodos16totalBlocks;
  int prodos16eof;
  long diskSpaceRequired;
  int osType;
  int nativeFileType;
  int phantomFileFlag;
  int dataFlags;
  int version;
  int filesToFollow;

  boolean compressed;
  boolean encrypted;
  boolean sparsePacked;

  // ---------------------------------------------------------------------------------//
  public Binary2Header (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    this.ptr = ptr;
    this.buffer = buffer;

    accessCode = buffer[ptr + 3] & 0xFF;
    fileType = buffer[ptr + 4];
    auxType = Utility.getShort (buffer, ptr + 5);
    storageType = buffer[ptr + 7] & 0xFF;
    totalBlocks = Utility.getShort (buffer, ptr + 8);
    modified = Utility.getAppleDate (buffer, ptr + 10);
    created = Utility.getAppleDate (buffer, ptr + 14);
    id = buffer[ptr + 18] & 0xFF;
    eof = Utility.readTriple (buffer, ptr + 20);
    fileName = Utility.getPascalString (buffer, ptr + 23);
    prodos16accessCode = buffer[ptr + 111] & 0xFF;
    prodos16fileType = buffer[ptr + 112] & 0xFF;
    prodos16storageType = buffer[113] & 0xFF;
    prodos16totalBlocks = Utility.getShort (buffer, ptr + 114);
    prodos16eof = buffer[ptr + 116] & 0xFF;
    diskSpaceRequired = Utility.getLong (buffer, ptr + 117);
    osType = buffer[ptr + 121] & 0xFF;
    nativeFileType = Utility.getShort (buffer, ptr + 122);
    phantomFileFlag = buffer[ptr + 124] & 0xFF;
    dataFlags = buffer[ptr + 125] & 0xFF;
    version = buffer[ptr + 126] & 0xFF;
    filesToFollow = buffer[ptr + 127] & 0xFF;

    compressed = (dataFlags & 0x80) != 0;
    encrypted = (dataFlags & 0x40) != 0;
    sparsePacked = (dataFlags & 0x01) != 0;
  }

  // ---------------------------------------------------------------------------------//
  public String getLine ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format (" %-33s %3s  $%04X  %s  unc   %7d", fileName, fileTypes[fileType],
        auxType, modified.format (formatter), eof);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("Binary2 Header\n==============\n");
    text.append (String.format ("Access ................ %02X%n", accessCode));
    text.append (String.format ("File type ............. %02X%n", fileType));
    text.append (String.format ("Aux type .............. %04X%n", auxType));
    text.append (String.format ("Storage type .......... %02X  %s%n", storageType,
        storageTypes[storageType]));
    text.append (String.format ("Total blocks .......... %04X  %<,d%n", totalBlocks));
    text.append (String.format ("Modified .............. %s%n", modified));
    text.append (String.format ("Created ............... %s%n", created));
    text.append (String.format ("ID (0x02) ............. %02X%n", id));
    text.append (String.format ("End of file ........... %06X  %<,d%n", eof));
    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (String.format ("Prodos access ......... %02X%n", prodos16accessCode));
    text.append (String.format ("Prodos file type ...... %02X%n", prodos16fileType));
    text.append (String.format ("Prodos storage type ... %02X%n", prodos16storageType));
    text.append (String.format ("Prodos total blocks ... %02X%n", prodos16totalBlocks));
    text.append (String.format ("Prodos eof ............ %06X  %<,d%n", prodos16eof));
    text.append (String.format ("Disk space needed ..... %08X  %<,d%n", diskSpaceRequired));
    text.append (String.format ("OS type ............... %02X  %s%n", osType, osTypes[osType]));
    text.append (String.format ("Native file type ...... %02X%n", nativeFileType));
    text.append (String.format ("Data flags ............ %02X%n", dataFlags));
    text.append (String.format ("Version ............... %02X%n", version));
    text.append (String.format ("Following files ....... %02X%n", filesToFollow));

    return text.toString ();
  }
}