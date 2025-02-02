package com.bytezone.filesystem;

import java.time.LocalDate;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class CatalogEntryPascal
// -----------------------------------------------------------------------------------//
{
  private static final int CATALOG_ENTRY_SIZE = 26;

  // header entry
  String volumeName;
  int firstCatalogBlock;
  int firstFileBlock;
  int entryType;
  int totalBlocks;         // size of disk
  int totalFiles;          // no of files on disk
  LocalDate volumeDate;

  // fileEntry
  int firstBlock;
  int lastBlock;
  int fileType;
  String fileName;
  int bytesUsedInLastBlock;
  int wildCard;
  LocalDate fileDate;

  // ---------------------------------------------------------------------------------//
  public CatalogEntryPascal (byte[] buffer, int slot)
  // ---------------------------------------------------------------------------------//
  {
    int ptr = slot * CATALOG_ENTRY_SIZE;

    if (slot == 0)                         // volume header
    {
      firstCatalogBlock = Utility.unsignedShort (buffer, 0);
      firstFileBlock = Utility.unsignedShort (buffer, 2);

      if (firstCatalogBlock != 0 || firstFileBlock != 6)
        throw new FileFormatException (String.format ("Pascal: from: %d, to: %d",
            firstCatalogBlock, firstFileBlock));

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
      firstBlock = Utility.unsignedShort (buffer, ptr);
      fileName = Utility.getPascalString (buffer, ptr + 6);

      if (firstBlock == 0 || fileName.isEmpty ())
        return;

      lastBlock = Utility.unsignedShort (buffer, ptr + 2);
      fileType = buffer[ptr + 4] & 0xFF;

      wildCard = buffer[ptr + 5] & 0xFF;

      bytesUsedInLastBlock = Utility.unsignedShort (buffer, ptr + 22);
      fileDate = Utility.getPascalLocalDate (buffer, ptr + 24);     // could return null

      System.out.printf ("%2d %-20s %5d %5d%n", slot, fileName, firstBlock, lastBlock);
    }
  }

  // ---------------------------------------------------------------------------------//
  static void checkFormat (byte[] buffer)
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
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return super.toString ();
  }
}
