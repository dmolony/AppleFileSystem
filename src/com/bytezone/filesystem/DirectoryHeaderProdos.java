package com.bytezone.filesystem;

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

    text.append (String.format ("Storage type .......... %02X  %s%n", storageType,
        storageTypeText));
    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (String.format ("Reserved .............. $%02X              %s%n",
        folderType, message));
    text.append (String.format ("Reserved .............. %s%n", reserved));
    text.append (
        String.format ("Created ............... %9s %-5s%n", dateCreated, timeCreated));
    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Min version ........... %d%n", minVersion));
    text.append (String.format ("Access ................ %02X    %<9d  %s%n", access,
        Utility.getAccessText (access)));
    text.append (String.format ("Entry length .......... %d%n", entryLength));
    text.append (String.format ("Entries per block ..... %d%n", entriesPerBlock));
    text.append (String.format ("File count ............ %d%n", fileCount));
    text.append (String.format ("Catalog blocks ........ %d%n%n", catalogBlocks.size ()));

    if (storageType == 0x0F)
    {
      text.append (String.format ("Bitmap pointer ........ %04X  %<,9d%n", keyPtr));
      text.append (String.format ("Total blocks .......... %04X  %<,9d%n", totalBlocks));
    }
    else
    {
      text.append (String.format (
          "Parent pointer ........ %04X  %<,9d  (parent Directory Header block)%n",
          keyPtr));
      text.append (String.format (
          "Parent entry .......... %02X      %<,7d  (slot in catalog)%n", parentEntry));
      text.append (String.format (
          "Parent entry length ... %02X      %<,7d  (always 0x27)%n", parentEntryLength));
    }

    return text.toString ();
  }
}
