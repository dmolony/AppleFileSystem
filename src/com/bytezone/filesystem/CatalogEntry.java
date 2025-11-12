package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import java.util.List;

// Used by Dos, Prodos and Pascal
// -----------------------------------------------------------------------------------//
public abstract class CatalogEntry
// -----------------------------------------------------------------------------------//
{
  protected int slot;
  protected byte[] buffer;

  protected AppleBlock catalogBlock;            // most filesystems are self-contained
  protected List<AppleBlock> catalogBlocks;     // pascal uses 4 contiguous blocks

  protected String fileName = "";
  protected boolean isFileNameValid = true;
  protected String blockList = "";

  // ---------------------------------------------------------------------------------//
  public CatalogEntry (AppleBlock catalogBlock, int slot)
  // ---------------------------------------------------------------------------------//
  {
    this.catalogBlock = catalogBlock;
    this.slot = slot;

    this.buffer = catalogBlock.getBuffer ();
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

  // ---------------------------------------------------------------------------------//
  public AppleBlock getCatalogBlock ()
  // ---------------------------------------------------------------------------------//
  {
    assert catalogBlock != null;
    return catalogBlock;
  }

  // ---------------------------------------------------------------------------------//
  public List<AppleBlock> getCatalogBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    assert catalogBlocks != null;
    return catalogBlocks;
  }

  // ---------------------------------------------------------------------------------//
  public int getCatalogSlot ()
  // ---------------------------------------------------------------------------------//
  {
    return slot;
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
      formatText (text, "Catalog block", 4, catalogBlock.getBlockNo ());
    else
      formatText (text, "Catalog blocks", blockList);

    formatText (text, "Catalog slot", 2, slot);
    formatText (text, "File name", fileName);

    return text.toString ();
  }
}
