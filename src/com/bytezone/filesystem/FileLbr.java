package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileLbr extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  int status;
  String name;
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
    name = new String (buffer, ptr + 1, 8);
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
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%02X  %-8s %-3s  %,5d  %,5d  %04X  %3d", status, name, extension,
        firstBlock, totalBlocks, crc, pad);
  }
}
