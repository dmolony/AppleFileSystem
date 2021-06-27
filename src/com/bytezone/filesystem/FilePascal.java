package com.bytezone.filesystem;

import java.time.LocalDate;

// -----------------------------------------------------------------------------------//
public class FilePascal extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  int firstBlock;
  int lastBlock;
  int bytesUsedInLastBlock;
  int fileType;
  int wildCard;
  LocalDate date;

  // ---------------------------------------------------------------------------------//
  FilePascal (FsPascal fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFile = true;

    firstBlock = AbstractFileSystem.unsignedShort (buffer, ptr);
    lastBlock = AbstractFileSystem.unsignedShort (buffer, ptr + 2);
    fileType = buffer[ptr + 4] & 0xFF;
    wildCard = buffer[ptr + 5] & 0xFF;

    int nameLength = buffer[ptr + 6] & 0xFF;
    name = AbstractFileSystem.string (buffer, ptr + 7, nameLength);

    bytesUsedInLastBlock = AbstractFileSystem.unsignedShort (buffer, ptr + 22);
    date = AbstractFileSystem.getPascalDate (buffer, ptr + 24);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-20s  %3d  %3d  %3d  %s", name, firstBlock, lastBlock,
        bytesUsedInLastBlock, date);
  }
}
