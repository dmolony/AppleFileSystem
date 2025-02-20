package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public abstract class FileDos extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  static final int ENTRY_SIZE = 0x23;
  static final int HEADER_SIZE = 0x0B;

  protected int eof;
  protected int loadAddress;
  protected int textFileGaps;       // total sparse file empty data sectors

  protected List<AppleBlock> indexBlocks = new ArrayList<> ();

  protected CatalogEntryDos catalogEntry;

  // ---------------------------------------------------------------------------------//
  FileDos (FsDos fs)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);
  }

  // ---------------------------------------------------------------------------------//
  protected void setLength ()
  // ---------------------------------------------------------------------------------//
  {
    if (getFileType () == 0x04 || getFileType () == 0x40)          // binary
    {
      if (dataBlocks.size () > 0)
      {
        byte[] buffer = dataBlocks.get (0).getBuffer ();
        loadAddress = Utility.unsignedShort (buffer, 0);
        eof = Utility.unsignedShort (buffer, 2);
      }
    }
    else if (getFileType () == 1 || getFileType () == 2)    // integer basic or applesoft
    {
      if (dataBlocks.size () > 0)
      {
        byte[] buffer = dataBlocks.get (0).getBuffer ();
        eof = Utility.unsignedShort (buffer, 0);
        // could calculate the address from the line numbers
      }
    }
    else
      eof = dataBlocks.size () * getParentFileSystem ().getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                       // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return eof;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                      // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return indexBlocks.size () + dataBlocks.size () - textFileGaps;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    List<AppleBlock> blocks = new ArrayList<AppleBlock> (dataBlocks);
    blocks.addAll (indexBlocks);

    return blocks;
  }

  // ---------------------------------------------------------------------------------//
  public int getLoadAddress ()
  // ---------------------------------------------------------------------------------//
  {
    return loadAddress;
  }

  // ---------------------------------------------------------------------------------//
  AppleBlock getCatalogBlock ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.catalogBlock;
  }

  // ---------------------------------------------------------------------------------//
  int getCatalogSlot ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.slot;
  }

  // ---------------------------------------------------------------------------------//
  public int getSectorCount ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.sectorCount;
  }

  // ---------------------------------------------------------------------------------//
  public int getTotalDataSectors ()
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size () - textFileGaps;
  }

  // ---------------------------------------------------------------------------------//
  public int getTotalIndexSectors ()
  // ---------------------------------------------------------------------------------//
  {
    return indexBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.isLocked;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.fileType;
  }

  // attempt to weed out the catalog entries that are just labels
  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isValidFile ()
  // ---------------------------------------------------------------------------------//
  {
    // Beagle Brothers "applesoft program"
    if (getFileType () == 2 && eof <= 3 && getFileName ().startsWith ("  "))
      return false;

    return catalogEntry.isNameValid && dataBlocks.size () > 0;
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

    AppleBlock catalogEntryBlock = catalogEntry.catalogBlock;

    text.append (String.format ("Locked ................ %s%n", isLocked ()));
    text.append (String.format ("Catalog sector ........ %02X / %02X%n",
        catalogEntryBlock.getTrackNo (), catalogEntryBlock.getSectorNo ()));
    text.append (String.format ("Catalog entry ......... %d%n", catalogEntry.slot));
    text.append (String.format ("Sectors ............... %04X  %<,5d%n",
        catalogEntry.sectorCount));
    text.append (String.format ("File length ........... %04X  %<,5d%n", eof));
    text.append (String.format ("Load address .......... %04X  %<,5d%n", loadAddress));
    text.append (String.format ("Text file gaps ........ %04X  %<,5d%n", textFileGaps));

    return Utility.rtrim (text);
  }
}
