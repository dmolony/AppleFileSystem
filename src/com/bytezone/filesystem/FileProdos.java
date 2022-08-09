package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileProdos extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern ("d-LLL-yy");
  private static final DateTimeFormatter stf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  private int storageType;
  private int fileType;
  private int keyPtr;
  private int size;
  private int eof;
  private int auxType;
  private int headerPtr;

  private byte version = 0x00;
  private byte minVersion = 0x00;
  private byte access = (byte) 0xE3;

  private LocalDateTime created;
  private LocalDateTime modified;
  private String dateC, timeC, dateM, timeM;

  private ForkProdos dataFork;
  private ForkProdos resourceFork;

  enum ForkType
  {
    DATA, RESOURCE;
  }

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

    created = Utility.getAppleDate (buffer, ptr + 0x18);
    version = buffer[ptr + 0x1C];
    minVersion = buffer[ptr + 0x1D];
    access = buffer[ptr + 0x1E];

    auxType = Utility.unsignedShort (buffer, ptr + 0x1F);
    modified = Utility.getAppleDate (buffer, ptr + 0x21);
    headerPtr = Utility.unsignedShort (buffer, ptr + 0x25);

    dateC = created == null ? NO_DATE : created.format (sdf);
    timeC = created == null ? "" : created.format (stf);
    dateM = modified == null ? NO_DATE : modified.format (sdf);
    timeM = modified == null ? "" : modified.format (stf);

    if (storageType == FsProdos.GSOS_EXTENDED_FILE)
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
    if (storageType == FsProdos.GSOS_EXTENDED_FILE)
      throw new FileFormatException ("File type is GS Extended");

    return dataFork.read ();
  }

  // ---------------------------------------------------------------------------------//
  public byte[] read (ForkType forkType)
  // ---------------------------------------------------------------------------------//
  {
    if (storageType != FsProdos.GSOS_EXTENDED_FILE)
      throw new FileFormatException ("File type not GS Extended");

    return forkType == ForkType.DATA ? dataFork.read () : resourceFork.read ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    if (storageType == FsProdos.GSOS_EXTENDED_FILE)
      throw new FileFormatException ("File type is GS Extended");

    return eof;
  }

  // ---------------------------------------------------------------------------------//
  public int getLength (ForkType forkType)                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    if (storageType != FsProdos.GSOS_EXTENDED_FILE)
      throw new FileFormatException ("File type not GS Extended");

    return forkType == ForkType.DATA ? dataFork.getEof () : resourceFork.getEof ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return size;              // size of both forks if GSOS extended
  }

  // ---------------------------------------------------------------------------------//
  public int getSize (ForkType forkType)                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    if (storageType != FsProdos.GSOS_EXTENDED_FILE)
      throw new FileFormatException ("File type not GS Extended");

    return forkType == ForkType.DATA ? dataFork.getSize () : resourceFork.getSize ();
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
