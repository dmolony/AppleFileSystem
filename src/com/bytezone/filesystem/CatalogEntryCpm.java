package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class CatalogEntryCpm implements Iterable<Integer>
// -----------------------------------------------------------------------------------//
{
  private final int userNumber;

  private final boolean readOnly;
  private final boolean systemFile;
  private final boolean archived;

  private final int extentCounterLo;
  private final int extentCounterHi;
  private final int byteCount;
  private final int recordCount;                      // records used in this extent

  private final String fileName;
  private final String fileTypeText;

  private final List<Integer> logicalBlocks = new ArrayList<> ();
  private final List<Integer> blocks = new ArrayList<> ();
  private final int extentNo;

  // ---------------------------------------------------------------------------------//
  CatalogEntryCpm (byte[] buffer, int ptr, int size)
  // ---------------------------------------------------------------------------------//
  {
    userNumber = buffer[ptr] & 0xFF;
    fileName = new String (buffer, ptr + 1, 8).trim ();

    // hi-bits of type are used for flags
    readOnly = (buffer[ptr + 9] & 0x80) != 0;
    systemFile = (buffer[ptr + 10] & 0x80) != 0;
    archived = (buffer[ptr + 11] & 0x80) != 0;

    byte[] typeBuffer = new byte[3];
    typeBuffer[0] = (byte) (buffer[ptr + 9] & 0x7F);
    typeBuffer[1] = (byte) (buffer[ptr + 10] & 0x7F);
    typeBuffer[2] = (byte) (buffer[ptr + 11] & 0x7F);

    String type = new String (typeBuffer);
    fileTypeText = type.isBlank () ? "   " : type;

    extentCounterLo = buffer[ptr + 12] & 0xFF;
    byteCount = buffer[ptr + 13] & 0xFF;
    extentCounterHi = buffer[ptr + 14] & 0xFF;
    recordCount = buffer[ptr + 15] & 0xFF;

    extentNo = (extentCounterLo & 0x1F) | ((extentCounterHi & 0x3F) << 5);

    if (size == 8)
      addDataBlocks8 (buffer, ptr + 16);
    else
      addDataBlocks16 (buffer, ptr + 16);
  }

  // ---------------------------------------------------------------------------------//
  void addDataBlocks8 (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    for (int i = 0; i < 16; i++)
    {
      byte b = buffer[ptr++];
      if (b == 0)
        break;

      logicalBlocks.add (b & 0xFF);

      // this allows blocks 0-12 to be used for file data
      int blockNo = ((b & 0x80) == 0) ? (b + 12) : (b & 0x7F);

      blocks.add (blockNo);
    }
  }

  // ---------------------------------------------------------------------------------//
  void addDataBlocks16 (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    for (int i = 0; i < 8; i++)
    {
      int blockNo = Utility.unsignedShort (buffer, ptr);
      if (blockNo == 0)
        break;

      logicalBlocks.add (blockNo);
      blocks.add (blockNo + 20);

      ptr += 2;
    }
  }

  // ---------------------------------------------------------------------------------//
  public int getUserNumber ()
  // ---------------------------------------------------------------------------------//
  {
    return userNumber;
  }

  // ---------------------------------------------------------------------------------//
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileName;
  }

  // ---------------------------------------------------------------------------------//
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return fileTypeText;
  }

  // ---------------------------------------------------------------------------------//
  public int getExtentNo ()
  // ---------------------------------------------------------------------------------//
  {
    return extentNo;
  }

  // ---------------------------------------------------------------------------------//
  public boolean isReadOnly ()
  // ---------------------------------------------------------------------------------//
  {
    return readOnly;
  }

  // ---------------------------------------------------------------------------------//
  public boolean isSystemFile ()
  // ---------------------------------------------------------------------------------//
  {
    return systemFile;
  }

  // ---------------------------------------------------------------------------------//
  public boolean isArchived ()
  // ---------------------------------------------------------------------------------//
  {
    return archived;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Iterator<Integer> iterator ()
  // ---------------------------------------------------------------------------------//
  {
    return blocks.iterator ();
  }

  // ---------------------------------------------------------------------------------//
  public String getLine ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    for (int b : logicalBlocks)
      text.append (String.format (" %02X", b));

    return String.format (" %2d   %-8s   %-3s  %s %s %s  %02X  %02X  %02X  %02X  %s",
        userNumber, fileName, fileTypeText, readOnly ? "*" : " ", systemFile ? "*" : " ",
        archived ? "*" : " ", extentCounterHi, extentCounterLo, recordCount, byteCount,
        text.toString ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("------ Catalog entry -------");
    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (String.format ("File extension ........ %s%n", fileTypeText));
    text.append (String.format ("Read only ............. %s%n", readOnly));
    text.append (String.format ("System file ........... %s%n", systemFile));
    text.append (String.format ("Archived .............. %s%n", archived));
    text.append (String.format ("User number ........... %,d%n", userNumber));
    text.append (String.format ("Extent lo ............. %02X%n", extentCounterLo));
    text.append (String.format ("Extent hi ............. %02X%n", extentCounterHi));
    text.append (String.format ("Extent no ............. %,d%n", extentNo));
    text.append (String.format ("Data blocks ........... %s%n", blocks));
    text.append (String.format ("Record count .......... %,d%n", recordCount));
    text.append (String.format ("Byte count ............ %,d", byteCount));

    return Utility.rtrim (text);
  }
}
