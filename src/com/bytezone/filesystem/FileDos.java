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
  protected void setFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    if (dataBlocks.size () == 0)
    {
      eof = 0;
      return;
    }

    // NB - don't get the buffer for text files - a sparse text file may NPE

    switch (getFileType ())
    {
      case FsDos.FILE_TYPE_TEXT:
        eof = dataBlocks.size () * getParentFileSystem ().getBlockSize ();
        break;

      case FsDos.FILE_TYPE_INTEGER_BASIC:
        byte[] buffer = dataBlocks.get (0).getBuffer ();
        eof = Utility.unsignedShort (buffer, 0);
        break;

      case FsDos.FILE_TYPE_APPLESOFT:
        buffer = dataBlocks.get (0).getBuffer ();
        eof = Utility.unsignedShort (buffer, 0);
        if (eof > 6)
        {
          loadAddress = Utility.getApplesoftLoadAddress (buffer);
          if (loadAddress == 0x801)           // don't display the default
            loadAddress = 0;
        }
        break;

      case FsDos.FILE_TYPE_BINARY:
      case FsDos.FILE_TYPE_R:                 // Dos Toolkit APA and HRCG
        //      case FsDos.FILE_TYPE_S:                 // fuck nose
      case FsDos.FILE_TYPE_BINARY_B:
      case FsDos.FILE_TYPE_BINARY_L:          // Dos4 uses this
        buffer = dataBlocks.get (0).getBuffer ();
        loadAddress = Utility.unsignedShort (buffer, 0);
        eof = Utility.unsignedShort (buffer, 2);
        break;

      default:
        System.out.println ("Unexpected file type: " + getFileType ());
    }
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
  public List<AppleBlock> getAllBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    List<AppleBlock> blocks = new ArrayList<AppleBlock> (dataBlocks);
    blocks.addAll (indexBlocks);
    blocks.add (catalogEntry.catalogBlock);

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
  public int getTextFileGaps ()
  // ---------------------------------------------------------------------------------//
  {
    return textFileGaps;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void delete (boolean force)
  // ---------------------------------------------------------------------------------//
  {
    if (isLocked () && !force)
      throw new FileLockedException (String.format ("%s is locked", getFileName ()));

    catalogEntry.delete ();
    ((FsDos) parentFileSystem).remove (this, force);
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
    text.append (String.format ("Locked ................ %s%n", catalogEntry.isLocked));
    text.append (String.format ("Catalog slot .......... %d%n", catalogEntry.slot));
    text.append (String.format ("Sectors ............... %04X    %<,9d%n",
        catalogEntry.sectorCount));
    text.append (String.format ("File length ........... %04X    %<,9d%n", eof));
    text.append (String.format ("Load address .......... %04X    %<,9d%n", loadAddress));
    text.append (String.format ("Text file gaps ........ %04X    %<,9d%n", textFileGaps));

    return Utility.rtrim (text);
  }
}
