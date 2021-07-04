package com.bytezone.filesystem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------------//
public class FilePascal extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  private int firstBlock;
  private int lastBlock;
  private int bytesUsedInLastBlock;
  private int fileType;
  private int wildCard;
  private LocalDate date;

  List<AppleBlock> dataBlocks = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  FilePascal (FsPascal fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFile = true;

    firstBlock = Utility.unsignedShort (buffer, ptr);
    lastBlock = Utility.unsignedShort (buffer, ptr + 2);
    fileType = buffer[ptr + 4] & 0xFF;
    wildCard = buffer[ptr + 5] & 0xFF;

    int nameLength = buffer[ptr + 6] & 0xFF;
    name = Utility.string (buffer, ptr + 7, nameLength);

    bytesUsedInLastBlock = Utility.unsignedShort (buffer, ptr + 22);
    date = Utility.getPascalDate (buffer, ptr + 24);

    for (int i = firstBlock; i < lastBlock; i++)
      dataBlocks.add (fs.getBlock (i));
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystem.readBlocks (dataBlocks);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return (dataBlocks.size () - 1) * fileSystem.getBlockSize () + bytesUsedInLastBlock;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-20s  %3d  %03X-%03X  %3d  %s", name, fileType, firstBlock,
        lastBlock, bytesUsedInLastBlock, date);
  }
}
