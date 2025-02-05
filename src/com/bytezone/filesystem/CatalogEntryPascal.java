package com.bytezone.filesystem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class CatalogEntryPascal
// -----------------------------------------------------------------------------------//
{
  private static final int CATALOG_ENTRY_SIZE = 26;
  private static final DateTimeFormatter dtf =
      DateTimeFormatter.ofLocalizedDate (FormatStyle.SHORT);

  private final int slot;
  private byte[] catalogBuffer;

  // header entry
  String volumeName;
  int firstCatalogBlock;    // always 0
  int lastCatalogBlock;     // usually 6
  int entryType;            // always 0
  int totalBlocks;          // size of disk
  int totalFiles;           // no of files on disk
  LocalDate volumeDate;

  // fileEntry
  int firstBlock;
  int lastBlock;
  int fileType;
  String fileName;
  int wildCard;
  int bytesUsedInLastBlock;
  LocalDate fileDate;

  // ---------------------------------------------------------------------------------//
  public CatalogEntryPascal (byte[] buffer, int slot)
  // ---------------------------------------------------------------------------------//
  {
    this.slot = slot;
    this.catalogBuffer = buffer;

    if (slot == 0)                         // volume header
    {
      firstCatalogBlock = Utility.unsignedShort (buffer, 0);
      lastCatalogBlock = Utility.unsignedShort (buffer, 2);

      if (firstCatalogBlock != 0 || lastCatalogBlock != 6)
        throw new FileFormatException (String.format ("Pascal: from: %d, to: %d",
            firstCatalogBlock, lastCatalogBlock));

      entryType = Utility.unsignedShort (buffer, 4);
      if (entryType != 0)
        throw new FileFormatException ("Pascal: entry type != 0");

      int nameLength = buffer[6] & 0xFF;
      if (nameLength < 1 || nameLength > 7)
        throw new FileFormatException ("bad name length : " + nameLength);

      volumeName = Utility.string (buffer, 7, nameLength);
      totalBlocks = Utility.unsignedShort (buffer, 14);       // 280, 1600, 2048
      totalFiles = Utility.unsignedShort (buffer, 16);
      firstBlock = Utility.unsignedShort (buffer, 18);
      volumeDate = Utility.getPascalLocalDate (buffer, 20);   // 2 bytes
    }
    else
    {
      int ptr = slot * CATALOG_ENTRY_SIZE;

      firstBlock = Utility.unsignedShort (buffer, ptr);

      int nameLength = buffer[ptr + 6] & 0xFF;
      if (firstBlock == 0 || nameLength == 0 || nameLength > 15)
      {
        firstBlock = 0;
        return;
      }

      fileName = Utility.getMaskedPascalString (buffer, ptr + 6);
      lastBlock = Utility.unsignedShort (buffer, ptr + 2);
      fileType = buffer[ptr + 4] & 0xFF;
      wildCard = buffer[ptr + 5] & 0x80;

      bytesUsedInLastBlock = Utility.unsignedShort (buffer, ptr + 22);
      fileDate = Utility.getPascalLocalDate (buffer, ptr + 24);     // could return null
    }
  }

  // ---------------------------------------------------------------------------------//
  int length ()
  // ---------------------------------------------------------------------------------//
  {
    if (slot == 0)
      return lastCatalogBlock - firstCatalogBlock;

    return lastBlock - firstBlock;
  }

  // ---------------------------------------------------------------------------------//
  void copyFileEntry (CatalogEntryPascal copy, int newFirstBlock)
  // ---------------------------------------------------------------------------------//
  {
    firstBlock = newFirstBlock;
    lastBlock = newFirstBlock + copy.lastBlock - copy.firstBlock;
    fileType = copy.fileType;
    fileName = copy.fileName;
    wildCard = copy.wildCard;
    bytesUsedInLastBlock = copy.bytesUsedInLastBlock;
    fileDate = copy.fileDate;
  }

  // ---------------------------------------------------------------------------------//
  void clearFileEntry ()
  // ---------------------------------------------------------------------------------//
  {
    if (slot == 0)
    {
      --totalFiles;           // adjust header
    }
    else
    {
      firstBlock = 0;
      lastBlock = 0;
      fileType = 0;
      fileName = "";
      wildCard = 0;
      bytesUsedInLastBlock = 0;
      fileDate = null;
    }

    writeCatalogEntry ();
  }

  // ---------------------------------------------------------------------------------//
  void writeCatalogEntry ()
  // ---------------------------------------------------------------------------------//
  {
    if (slot == 0)
    {
      Utility.writeShort (catalogBuffer, 0x10, totalFiles);
    }
    else
    {
      int ptr = slot * CATALOG_ENTRY_SIZE;

      Utility.writeShort (catalogBuffer, ptr, firstBlock);
      Utility.writeShort (catalogBuffer, ptr + 2, lastBlock);

      catalogBuffer[ptr + 4] = (byte) fileType;
      catalogBuffer[ptr + 5] = (byte) wildCard;

      Utility.writePascalString (fileName, catalogBuffer, ptr + 6);
      Utility.writeShort (catalogBuffer, ptr + 22, bytesUsedInLastBlock);

      if (fileDate == null)
        Utility.writeShort (catalogBuffer, ptr + 24, 0);
      else
        Utility.writePascalLocalDate (fileDate, catalogBuffer, ptr + 24);
    }
  }

  // ---------------------------------------------------------------------------------//
  static void checkVolumeHeaderFormat (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    int firstCatalogBlock = Utility.unsignedShort (buffer, 0);
    int firstFileBlock = Utility.unsignedShort (buffer, 2);

    if (firstCatalogBlock != 0 || firstFileBlock != 6)
      throw new FileFormatException (
          String.format ("Pascal: from: %d, to: %d", firstCatalogBlock, firstFileBlock));

    int entryType = Utility.unsignedShort (buffer, 4);
    if (entryType != 0)
      throw new FileFormatException ("Pascal: entry type != 0");

    int nameLength = buffer[6] & 0xFF;
    if (nameLength < 1 || nameLength > 7)
      throw new FileFormatException ("bad name length : " + nameLength);
  }

  // ---------------------------------------------------------------------------------//
  String getLine ()
  // ---------------------------------------------------------------------------------//
  {
    if (slot == 0)
      return String.format ("%2d  %-20s  %3d  %3d  %5d  %3d", slot, volumeName,
          firstCatalogBlock, lastCatalogBlock, totalBlocks, totalFiles);

    String date = fileDate == null ? "" : fileDate.toString ();
    String name = fileName == null ? "" : fileName;

    return String.format ("%2d  %-20s  %3d  %3d  %3d  %2d  %s", slot, name, firstBlock,
        lastBlock, bytesUsedInLastBlock, fileType, date);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    if (slot == 0)
    {
      text.append ("---- Pascal Header ----\n");
      text.append (String.format ("Volume name ........... %s%n", volumeName));
      text.append (String.format ("First catalog block ... %d%n", firstCatalogBlock));
      text.append (String.format ("First file block ...... %d%n", lastCatalogBlock));
      text.append (String.format ("Entry type ............ %,d%n", entryType));
      text.append (String.format ("Total blocks .......... %,d%n", totalBlocks));
      text.append (String.format ("Total files ........... %,d%n", totalFiles));
      text.append (String.format ("Date .................. %s", volumeDate));
    }
    else
    {
      text.append ("---- Catalog Entry ----\n");
      text.append (String.format ("File name ............. %s%n", fileName));
      text.append (String.format ("First block ........... %d%n", firstBlock));
      text.append (String.format ("Last block ............ %d%n", lastBlock));
      text.append (String.format ("Bytes in last block ... %d%n", bytesUsedInLastBlock));
      text.append (
          String.format ("Date .................. %s%n%n", fileDate.format (dtf)));
    }

    return Utility.rtrim (text);
  }
}
