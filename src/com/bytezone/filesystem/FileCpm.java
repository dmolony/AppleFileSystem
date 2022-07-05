package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------------//
public class FileCpm extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  private final int userNumber;
  private final String name;
  private final String type;

  private final boolean readOnly;
  private final boolean systemFile;

  private final int extentCounterLo;
  private final int extentCounterHi;
  private final int reserved;
  private int recordCount;                      // records used in this extent

  List<AppleBlock> dataBlocks = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  FileCpm (FsCpm fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFile = true;

    // hi-bits of type are used for flags
    readOnly = (buffer[ptr + 9] & 0x80) != 0;
    systemFile = (buffer[ptr + 10] & 0x80) != 0;

    byte[] typeBuffer = new byte[3];
    typeBuffer[0] = (byte) (buffer[ptr + 9] & 0x7F);
    typeBuffer[1] = (byte) (buffer[ptr + 10] & 0x7F);
    typeBuffer[2] = (byte) (buffer[ptr + 11] & 0x7F);
    type = new String (typeBuffer).trim ();

    userNumber = buffer[ptr] & 0xFF;
    name = new String (buffer, ptr + 1, 8).trim ();

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

      if ((b & 0x80) != 0)
        System.out.println ("CPM hi bit set");

      int blockNumber = ((b & 0x80) == 0) ? (b + 12) : (b & 0x7F);
      dataBlocks.add (fileSystem.getBlock (blockNumber));
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
    return fileSystem.readBlocks (dataBlocks);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size () * fileSystem.getBlockSize ();
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
    String fullName = type.isEmpty () ? name : name + "." + type;

    return String.format ("%-12s  %s  %s  %2d %3d  %3d", fullName, readOnly ? "*" : " ",
        systemFile ? "*" : " ", userNumber, dataBlocks.size (), recordCount);
  }
}
