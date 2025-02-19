package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public abstract class FileDos extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  protected int sectorCount;
  protected int eof;
  protected int loadAddress;
  protected int textFileGaps;       // total sparse file empty data sectors

  protected List<AppleBlock> indexBlocks = new ArrayList<> ();
  protected boolean isNameValid;

  protected AppleBlock catalogEntryBlock;
  protected int catalogEntryIndex;

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
    if (getFileType () == 4 || getFileType () == 0x40)          // binary
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
  public int getSectorCount ()
  // ---------------------------------------------------------------------------------//
  {
    return sectorCount;
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

  // attempt to weed out the catalog entries that are just labels
  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isValidFile ()
  // ---------------------------------------------------------------------------------//
  {
    // Beagle Brothers "applesoft program"
    if (getFileType () == 2 && eof <= 3 && getFileName ().startsWith ("  "))
      return false;

    return isNameValid && dataBlocks.size () > 0;
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

    text.append (String.format ("Locked ................ %s%n", isLocked ()));
    text.append (String.format ("Catalog sector ........ %02X / %02X%n",
        catalogEntryBlock.getTrackNo (), catalogEntryBlock.getSectorNo ()));
    text.append (String.format ("Catalog entry ......... %d%n", catalogEntryIndex));
    text.append (String.format ("Sectors ............... %04X  %<,5d%n", sectorCount));
    text.append (String.format ("File length ........... %04X  %<,5d%n", eof));
    text.append (String.format ("Load address .......... %04X  %<,5d%n", loadAddress));
    text.append (String.format ("Text file gaps ........ %04X  %<,5d%n", textFileGaps));

    return Utility.rtrim (text);
  }
}
