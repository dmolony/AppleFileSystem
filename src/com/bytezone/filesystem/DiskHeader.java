package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public abstract class DiskHeader
// -----------------------------------------------------------------------------------//
{
  BlockReader blockReader;
  DiskHeaderType diskHeaderType;

  enum DiskHeaderType
  {
    TWO_IMG, DISK_COPY
  }

  // ---------------------------------------------------------------------------------//
  public DiskHeader (BlockReader blockReader, DiskHeaderType diskHeaderType)
  // ---------------------------------------------------------------------------------//
  {
    this.blockReader = blockReader;
    this.diskHeaderType = diskHeaderType;
  }

  abstract BlockReader getBlockReader ();

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    Utility.formatMeta (text, "Header type", diskHeaderType.toString ());

    return text.toString ();
  }
}
