package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileProdos extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private FileEntryProdos fileEntry;

  private ForkProdos data;                                      // for non-forked files
  private List<ForkProdos> forks = new ArrayList<> ();          // for forked files

  public enum ForkType
  {
    DATA, RESOURCE;
  }

  // ---------------------------------------------------------------------------------//
  FileProdos (FsProdos parent, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    fileEntry = new FileEntryProdos (buffer, ptr);

    fileName = fileEntry.fileName;
    fileType = fileEntry.fileType;
    fileTypeText = ProdosConstants.fileTypes[fileEntry.fileType];
    isForkedFile = fileEntry.storageType == FsProdos.GSOS_EXTENDED_FILE;
    isLocked = fileEntry.isLocked;

    if (isForkedFile)
      createForks ();
    else
      data = new ForkProdos (this, null, fileEntry.keyPtr, fileEntry.storageType,
          fileEntry.size, fileEntry.eof);
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
    return fileEntry.size;                    // size of both forks if GSOS extended
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
