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

  private byte version = 0x00;
  private byte minVersion = 0x00;
  private byte access = (byte) 0xE3;

  private LocalDateTime created;
  private LocalDateTime modified;
  private String dateC, timeC, dateM, timeM;

  private ForkProdos dataFork;
  private ForkProdos resourceFork;

  public enum ForkType
  {
    DATA, RESOURCE;
  }

  // ---------------------------------------------------------------------------------//
  FileProdos (FsProdos parent, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    isFile = true;

    storageType = (buffer[ptr] & 0xF0) >>> 4;
    int nameLength = buffer[ptr] & 0x0F;
    if (nameLength > 0)
      fileName = Utility.string (buffer, ptr + 1, nameLength);

    fileType = buffer[ptr + 0x10] & 0xFF;
    fileTypeText = ProdosConstants.fileTypes[fileType];

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

    if (isForkedFile ())
      createForks ();
    else
      dataFork = new ForkProdos (this, null, keyPtr, storageType, size, eof);
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
        {
          dataFork = new ForkProdos (this, ForkType.DATA, keyPtr, storageType, size, eof);
          addFile (dataFork);
        }
        else
        {
          resourceFork = new ForkProdos (this, ForkType.RESOURCE, keyPtr, storageType, size, eof);
          addFile (resourceFork);
        }
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
  public boolean isForkedFile ()
  // ---------------------------------------------------------------------------------//
  {
    return storageType == FsProdos.GSOS_EXTENDED_FILE;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    if (isForkedFile ())
      throw new FileFormatException ("Cannot read() a forked file");

    return dataFork.read ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                                  // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    if (isForkedFile ())
      throw new FileFormatException ("Cannot getLength() on a forked file");

    return dataFork.getLength ();
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
    return toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    int length = isForkedFile () ? 0 : dataFork.getLength ();      // fix this!!

    return String.format ("%-30s %-3s  %04X %4d %,10d", fileName, fileTypeText, keyPtr,
        getTotalBlocks (), length);
  }
}
