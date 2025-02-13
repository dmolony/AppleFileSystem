package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
public abstract class CatalogEntry
// -----------------------------------------------------------------------------------//
{
  protected int slot;
  protected byte[] buffer;

  protected AppleBlock catalogBlock;            // most filesystems are self-contained
  protected List<AppleBlock> catalogBlocks;     // pascal uses 4 contiguous blocks

  // ---------------------------------------------------------------------------------//
  public CatalogEntry (AppleBlock catalogBlock, int slot)
  // ---------------------------------------------------------------------------------//
  {
    this.catalogBlock = catalogBlock;
    this.buffer = catalogBlock.getBuffer ();
    this.slot = slot;
  }

  // ---------------------------------------------------------------------------------//
  public CatalogEntry (List<AppleBlock> catalogBlocks, byte[] buffer, int slot)
  // ---------------------------------------------------------------------------------//
  {
    this.catalogBlocks = catalogBlocks;
    this.buffer = buffer;
    this.slot = slot;
  }

  abstract void read ();

  abstract void write ();

  abstract void delete ();
}
