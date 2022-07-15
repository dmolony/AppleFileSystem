package com.bytezone.prodos.write;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class VolumeDirectoryHeader extends DirectoryHeader
// -----------------------------------------------------------------------------------//
{
  int bitMapPointer = 0x06;
  int totalBlocks;

  // ---------------------------------------------------------------------------------//
  public VolumeDirectoryHeader (ProdosDisk disk, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (disk, ptr);

    storageType = (byte) 0x0F;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void read ()
  // ---------------------------------------------------------------------------------//
  {
    super.read ();

    bitMapPointer = Utility.unsignedShort (buffer, ptr + 0x23);
    totalBlocks = Utility.unsignedShort (buffer, ptr + 0x25);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void write ()
  // ---------------------------------------------------------------------------------//
  {
    super.write ();

    Utility.writeShort (buffer, ptr + 0x23, bitMapPointer);
    Utility.writeShort (buffer, ptr + 0x25, totalBlocks);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  String toText ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-29s %04X %04X", super.toText (), totalBlocks, bitMapPointer);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (UNDERLINE);
    text.append ("Volume Directory Header\n");
    text.append (UNDERLINE);
    text.append (super.toString ());
    text.append (String.format ("Bitmap pointer ... %02X%n", bitMapPointer));
    text.append (String.format ("Total blocks ..... %d%n", totalBlocks));
    text.append (UNDERLINE);

    return text.toString ();
  }
}
