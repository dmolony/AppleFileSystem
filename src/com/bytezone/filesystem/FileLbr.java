package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileLbr extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  int status;
  String extension;
  int firstBlock;
  int totalBlocks;
  int crc;
  int pad;

  // ---------------------------------------------------------------------------------//
  FileLbr (FsLbr fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFile = true;

    status = buffer[ptr] & 0xFF;
    fileName = new String (buffer, ptr + 1, 8);
    extension = new String (buffer, ptr + 9, 3);
    firstBlock = Utility.unsignedShort (buffer, ptr + 12);
    totalBlocks = Utility.unsignedShort (buffer, ptr + 14);
    crc = Utility.unsignedShort (buffer, ptr + 16);
    pad = Utility.unsignedShort (buffer, ptr + 26);

    fileTypeText = extension;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("Status ................ %d%n", status));
    text.append (String.format ("Extension ............. %s%n", extension));
    text.append (String.format ("First block ........... %,d%n", firstBlock));
    text.append (String.format ("Total blocks .......... %,d%n", totalBlocks));
    text.append (String.format ("CRC ................... %,d%n", crc));

    return Utility.rtrim (text);
  }
}
