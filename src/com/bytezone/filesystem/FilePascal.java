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
  private static final int CATALOG_ENTRY_SIZE = 26;

  private static final String[] fileTypes =
      { "Volume", "Bad ", "Code", "Text", "Info", "Data", "Graf", "Foto", "SecureDir" };

  //  private int firstBlock;
  //  private int lastBlock;
  //  private int bytesUsedInLastBlock;
  //  private int wildCard;
  //  private LocalDate date;
  //  private int catalogPtr;
  CatalogEntryPascal catalogEntry;
  int slot;

  private boolean debug = false;

  // ---------------------------------------------------------------------------------//
  FilePascal (FsPascal fs, CatalogEntryPascal catalogEntry, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    //    this.catalogPtr = ptr;          // for possible updating
    this.catalogEntry = catalogEntry;
    this.slot = slot;

    //    firstBlock = Utility.unsignedShort (buffer, ptr);
    //    lastBlock = Utility.unsignedShort (buffer, ptr + 2);

    fileType = catalogEntry.fileType;
    fileTypeText = fileTypes[fileType];

    //    wildCard = buffer[ptr + 5] & 0xFF;

    fileName = catalogEntry.fileName;
    //    bytesUsedInLastBlock = Utility.unsignedShort (buffer, ptr + 22);
    //    date = Utility.getPascalLocalDate (buffer, ptr + 24);           // could return null

    //    if (debug)
    //      System.out.printf ("First block: %d, last block: %d%n", firstBlock, lastBlock);

    for (int i = catalogEntry.firstBlock; i < catalogEntry.lastBlock; i++)
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
        + catalogEntry.bytesUsedInLastBlock;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
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
  public Buffer getFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return getFileBuffer (getFileLength ());
  }

  // ---------------------------------------------------------------------------------//
  public LocalDate getDate ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.fileDate;
  }

  // ---------------------------------------------------------------------------------//
  public int getFirstBlock ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.firstBlock;
  }

  // ---------------------------------------------------------------------------------//
  public int getLastBlock ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.lastBlock;
  }

  // ---------------------------------------------------------------------------------//
  void setDeleted (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    int catalogPtr = slot * CATALOG_ENTRY_SIZE;

    Utility.writeShort (buffer, catalogPtr, 0);           // first block
    Utility.writeShort (buffer, catalogPtr + 2, 0);       // last block
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

    text.append (String.format ("First block ........... %d%n", catalogEntry.firstBlock));
    text.append (String.format ("Last block ............ %d%n", catalogEntry.lastBlock));
    text.append (String.format ("Bytes in last block ... %d%n",
        catalogEntry.bytesUsedInLastBlock));
    text.append (String.format ("Date .................. %s%n", getDate ().format (dtf)));

    return Utility.rtrim (text);
  }
}
