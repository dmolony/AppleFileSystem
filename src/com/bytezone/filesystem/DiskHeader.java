package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

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

    formatText (text, "Header type", diskHeaderType.toString ());

    return text.toString ();
  }
}
