package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class CatalogBlockProdos
{
  private static Locale US = Locale.US;          // to force 3 character months
  private static final DateTimeFormatter df =
      DateTimeFormatter.ofPattern ("d-LLL-yy", US);
  private static final DateTimeFormatter tf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  final String fileName;
  final int storageType;
  final int version;
  final int minVersion;
  final int access;
  final int entryLength;
  final int entriesPerBlock;

  int fileCount;                  // modified if a file is added or deleted

  final int keyPtr;               // bitmap ptr or first directory block
  final int totalBlocks;          // if volume directory header
  final int parentEntry;          // if subdirectory header
  final int parentEntryLength;    // if subdirectory header

  final int folderType;           // 0 = Volume Directory, 0x75 = Subdirectory (0x76?)

  final LocalDateTime created;
  final String dateCreated, timeCreated;
  final String storageTypeText;

  final FsProdos fileSystem;
  final List<AppleBlock> catalogBlocks = new ArrayList<AppleBlock> ();

  // ---------------------------------------------------------------------------------//
  CatalogBlockProdos (FsProdos fs, int firstCatalogBlockNo)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystem = fs;

    int ptr = 4;
    getCatalogBlocks (firstCatalogBlockNo);
    byte[] buffer = catalogBlocks.get (0).getBuffer ();

    storageType = (buffer[ptr] & 0xF0) >>> 4;
    storageTypeText = ProdosConstants.storageTypes[storageType];

    int nameLength = buffer[ptr] & 0x0F;
    fileName = nameLength > 0 ? Utility.string (buffer, ptr + 1, nameLength) : "";

    folderType = buffer[ptr + 0x10] & 0xFF;

    created = Utility.getAppleDate (buffer, ptr + 0x18);
    dateCreated = created == null ? NO_DATE : created.format (df);
    timeCreated = created == null ? "" : created.format (tf);

    version = buffer[ptr + 0x1C] & 0xFF;
    minVersion = buffer[ptr + 0x1D] & 0xFF;
    access = buffer[ptr + 0x1E] & 0xFF;
    entryLength = buffer[ptr + 0x1F] & 0xFF;
    entriesPerBlock = buffer[ptr + 0x20] & 0xFF;
    fileCount = Utility.unsignedShort (buffer, ptr + 0x21);

    if (entryLength != ProdosConstants.ENTRY_SIZE
        || entriesPerBlock != ProdosConstants.ENTRIES_PER_BLOCK)
      throw new FileFormatException ("FsProdos: Invalid entry data");

    // bitmap pointer for VOL, first directory block for DIR
    keyPtr = Utility.unsignedShort (buffer, ptr + 0x23);

    if (storageType == 0x0F)         // volume directory header
    {
      totalBlocks = Utility.unsignedShort (buffer, ptr + 0x25);
      parentEntry = -1;
      parentEntryLength = -1;
    }
    else                             // subdirectory header
    {
      totalBlocks = -1;
      parentEntry = buffer[ptr + 0x25] & 0xFF;
      parentEntryLength = buffer[ptr + 0x26] & 0xFF;
    }
  }

  // ---------------------------------------------------------------------------------//
  private void getCatalogBlocks (int nextBlockNo)
  // ---------------------------------------------------------------------------------//
  {
    int prevBlockNo;
    String subType = nextBlockNo == 2 ? "CATALOG" : "FOLDER";

    while (nextBlockNo != 0)
    {
      AppleBlock vtoc = fileSystem.getBlock (nextBlockNo, BlockType.FS_DATA);
      if (vtoc == null)
        throw new FileFormatException ("FolderProdos: Invalid catalog");

      vtoc.setBlockSubType (subType);
      catalogBlocks.add (vtoc);

      byte[] buffer = vtoc.getBuffer ();
      prevBlockNo = Utility.unsignedShort (buffer, 0);
      nextBlockNo = Utility.unsignedShort (buffer, 2);

      if (!fileSystem.isValidAddress (prevBlockNo))
        throw new FileFormatException (
            "FolderProdos: Invalid catalog previous block - " + prevBlockNo);

      if (!fileSystem.isValidAddress (nextBlockNo))
        throw new FileFormatException (
            "FolderProdos: Invalid catalog next block - " + nextBlockNo);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("---- Prodos Header ----\n");
    text.append (String.format ("Storage type .......... %02X  %s%n", storageType,
        storageTypeText));
    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (String.format ("Reserved .............. $%02X%n", folderType));
    text.append (
        String.format ("Created ............... %9s %-5s%n", dateCreated, timeCreated));
    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Min version ........... %d%n", minVersion));
    text.append (String.format ("Access ................ %02X    %<7d%n", access));
    text.append (String.format ("Entry length .......... %d%n", entryLength));
    text.append (String.format ("Entries per block ..... %d%n", entriesPerBlock));
    text.append (String.format ("File count ............ %d%n", fileCount));
    text.append (String.format ("Catalog blocks ........ %d%n", catalogBlocks.size ()));

    if (storageType == 0x0F)
    {
      text.append (String.format ("Bitmap pointer ........ %d%n", keyPtr));
      text.append (String.format ("Total blocks .......... %d%n", totalBlocks));
    }
    else
    {
      text.append (String.format ("Parent pointer ........ %d%n", keyPtr));
      text.append (String.format ("Parent entry .......... %d%n", parentEntry));
      text.append (String.format ("Parent entry length ... %d%n", parentEntryLength));
    }

    return text.toString ();
  }
}
