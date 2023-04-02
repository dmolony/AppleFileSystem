package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileProdos extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  protected static final DateTimeFormatter sdf = DateTimeFormatter.ofPattern ("d-LLL-yy");
  protected static final DateTimeFormatter stf = DateTimeFormatter.ofPattern ("H:mm");
  protected static final String NO_DATE = "<NO DATE>";

  private FileEntryProdos fileEntry;
  private AppleContainer container;

  private ForkProdos data;                                      // for non-forked files
  private List<ForkProdos> forks = new ArrayList<> ();          // for forked files

  public enum ForkType
  {
    DATA, RESOURCE;
  }

  // ---------------------------------------------------------------------------------//
  FileProdos (FsProdos parent, AppleContainer container, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    this.container = container;
    fileEntry = new FileEntryProdos (buffer, ptr);

    fileName = fileEntry.fileName;
    fileType = fileEntry.fileType;
    fileTypeText = ProdosConstants.fileTypes[fileEntry.fileType];
    isForkedFile = fileEntry.storageType == ProdosConstants.GSOS_EXTENDED_FILE;
    isLocked = fileEntry.isLocked;

    if (isForkedFile)
      createForks ();
    else
      data = new ForkProdos (this, null, fileEntry.keyPtr, fileEntry.storageType,
          fileEntry.blocksUsed, fileEntry.eof);
  }

  // ---------------------------------------------------------------------------------//
  private void createForks ()
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = getParentFileSystem ().getBlock (fileEntry.keyPtr).read ();

    for (int ptr = 0; ptr < 512; ptr += 256)
    {
      int storageType = buffer[ptr] & 0x0F;                       // use right nybble!
      int keyPtr = Utility.unsignedShort (buffer, ptr + 1);
      int size = Utility.unsignedShort (buffer, ptr + 3);
      int eof = Utility.unsignedTriple (buffer, ptr + 5);

      if (keyPtr > 0)
        forks.add (new ForkProdos (this, ptr == 0 ? ForkType.DATA : ForkType.RESOURCE,
            keyPtr, storageType, size, eof));
    }
  }

  // ---------------------------------------------------------------------------------//
  public int getAuxType ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntry.auxType;
  }

  // ---------------------------------------------------------------------------------//
  public LocalDateTime getCreated ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntry.created;
  }

  // ---------------------------------------------------------------------------------//
  public LocalDateTime getModified ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntry.modified;
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
    return fileEntry.blocksUsed;                // size of both forks if GSOS extended
  }

  // ---------------------------------------------------------------------------------//
  String getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return container.getPath () + "/" + getFileName ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    LocalDateTime created = getCreated ();
    LocalDateTime modified = getModified ();

    int fileLength = isForkedFile () ? 0 : getFileLength ();

    String dateCreated = created == null ? NO_DATE : created.format (sdf);
    String timeCreated = created == null ? "" : created.format (stf);
    String dateModified = modified == null ? NO_DATE : modified.format (sdf);
    String timeModified = modified == null ? "" : modified.format (stf);

    String forkFlag = isForkedFile () ? "+" : " ";

    text.append (String.format ("%s%-15s %3s%s  %5d  %9s %5s  %9s %5s %8d %7s%n",
        isLocked () ? "*" : " ", getFileName (), getFileTypeText (), forkFlag,
        getTotalBlocks (), dateModified, timeModified, dateCreated, timeCreated,
        fileLength, getSubType ()));

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  private String getSubType ()
  // ---------------------------------------------------------------------------------//
  {
    switch (getFileType ())
    {
      case ProdosConstants.FILE_TYPE_TEXT:
        return String.format ("R=%5d", getAuxType ());

      case ProdosConstants.FILE_TYPE_BINARY:
      case ProdosConstants.FILE_TYPE_PNT:
      case ProdosConstants.FILE_TYPE_PIC:
      case ProdosConstants.FILE_TYPE_FOT:
        return String.format ("A=$%4X", getAuxType ());
    }

    return "";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (fileEntry);

    return Utility.rtrim (text);
  }
}
