package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsCpm extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final int EMPTY_BYTE_VALUE = 0xE5;
  private List<FileEntryCpm> fileEntries = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  public FsCpm (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.CPM);

    assert getTotalCatalogBlocks () == 0;

    int catalogBlocks = 0;

    int firstBlock = 0;
    int maxBlocks = 0;
    int size = 0;

    if (getDataRecord ().length () == 143_360)
    {
      firstBlock = 12;        // track 3 x (4 blocks per track)
      maxBlocks = 2;          // 2 blocks (half a track)
      size = 8;               // 8 bits per block address
    }
    else if (getDataRecord ().length () == 819_200)
    {
      firstBlock = 16;        // track 4 x (4 blocks per track)
      maxBlocks = 8;          // 8 blocks (2 full tracks)
      size = 16;              // 16 bits per block address
    }

    OUT: for (int i = 0; i < maxBlocks; i++)
    {
      AppleBlock block = getBlock (firstBlock + i, BlockType.FS_DATA);
      block.setBlockSubType ("CATALOG");
      byte[] buffer = block.read ();

      for (int j = 0; j < buffer.length; j += 32)
      {
        int b1 = buffer[j] & 0xFF;          // user number
        if (b1 == EMPTY_BYTE_VALUE)         // deleted file??
          continue;

        if (b1 > 31)
          //          throw new FileFormatException ("CPM: bad user number: " + b1);
          break OUT;

        int b2 = buffer[j + 1] & 0xFF;      // first letter of filename
        if (b2 <= 32 || (b2 > 126 && b2 != EMPTY_BYTE_VALUE))
          //          throw new FileFormatException ("CPM: bad name value");
          break OUT;

        fileEntries.add (new FileEntryCpm (buffer, j, size));
      }

      ++catalogBlocks;
    }

    setTotalCatalogBlocks (catalogBlocks);
    if (catalogBlocks == 0)
      return;

    // create files

    List<FileEntryCpm> shortList = new ArrayList<> ();

    for (FileEntryCpm fileEntryCpm : fileEntries)
    {
      //      System.out.println (fileEntryCpm);
      //      System.out.println ();

      if (fileEntryCpm.getExtentNo () == 0 && shortList.size () > 0)
      {
        files.add (new FileCpm (this, shortList));
        shortList = new ArrayList<> ();
      }
      shortList.add (fileEntryCpm);
    }

    files.add (new FileCpm (this, shortList));

    // flag DOS sectors
    for (int blockNo = 0; blockNo < 12; blockNo++)
    {
      AppleBlock block = getBlock (blockNo);
      if (block.getBlockType () == BlockType.ORPHAN)
      {
        block.setBlockType (BlockType.FS_DATA);
        block.setBlockSubType ("DOS");
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    String line = "----  ---------  ---  - - -  --  --  --  --   "
        + "-----------------------------------------------\n";

    text.append ("User  Name       Typ  R S A  Eh  El  RC  BC   Blocks\n");
    text.append (line);

    for (AppleFile file : getFiles ())
    {
      text.append (file.getCatalogLine ());
      text.append ("\n");
    }

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("----- CPM Header ------\n");
    text.append (String.format ("Entry length .......... %d%n", 32));
    text.append (String.format ("Entries per block ..... %d%n", getBlockSize () / 32));
    text.append (String.format ("File count ............ %d", getFiles ().size ()));

    return Utility.rtrim (text);
  }
}
