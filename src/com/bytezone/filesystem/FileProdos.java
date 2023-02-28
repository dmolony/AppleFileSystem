package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileProdos extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern ("d-LLL-yy");
  private static final DateTimeFormatter stf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  private int storageType;
  private int keyPtr;
  private int size;
  private int eof;
  private int auxType;
  private int headerPtr;

  private int version = 0x00;
  private int minVersion = 0x00;
  private int access = (byte) 0xE3;

  private LocalDateTime created;
  private LocalDateTime modified;
  private String dateCreated, timeCreated, dateModified, timeModified;

  private ForkProdos data;            // for non-forked files

  public enum ForkType
  {
    DATA, RESOURCE;
  }

  // ---------------------------------------------------------------------------------//
  FileProdos (FsProdos parent, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    storageType = (buffer[ptr] & 0xF0) >>> 4;

    isFile = true;
    isForkedFile = storageType == FsProdos.GSOS_EXTENDED_FILE;

    int nameLength = buffer[ptr] & 0x0F;
    if (nameLength > 0)
      fileName = Utility.string (buffer, ptr + 1, nameLength);

    fileType = buffer[ptr + 0x10] & 0xFF;
    fileTypeText = ProdosConstants.fileTypes[fileType];

    keyPtr = Utility.unsignedShort (buffer, ptr + 0x11);
    size = Utility.unsignedShort (buffer, ptr + 0x13);
    eof = Utility.unsignedTriple (buffer, ptr + 0x15);

    created = Utility.getAppleDate (buffer, ptr + 0x18);
    version = buffer[ptr + 0x1C] & 0xFF;
    minVersion = buffer[ptr + 0x1D] & 0xFF;
    access = buffer[ptr + 0x1E] & 0xFF;

    auxType = Utility.unsignedShort (buffer, ptr + 0x1F);
    modified = Utility.getAppleDate (buffer, ptr + 0x21);
    headerPtr = Utility.unsignedShort (buffer, ptr + 0x25);

    dateCreated = created == null ? NO_DATE : created.format (sdf);
    timeCreated = created == null ? "" : created.format (stf);
    dateModified = modified == null ? NO_DATE : modified.format (sdf);
    timeModified = modified == null ? "" : modified.format (stf);

    // 
    if (isForkedFile ())
      createForks ();
    else
      data = new ForkProdos (this, null, keyPtr, storageType, size, eof);
  }

  // ---------------------------------------------------------------------------------//
  private void createForks ()
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = getFileSystem ().getBlock (keyPtr).read ();

    for (int ptr = 0; ptr < 512; ptr += 256)
    {
      int storageType = buffer[ptr] & 0x0F;                       // use other nybble!
      int keyPtr = Utility.unsignedShort (buffer, ptr + 1);
      int size = Utility.unsignedShort (buffer, ptr + 3);
      int eof = Utility.unsignedTriple (buffer, ptr + 5);

      if (keyPtr > 0)
        if (ptr == 0)
          addFile (new ForkProdos (this, ForkType.DATA, keyPtr, storageType, size, eof));
        else
          addFile (
              new ForkProdos (this, ForkType.RESOURCE, keyPtr, storageType, size, eof));

    }
  }

  // ---------------------------------------------------------------------------------//
  public int getAuxType ()
  // ---------------------------------------------------------------------------------//
  {
    return auxType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    if (isForkedFile ())
      throw new FileFormatException ("Cannot read() a forked file");

    return data.read ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                                         // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    if (isForkedFile ())
      throw new FileFormatException ("Cannot getLength() on a forked file");

    return data.getFileLength ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                                    // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return size;                              // size of both forks if GSOS extended
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    int length = isForkedFile () ? 0 : data.getFileLength ();

    return String.format ("%-30s %-3s  %04X %4d %,10d", fileName, fileTypeText, keyPtr,
        getTotalBlocks (), length);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    int length = isForkedFile () ? 0 : data.getFileLength ();

    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (
        String.format ("File type ............. %02X  %s%n", fileType, fileTypeText));
    text.append (String.format ("Key ptr ............... %04X%n", keyPtr));
    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Min version ........... %d%n", minVersion));
    text.append (String.format ("Access ................ %02X    %<7d%n", access));
    text.append (String.format ("Size (blocks) ......... %04X  %<,7d%n", size));
    text.append (String.format ("Eof ................... %04X  %<,7d%n", eof));
    text.append (String.format ("Auxtype ............... %04X  %<,7d%n", auxType));
    text.append (String.format ("Key ptr ............... %04X  %<,7d%n", keyPtr));
    text.append (String.format ("Created ............... %9s%n", dateCreated));
    text.append (String.format ("Modified .............. %9s", dateModified));

    return text.toString ();
  }
}
