package com.bytezone.filesystem;

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

    text.append (String.format ("Header type ........... %s%n", diskHeaderType));

    return text.toString ();
  }
}
