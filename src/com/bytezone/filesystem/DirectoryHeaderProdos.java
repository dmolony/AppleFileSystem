package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatMeta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class DirectoryHeaderProdos
// -----------------------------------------------------------------------------------//
{
  private static Locale US = Locale.US;          // to force 3 character months
  private static final DateTimeFormatter df =
      DateTimeFormatter.ofPattern ("d-LLL-yy", US);
  private static final DateTimeFormatter tf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  //  int firstCatalogBlock;
  protected final String fileName;
  protected final int storageType;
  protected final int version;
  protected final int minVersion;
  protected final int access;
  protected final int entryLength;
  protected final int entriesPerBlock;
  protected final String reserved;

  int fileCount;                  // modified if a file is added or deleted

  final int keyPtr;               // bitmap ptr or first directory block
  final int totalBlocks;          // if volume directory header
  final int parentEntry;          // if subdirectory header (1:13)
  final int parentEntryLength;    // if subdirectory header (always 0x27)

  final int folderType;           // 0 = Volume Directory, 0x75 = Subdirectory (0x76?)

  final LocalDateTime created;
  final String dateCreated, timeCreated;
  final String storageTypeText;

  final List<AppleBlock> catalogBlocks = new ArrayList<AppleBlock> ();

  // Either a Volume Directory Header or Subdirectory Header. It is the first of up to
  // 13 entries in the first directory block. The Volume Directory always has exactly
  // 4 directory blocks, while subdirectories have  1 - n blocks.
  // A Directory Header keeps a running total of the number of active entries in the
  // directory.
  //
  // ---------------------------------------------------------------------------------//
  DirectoryHeaderProdos (FsProdos fs, int firstCatalogBlockNo)
  // ---------------------------------------------------------------------------------//
  {
    getCatalogBlocks (fs, firstCatalogBlockNo);
    byte[] buffer = catalogBlocks.get (0).getBuffer ();

    storageType = (buffer[0x04] & 0xF0) >>> 4;
    storageTypeText = ProdosConstants.storageTypes[storageType];

    int nameLength = buffer[0x04] & 0x0F;
    fileName = nameLength > 0 ? Utility.string (buffer, 0x05, nameLength) : "";

    folderType = buffer[0x14] & 0xFF;                     // 0x75 for SDH
    reserved = Utility.formatRaw (buffer, 0x14, 7);

    created = Utility.getAppleDate (buffer, 0x1C);
    dateCreated = created == null ? NO_DATE : created.format (df);
    timeCreated = created == null ? "" : created.format (tf);

    version = buffer[0x20] & 0xFF;
    minVersion = buffer[0x21] & 0xFF;
    access = buffer[0x22] & 0xFF;
    entryLength = buffer[0x23] & 0xFF;
    entriesPerBlock = buffer[0x24] & 0xFF;
    fileCount = Utility.unsignedShort (buffer, 0x25);

    if (entryLength != ProdosConstants.ENTRY_SIZE
        || entriesPerBlock != ProdosConstants.ENTRIES_PER_BLOCK)
      throw new FileFormatException ("FsProdos: Invalid entry data");

    // bitmap pointer for VOL, first directory block for DIR
    keyPtr = Utility.unsignedShort (buffer, 0x27);

    if (storageType == 0x0F)         // volume directory header
    {
      totalBlocks = Utility.unsignedShort (buffer, 0x29);
      parentEntry = -1;
      parentEntryLength = -1;
    }
    else                             // subdirectory header
    {
      totalBlocks = -1;
      parentEntry = buffer[0x29] & 0xFF;
      parentEntryLength = buffer[0x2A] & 0xFF;
    }
  }

  // ---------------------------------------------------------------------------------//
  private void getCatalogBlocks (FsProdos fs, int nextBlockNo)
  // ---------------------------------------------------------------------------------//
  {
    int prevBlockNo;
    String subType = nextBlockNo == 2 ? "CATALOG" : "FOLDER";

    while (nextBlockNo != 0)
    {
      AppleBlock vtoc = fs.getBlock (nextBlockNo, BlockType.FS_DATA);
      if (vtoc == null)
        throw new FileFormatException ("FolderProdos: Invalid catalog");

      vtoc.setBlockSubType (subType);
      catalogBlocks.add (vtoc);

      byte[] buffer = vtoc.getBuffer ();
      prevBlockNo = Utility.unsignedShort (buffer, 0);
      nextBlockNo = Utility.unsignedShort (buffer, 2);

      if (!fs.isValidAddress (prevBlockNo))
        throw new FileFormatException (
            "FolderProdos: Invalid catalog previous block - " + prevBlockNo);

      if (!fs.isValidAddress (nextBlockNo))
        throw new FileFormatException (
            "FolderProdos: Invalid catalog next block - " + nextBlockNo);
    }
  }

  // ---------------------------------------------------------------------------------//
  private String dateBytes (int ptr)
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = catalogBlocks.get (0).getBuffer ();
    StringBuilder text = new StringBuilder ();

    for (int i = 0; i < 4; i++)
      text.append (String.format ("%02X ", buffer[ptr + i]));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    String message = "";
    if (storageType == 0x0F)
      text.append ("---- Volume Header ----\n");
    else
    {
      text.append ("--- Directory Header --\n");
      message = folderType == 0x75 ? "" : "<-- should be $75";
    }

    formatMeta (text, "Storage type", 2, storageType, storageTypeText);
    formatMeta (text, "File name", fileName);
    formatMeta (text, "Reserved", 2, folderType, message);
    formatMeta (text, "Reserved", reserved);
    formatMeta (text, "Created", dateBytes (0x1C), dateCreated, timeCreated);
    formatMeta (text, "Version", 2, version);
    formatMeta (text, "Min version", 2, minVersion);
    formatMeta (text, "Access", 2, access, Utility.getAccessText (access));
    formatMeta (text, "Entry length", 2, entryLength);
    formatMeta (text, "Entries per block", 2, entriesPerBlock);
    formatMeta (text, "File count", 2, fileCount);
    formatMeta (text, "Catalog blocks", 2, catalogBlocks.size ());

    if (storageType == 0x0F)
    {
      formatMeta (text, "Bitmap pointer", 4, keyPtr);
      formatMeta (text, "Total blocks", 4, totalBlocks);
    }
    else
    {
      formatMeta (text, "Parent pointer", 4, keyPtr, "(parent Directory Header block)");
      formatMeta (text, "Parent entry", 2, parentEntry, "(slot in catalog)");
      formatMeta (text, "Parent entry length", 2, parentEntryLength, "(always 0x27)");
    }

    return text.toString ();
  }
}
