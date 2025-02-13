package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public abstract class CatalogEntryDos extends CatalogEntry
// -----------------------------------------------------------------------------------//
{
  static final int ENTRY_SIZE = 0x23;
  static final int HEADER_SIZE = 0x0B;

  int fileType;
  boolean isLocked;
  String fileName;
  int sectorCount;
  boolean isNameValid;

  // ---------------------------------------------------------------------------------//
  public CatalogEntryDos (AppleBlock catalogBlock, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (catalogBlock, slot);
  }

  // ---------------------------------------------------------------------------------//
  protected boolean checkName (String name)
  // ---------------------------------------------------------------------------------//
  {
    for (byte b : name.getBytes ())
      if (b == (byte) 0x88)
        return false;

    return true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("Locked ................ %s%n", isLocked));
    //    text.append (String.format ("Catalog sector ........ %02X / %02X%n",
    //        catalogEntryBlock.getTrackNo (), catalogEntryBlock.getSectorNo ()));
    //    text.append (String.format ("Catalog entry ......... %d%n", catalogEntryIndex));
    text.append (String.format ("Sectors ............... %04X  %<,5d%n", sectorCount));
    //    text.append (String.format ("File length ........... %04X  %<,5d%n", eof));
    //    text.append (String.format ("Load address .......... %04X  %<,5d%n", loadAddress));
    //    text.append (String.format ("Text file gaps ........ %04X  %<,5d%n", textFileGaps));

    return Utility.rtrim (text);
  }
}
