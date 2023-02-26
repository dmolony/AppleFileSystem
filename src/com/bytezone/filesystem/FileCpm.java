package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------------//
public class FileCpm extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private final int userNumber;

  private final boolean readOnly;
  private final boolean systemFile;

  private final int extentCounterLo;
  private final int extentCounterHi;
  private final int reserved;
  private int recordCount;                      // records used in this extent

  List<AppleBlock> dataBlocks = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  FileCpm (FsCpm parent, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    isFile = true;

    // hi-bits of type are used for flags
    readOnly = (buffer[ptr + 9] & 0x80) != 0;
    systemFile = (buffer[ptr + 10] & 0x80) != 0;

    byte[] typeBuffer = new byte[3];
    typeBuffer[0] = (byte) (buffer[ptr + 9] & 0x7F);
    typeBuffer[1] = (byte) (buffer[ptr + 10] & 0x7F);
    typeBuffer[2] = (byte) (buffer[ptr + 11] & 0x7F);
    fileTypeText = new String (typeBuffer).trim ();

    userNumber = buffer[ptr] & 0xFF;
    fileName = new String (buffer, ptr + 1, 8).trim ();

    extentCounterLo = buffer[ptr + 12] & 0xFF;
    reserved = buffer[ptr + 13] & 0xFF;
    extentCounterHi = buffer[ptr + 14] & 0xFF;

    append (buffer, ptr);
  }

  // ---------------------------------------------------------------------------------//
  void append (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    recordCount = buffer[ptr + 15] & 0xFF;

    ptr += 16;
    for (int i = 0; i < 16; i++)
    {
      byte b = buffer[ptr + i];
      if (b == 0)
        break;

      if (false)
        if ((b & 0x80) != 0)
          System.out.printf ("%s CPM hi bit set%n", getFileName ());

      int blockNumber = ((b & 0x80) == 0) ? (b + 12) : (b & 0x7F);
      dataBlocks.add (getFileSystem ().getBlock (blockNumber));
    }
  }

  // ---------------------------------------------------------------------------------//
  boolean isComplete ()
  // ---------------------------------------------------------------------------------//
  {
    return (recordCount & 0x80) == 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFileSystem.readBlocks (dataBlocks);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size () * getFileSystem ().getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileTypeText.isEmpty () ? fileName : fileName + "." + fileTypeText;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-12s  %s  %s  %2d %3d  %3d", getFileName (),
        readOnly ? "*" : " ", systemFile ? "*" : " ", userNumber, dataBlocks.size (),
        recordCount);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (String.format ("File type ............. %s%n", fileTypeText));
    text.append (String.format ("Read only ............. %s%n", readOnly));
    text.append (String.format ("System file ........... %s%n", systemFile));
    text.append (String.format ("User number ........... %,d%n", userNumber));
    text.append (String.format ("Extent lo ............. %,d%n", extentCounterLo));
    text.append (String.format ("Extent hi ............. %,d%n", extentCounterHi));
    text.append (String.format ("Data blocks ........... %,d%n", dataBlocks.size ()));
    text.append (String.format ("Records ............... %,d", recordCount));

    return text.toString ();
  }
}
