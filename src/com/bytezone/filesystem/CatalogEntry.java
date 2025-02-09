package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public abstract class CatalogEntry
// -----------------------------------------------------------------------------------//
{
  protected final int slot;
  protected final byte[] buffer;

  // ---------------------------------------------------------------------------------//
  public CatalogEntry (byte[] buffer, int slot)
  // ---------------------------------------------------------------------------------//
  {
    this.buffer = buffer;
    this.slot = slot;
  }

  abstract void readCatalogEntry ();

  abstract void writeCatalogEntry ();
}
