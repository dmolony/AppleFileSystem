package com.bytezone.filesystem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FilePascal extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter dtf =
      DateTimeFormatter.ofLocalizedDate (FormatStyle.SHORT);

  private static final String[] fileTypes =
      { "Volume", "Bad ", "Code", "Text", "Info", "Data", "Graf", "Foto", "SecureDir" };

  private int firstBlock;
  private int lastBlock;
  private int bytesUsedInLastBlock;
  private int wildCard;
  private LocalDate date;

  private boolean debug = false;

  // ---------------------------------------------------------------------------------//
  FilePascal (FsPascal fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    firstBlock = Utility.unsignedShort (buffer, ptr);
    lastBlock = Utility.unsignedShort (buffer, ptr + 2);
    fileType = buffer[ptr + 4] & 0xFF;
    fileTypeText = fileTypes[fileType];

    wildCard = buffer[ptr + 5] & 0xFF;

    fileName = Utility.getPascalString (buffer, ptr + 6);
    bytesUsedInLastBlock = Utility.unsignedShort (buffer, ptr + 22);
    date = Utility.getPascalLocalDate (buffer, ptr + 24);           // could return null

    if (debug)
      System.out.printf ("First block: %d, last block: %d%n", firstBlock, lastBlock);

    for (int i = firstBlock; i < lastBlock; i++)
    {
      AppleBlock block = fs.getBlock (i, BlockType.FILE_DATA);
      if (block == null)
        break;                // allow wiz4/5 boot disks

      block.setFileOwner (this);
      dataBlocks.add (block);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return (getTotalBlocks () - 1) * getParentFileSystem ().getBlockSize ()
        + bytesUsedInLastBlock;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    //    return lastBlock - firstBlock;
    return dataBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public DataRecord getDataRecord ()
  // ---------------------------------------------------------------------------------//
  {
    return getDataRecord (getFileLength ());
  }

  // ---------------------------------------------------------------------------------//
  public LocalDate getDate ()
  // ---------------------------------------------------------------------------------//
  {
    return date;
  }

  // ---------------------------------------------------------------------------------//
  public int getFirstBlock ()
  // ---------------------------------------------------------------------------------//
  {
    return firstBlock;
  }

  // ---------------------------------------------------------------------------------//
  public int getLastBlock ()
  // ---------------------------------------------------------------------------------//
  {
    return lastBlock;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%4d   %-15s   %-4s   %8s  %,7d   %4d   %4d", getTotalBlocks (),
        getFileName (), getFileTypeText (), getDate ().format (dtf), getFileLength (),
        getFirstBlock (), getLastBlock ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("First block ........... %d%n", firstBlock));
    text.append (String.format ("Last block ............ %d%n", lastBlock));
    text.append (String.format ("Bytes in last block ... %d%n", bytesUsedInLastBlock));
    text.append (String.format ("Date .................. %s%n", getDate ().format (dtf)));

    return Utility.rtrim (text);
  }
}
