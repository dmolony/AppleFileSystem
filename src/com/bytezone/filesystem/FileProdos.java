package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// -----------------------------------------------------------------------------------//
public class FileProdos extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern ("d-LLL-yy");
  private static final DateTimeFormatter stf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  static final int GSOS_EXTENDED_FILE = 0x05;      // tech note #25

  int storageType;
  int fileType;
  int keyPtr;
  int size;
  int eof;
  int auxType;
  int headerPtr;

  LocalDateTime created;
  LocalDateTime modified;
  String dateC, timeC, dateM, timeM;

  private ForkProdos dataFork;
  private ForkProdos resourceFork;

  // ---------------------------------------------------------------------------------//
  FileProdos (FsProdos fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFile = true;

    storageType = (buffer[ptr] & 0xF0) >>> 4;
    int nameLength = buffer[ptr] & 0x0F;
    if (nameLength > 0)
      name = Utility.string (buffer, ptr + 1, nameLength);

    fileType = buffer[ptr + 0x10] & 0xFF;
    keyPtr = Utility.unsignedShort (buffer, ptr + 0x11);
    size = Utility.unsignedShort (buffer, ptr + 0x13);
    eof = Utility.unsignedTriple (buffer, ptr + 0x15);

    auxType = Utility.unsignedShort (buffer, ptr + 0x1F);
    headerPtr = Utility.unsignedShort (buffer, ptr + 0x25);

    created = Utility.getAppleDate (buffer, ptr + 0x18);
    dateC = created == null ? NO_DATE : created.format (sdf);
    timeC = created == null ? "" : created.format (stf);

    modified = Utility.getAppleDate (buffer, ptr + 0x21);
    dateM = modified == null ? NO_DATE : modified.format (sdf);
    timeM = modified == null ? "" : modified.format (stf);

    if (storageType == GSOS_EXTENDED_FILE)
      readForks ();
    else
      dataFork = new ForkProdos (fs, keyPtr, storageType, size, eof);
  }

  // ---------------------------------------------------------------------------------//
  private void readForks ()
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = fileSystem.getBlock (keyPtr).read ();

    for (int ptr = 0; ptr < 512; ptr += 256)
    {
      int storageType = buffer[ptr] & 0x0F;
      int keyBlock = Utility.unsignedShort (buffer, ptr + 1);
      int size = Utility.unsignedShort (buffer, ptr + 3);
      int eof = Utility.unsignedTriple (buffer, ptr + 5);

      if (ptr == 0)
        dataFork = new ForkProdos ((FsProdos) fileSystem, keyBlock, storageType, size, eof);
      else
        resourceFork = new ForkProdos ((FsProdos) fileSystem, keyBlock, storageType, size, eof);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    // how to determine which fork to return?
    return true ? dataFork.read () : resourceFork.read ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return eof;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return size;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-30s %-3s  %04X %4d %,8d", name, ProdosConstants.fileTypes[fileType],
        keyPtr, getSize (), getLength ());
  }
}
