package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class CatalogEntryPascal extends CatalogEntry
// -----------------------------------------------------------------------------------//
{
  private static final int CATALOG_ENTRY_SIZE = 26;
  private static final DateTimeFormatter dtf =
      DateTimeFormatter.ofLocalizedDate (FormatStyle.SHORT);

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
  //  String fileName;
  int wildCard;
  int bytesUsedInLastBlock;
  LocalDate fileDate;

  // ---------------------------------------------------------------------------------//
  public CatalogEntryPascal (List<AppleBlock> catalogBlocks, byte[] buffer, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (catalogBlocks, buffer, slot);

    read ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void read ()
  // ---------------------------------------------------------------------------------//
  {
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
  @Override
  void write ()
  // ---------------------------------------------------------------------------------//
  {
    if (slot == 0)
    {
      Utility.writeShort (buffer, 0x10, totalFiles);
    }
    else
    {
      int ptr = slot * CATALOG_ENTRY_SIZE;

      Utility.writeShort (buffer, ptr, firstBlock);
      Utility.writeShort (buffer, ptr + 2, lastBlock);

      buffer[ptr + 4] = (byte) fileType;
      buffer[ptr + 5] = (byte) wildCard;

      Utility.writePascalString (fileName, buffer, ptr + 6);
      Utility.writeShort (buffer, ptr + 22, bytesUsedInLastBlock);

      if (fileDate == null)
        Utility.writeShort (buffer, ptr + 24, 0);
      else
        Utility.writePascalLocalDate (fileDate, buffer, ptr + 24);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void delete ()
  // ---------------------------------------------------------------------------------//
  {
    clear ();
  }

  // ---------------------------------------------------------------------------------//
  void clear ()
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

    write ();
  }

  // file size in blocks
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
  String fileDateFormatted ()
  // ---------------------------------------------------------------------------------//
  {
    return fileDate == null ? "" : fileDate.format (dtf);
  }

  // ---------------------------------------------------------------------------------//
  String getLine ()
  // ---------------------------------------------------------------------------------//
  {
    if (slot == 0)
      return String.format ("%2d  %-20s  %3d  %3d  %5d  %3d", slot, volumeName,
          firstCatalogBlock, lastCatalogBlock, totalBlocks, totalFiles);

    String name = fileName == null ? "" : fileName;

    return String.format ("%2d  %-20s  %3d  %3d  %3d  %2d  %s", slot, name, firstBlock,
        lastBlock, bytesUsedInLastBlock, fileType, fileDateFormatted ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    if (slot == 0)
    {
      text.append ("---- Pascal Header ----\n");
      formatText (text, "Volume name", volumeName);
      formatText (text, "First catalog block", 4, firstCatalogBlock);
      formatText (text, "First file block", 4, lastCatalogBlock);
      formatText (text, "Entry type", 4, entryType);
      formatText (text, "Total blocks", 4, totalBlocks);
      formatText (text, "Total files", 4, totalFiles);
      formatText (text, "Date", volumeDate);
    }
    else
    {
      formatText (text, "First block", 4, firstBlock);
      formatText (text, "Last block", 4, lastBlock);
      formatText (text, "Bytes in last block", 4, bytesUsedInLastBlock);
      formatText (text, "Date", fileDateFormatted ());
    }

    return Utility.rtrim (text);
  }
}
