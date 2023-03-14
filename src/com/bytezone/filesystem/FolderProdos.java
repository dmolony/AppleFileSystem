package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FolderProdos extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter df = DateTimeFormatter.ofPattern ("d-LLL-yy");
  private static final DateTimeFormatter tf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  int storageType;
  int version;
  int minVersion;
  int access;
  int size;

  int fileType;
  int keyPtr;
  int eof;
  int auxType;
  int headerPtr;

  LocalDateTime created;
  String dateCreated, timeCreated;
  LocalDateTime modified;
  String dateModified, timeModified;

  // ---------------------------------------------------------------------------------//
  FolderProdos (FsProdos parent, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    isFolder = true;

    storageType = (buffer[ptr] & 0xF0) >>> 4;
    int nameLength = buffer[ptr] & 0x0F;
    if (nameLength > 0)
      fileName = Utility.string (buffer, ptr + 1, nameLength);

    version = buffer[ptr + 0x1C] & 0xFF;
    minVersion = buffer[ptr + 0x1D] & 0xFF;
    access = buffer[ptr + 0x1E] & 0xFF;

    fileType = buffer[ptr + 0x10] & 0xFF;
    fileTypeText = ProdosConstants.fileTypes[fileType];

    keyPtr = Utility.unsignedShort (buffer, ptr + 0x11);
    size = Utility.unsignedShort (buffer, ptr + 0x13);
    eof = Utility.unsignedTriple (buffer, ptr + 0x15);
    auxType = Utility.unsignedShort (buffer, ptr + 0x1F);   // pointless ?

    created = Utility.getAppleDate (buffer, ptr + 0x18);
    dateCreated = created == null ? NO_DATE : created.format (df);
    timeCreated = created == null ? "" : created.format (tf);

    modified = Utility.getAppleDate (buffer, ptr + 0x21);
    dateModified = modified == null ? NO_DATE : modified.format (df);
    timeModified = modified == null ? "" : modified.format (tf);

    headerPtr = Utility.unsignedShort (buffer, ptr + 0x25);
  }

  // ---------------------------------------------------------------------------------//
  public int getAuxType ()
  // ---------------------------------------------------------------------------------//
  {
    return auxType;
  }

  // ---------------------------------------------------------------------------------//
  public LocalDateTime getCreated ()
  // ---------------------------------------------------------------------------------//
  {
    return created;
  }

  // ---------------------------------------------------------------------------------//
  public LocalDateTime getModified ()
  // ---------------------------------------------------------------------------------//
  {
    return modified;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                                    // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return size;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    return eof;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (
        String.format ("File type ............. %d  %s%n", fileType, fileTypeText));
    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Min version ........... %d%n", minVersion));
    text.append (String.format ("Access ................ %02X    %<7d%n", access));
    text.append (String.format ("Size (blocks) ......... %04X  %<,7d%n", size));
    text.append (String.format ("Eof ................... %04X  %<,7d%n", eof));
    text.append (String.format ("Auxtype ............... %04X  %<,7d%n", auxType));
    text.append (String.format ("Key ptr ............... %04X  %<,7d%n", keyPtr));
    text.append (
        String.format ("Created ............... %9s %-5s%n", dateCreated, timeCreated));
    text.append (
        String.format ("Modified .............. %9s %-5s", dateModified, timeModified));

    return text.toString ();
  }
}
