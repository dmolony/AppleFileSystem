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
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return null;
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public String getCatalogLine ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return String.format ("%02X  %-8s %-3s  %,5d  %,5d  %04X  %3d", status, fileName,
  //        extension, firstBlock, totalBlocks, crc, pad);
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (
        String.format ("File type ............. %d  %s%n", fileType, fileTypeText));
    text.append (String.format ("Extension ............. %s%n", extension));
    text.append (String.format ("First block ........... %,d%n", firstBlock));
    text.append (String.format ("Total blocks .......... %,d%n", totalBlocks));
    text.append (String.format ("CRC ................... %,d%n", crc));

    return text.toString ();
  }
}
