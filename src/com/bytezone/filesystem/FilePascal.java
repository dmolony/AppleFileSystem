package com.bytezone.filesystem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

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

  CatalogEntryPascal catalogEntry;

  // ---------------------------------------------------------------------------------//
  FilePascal (FsPascal fs, CatalogEntryPascal catalogEntry)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    this.catalogEntry = catalogEntry;

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
  //  @Override
  //  public List<AppleBlock> getDataBlocks ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return dataBlocks;
  //  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public Buffer getFileBuffer ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return getFileBuffer (getFileLength ());
  //  }

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
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.fileType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return fileTypes[catalogEntry.fileType];
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return false;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void delete (boolean force)
  // ---------------------------------------------------------------------------------//
  {
    catalogEntry.delete ();
    ((FsPascal) parentFileSystem).remove (this);
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

    text.append ("\n");
    text.append (catalogEntry);

    return Utility.rtrim (text);
  }
}
