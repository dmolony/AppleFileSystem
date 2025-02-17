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

  protected String fileName = "";
  protected String blockList = "";

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

    for (int i = 0; i < catalogBlocks.size (); i++)
      blockList += String.format ("%02X ", catalogBlocks.get (i).getBlockNo ());
  }

  abstract void read ();

  abstract void write ();

  abstract void delete ();

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("---- Catalog entry ----\n");

    if (catalogBlock != null)
      text.append (String.format ("Catalog block ......... %04X    %<,7d%n",
          catalogBlock.getBlockNo ()));
    else
      text.append (String.format ("Catalog blocks ........ %s   %n", blockList));

    text.append (String.format ("Catalog slot .......... %d%n", slot));
    text.append (String.format ("File name ............. %s%n%n", fileName));

    return text.toString ();
  }
}
