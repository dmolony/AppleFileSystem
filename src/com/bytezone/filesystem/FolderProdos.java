package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FolderProdos extends AbstractAppleFile implements AppleFileContainer
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter df = DateTimeFormatter.ofPattern ("d-LLL-yy");
  private static final DateTimeFormatter tf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  FileEntryProdos fileEntry;

  int storageType;
  int version;
  int minVersion;
  int access;
  int entryLength;
  int entriesPerBlock;
  int fileCount;
  int keyPtr;               // bitmap ptr or first directory block
  int totalBlocks;          // if VDH
  int parentEntry;          // if subdirectory;
  int parentEntryLength;    // if subdirectory

  int folderType;           // 0 = Volume Directory, 0x75 = Subdirectory

  LocalDateTime created;
  String dateCreated, timeCreated;

  List<AppleFile> files = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  FolderProdos (FsProdos parent, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    if ((buffer[ptr] & 0xF0) == 0xF0)         // Volume Directory Header
      addDirectoryHeader (buffer, ptr);
    else                                      // Subdirectory
      fileEntry = new FileEntryProdos (buffer, ptr);

    isFolder = true;
  }

  // ---------------------------------------------------------------------------------//
  void addDirectoryHeader (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    storageType = (buffer[ptr] & 0xF0) >>> 4;
    int nameLength = buffer[ptr] & 0x0F;
    if (nameLength > 0)
      fileName = Utility.string (buffer, ptr + 1, nameLength);

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

    // bitmap pointer for VOL, first directory block for DIR
    keyPtr = Utility.unsignedShort (buffer, ptr + 0x23);

    if (folderType == 0x00)         // volume directory header
    {
      fileTypeText = "VOL";
      totalBlocks = Utility.unsignedShort (buffer, ptr + 0x25);
    }
    else                            // subdirectory header - WTF is 0x76?
    {
      fileTypeText = "DIR";
      parentEntry = buffer[ptr + 0x25] & 0xFF;
      parentEntryLength = buffer[ptr + 0x26] & 0xFF;
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    files.add (file);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()
  // ---------------------------------------------------------------------------------//
  {
    return files;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (fileEntry);
    text.append ("\n\n");

    text.append (String.format ("Type .................. %s%n",
        folderType == 0x75 ? "Sub Directory" : "Volume Directory"));
    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Min version ........... %d%n", minVersion));
    text.append (String.format ("Access ................ %02X    %<7d%n", access));
    text.append (
        String.format ("Created ............... %9s %-5s%n", dateCreated, timeCreated));
    text.append (String.format ("Entry length .......... %d%n", entryLength));
    text.append (String.format ("Entries per block ..... %d%n", entriesPerBlock));
    text.append (String.format ("File count ............ %d%n", fileCount));

    if (folderType == 0x75)
    {
      text.append (String.format ("Parent pointer ........ %d%n", keyPtr));
      text.append (String.format ("Parent entry .......... %d%n", parentEntry));
      text.append (String.format ("Parent entry length ... %d%n", parentEntryLength));
    }
    else
    {
      text.append (String.format ("Bitmap pointer ........ %d%n", keyPtr));
      text.append (String.format ("Total blocks .......... %d%n", totalBlocks));
    }

    return Utility.rtrim (text);
  }
}
