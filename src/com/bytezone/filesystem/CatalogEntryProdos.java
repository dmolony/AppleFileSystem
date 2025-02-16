package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class CatalogEntryProdos extends CatalogEntry
// -----------------------------------------------------------------------------------//
{
  private static Locale US = Locale.US;                 // to force 3 character months
  private static final DateTimeFormatter df =
      DateTimeFormatter.ofPattern ("d-LLL-yy", US);
  private static final DateTimeFormatter tf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  //  String fileName;
  int fileType;
  boolean isLocked;

  int storageType;
  int keyPtr;                 // blockNumber 
  int blocksUsed;
  int eof;
  int auxType;
  int headerPtr;              // catalog sector containing the file count

  int version;
  int minVersion;
  int access;

  LocalDateTime created;
  LocalDateTime modified;
  String dateCreated, timeCreated;
  String dateModified, timeModified;

  String fileTypeText;
  String storageTypeText;

  // ---------------------------------------------------------------------------------//
  CatalogEntryProdos (AppleBlock catalogBlock, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (catalogBlock, slot);

    read ();

  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void read ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = 4 + slot * ProdosConstants.ENTRY_SIZE;

    byte[] buffer = catalogBlock.getBuffer ();
    storageType = (buffer[ptr] & 0xF0) >>> 4;         // 0=deleted

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

    isLocked = (access & 0x01) != 0;

    fileTypeText = ProdosConstants.fileTypes[fileType];
    storageTypeText = ProdosConstants.storageTypes[storageType];

    dateCreated = created == null ? NO_DATE : created.format (df);
    timeCreated = created == null ? "" : created.format (tf);

    dateModified = modified == null ? NO_DATE : modified.format (df);
    timeModified = modified == null ? "" : modified.format (tf);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void write ()
  // ---------------------------------------------------------------------------------//
  {

  }

  // ---------------------------------------------------------------------------------//
  @Override
  void delete ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = 4 + slot * ProdosConstants.ENTRY_SIZE;

    // mark this file's entry as deleted
    byte[] buffer = catalogBlock.getBuffer ();
    buffer[ptr] = 0;              // deleted
    catalogBlock.markDirty ();

    // reduce total files in header
    AppleFileSystem fs = catalogBlock.getFileSystem ();
    AppleBlock headerBlock = fs.getBlock (headerPtr);

    buffer = headerBlock.getBuffer ();
    int fileCount = Utility.unsignedShort (buffer, 0x25);
    assert fileCount > 0;

    Utility.writeShort (buffer, 0x25, fileCount - 1);
    headerBlock.markDirty ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    String message = eof == 0 ? message = "<-- zero" : "";

    //    text.append ("---- Catalog entry ----\n");
    text.append (String.format ("Storage type .......... %02X               %s%n",
        storageType, storageTypeText));
    //    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (String.format ("File type ............. %02X      %<,7d  %s%n", fileType,
        fileTypeText));
    text.append (String.format ("Key ptr ............... %04X    %<,7d%n", keyPtr));
    text.append (String.format ("Blocks used ........... %04X    %<,7d%n", blocksUsed));
    text.append (
        String.format ("EOF ................... %06X %<,8d  %s%n", eof, message));
    text.append (
        String.format ("Created ............... %9s %-5s%n", dateCreated, timeCreated));
    text.append (
        String.format ("Modified .............. %9s %-5s%n", dateModified, timeModified));
    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Min version ........... %d%n", minVersion));
    text.append (String.format ("Access ................ %02X      %<7d  %s%n", access,
        Utility.getAccessText (access)));
    text.append (String.format ("Auxtype ............... %04X    %<,7d%n", auxType));
    text.append (String.format ("Header ptr ............ %04X    %<,7d%n", headerPtr));

    return Utility.rtrim (text);
  }
}
