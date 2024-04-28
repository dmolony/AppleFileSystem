package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public abstract class FileDos extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  protected int sectorCount;
  protected int length;
  protected int address;
  protected int textFileGaps;

  protected List<AppleBlock> indexBlocks = new ArrayList<> ();

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
    if (fileType == 4)                            // binary
    {
      if (dataBlocks.size () > 0)
      {
        byte[] fileBuffer = getParentFileSystem ().readBlock (dataBlocks.get (0));
        address = Utility.unsignedShort (fileBuffer, 0);
        length = Utility.unsignedShort (fileBuffer, 2);
      }
    }
    else if (fileType == 1 || fileType == 2)      // integer basic or applesoft
    {
      if (dataBlocks.size () > 0)
      {
        byte[] fileBuffer = getParentFileSystem ().readBlock (dataBlocks.get (0));
        length = Utility.unsignedShort (fileBuffer, 0);
        // could calculate the address from the line numbers
      }
    }
    else
      length = dataBlocks.size () * getParentFileSystem ().getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                       // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return length;
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
  public int getAddress ()
  // ---------------------------------------------------------------------------------//
  {
    return address;
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
}
