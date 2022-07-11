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
  private static final int GSOS_EXTENDED_FILE = 0x05;      // tech note #25

  private int storageType;
  private int fileType;
  private int keyPtr;
  private int size;
  private int eof;
  private int auxType;
  private int headerPtr;

  private LocalDateTime created;
  private LocalDateTime modified;
  private String dateC, timeC, dateM, timeM;

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
      createBothForks ();
    else
      dataFork = new ForkProdos (fs, keyPtr, storageType, size, eof);
  }

  // ---------------------------------------------------------------------------------//
  private void createBothForks ()
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = fileSystem.getBlock (keyPtr).read ();

    for (int ptr = 0; ptr < 512; ptr += 256)
    {
      int storageType = buffer[ptr] & 0x0F;                       // use other nybble!
      int keyPtr = Utility.unsignedShort (buffer, ptr + 1);
      int size = Utility.unsignedShort (buffer, ptr + 3);
      int eof = Utility.unsignedTriple (buffer, ptr + 5);

      if (ptr == 0)
        dataFork = new ForkProdos ((FsProdos) fileSystem, keyPtr, storageType, size, eof);
      else
        resourceFork = new ForkProdos ((FsProdos) fileSystem, keyPtr, storageType, size, eof);
    }
  }

  // ---------------------------------------------------------------------------------//
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return fileType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    if (storageType == GSOS_EXTENDED_FILE)
    {
      return true ? dataFork.read () : resourceFork.read ();
    }
    else
      return dataFork.read ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    if (storageType == GSOS_EXTENDED_FILE)
    {
      return true ? dataFork.getEof () : resourceFork.getEof ();
    }
    else
      return eof;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    if (storageType == GSOS_EXTENDED_FILE)
    {
      return size;
    }
    else
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
