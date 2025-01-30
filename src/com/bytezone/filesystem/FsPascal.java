package com.bytezone.filesystem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.filesystem.BlockReader.AddressType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsPascal extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter dtf =
      DateTimeFormatter.ofLocalizedDate (FormatStyle.SHORT);

  private static final int CATALOG_ENTRY_SIZE = 26;

  private String volumeName;
  private int firstCatalogBlock;
  private int firstFileBlock;
  private int entryType;
  private int totalBlocks;         // size of disk
  private int totalFiles;          // no of files on disk
  private int firstBlock;
  private LocalDate date;
  private List<AppleBlock> catalogBlocks = new ArrayList<> ();

  private boolean debug = false;

  // ---------------------------------------------------------------------------------//
  public FsPascal (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.PASCAL);

    AppleBlock vtoc = getBlock (2, BlockType.FS_DATA);
    vtoc.setBlockSubType ("CATALOG");
    byte[] buffer = vtoc.getBuffer ();

    firstCatalogBlock = Utility.unsignedShort (buffer, 0);
    firstFileBlock = Utility.unsignedShort (buffer, 2);
    if (firstCatalogBlock != 0 || firstFileBlock != 6)
      throw new FileFormatException (
          String.format ("Pascal: from: %d, to: %d", firstCatalogBlock, firstFileBlock));

    entryType = Utility.unsignedShort (buffer, 4);
    if (entryType != 0)
      throw new FileFormatException ("Pascal: entry type != 0");

    int nameLength = buffer[6] & 0xFF;
    if (nameLength < 1 || nameLength > 7)
      throw new FileFormatException ("bad name length : " + nameLength);

    volumeName = Utility.string (buffer, 7, nameLength);
    totalBlocks = Utility.unsignedShort (buffer, 14);       // 280, 1600, 2048
    totalFiles = Utility.unsignedShort (buffer, 16);
    firstBlock = Utility.unsignedShort (buffer, 18);
    date = Utility.getPascalLocalDate (buffer, 20);         // 2 bytes

    setTotalCatalogBlocks (firstFileBlock - 2);

    if (debug)
      System.out.println (this);

    for (int i = 2; i < firstFileBlock; i++)
    {
      AppleBlock block = getBlock (i, BlockType.FS_DATA);
      block.setBlockSubType ("CATALOG");
      catalogBlocks.add (block);
    }

    buffer = readBlocks (catalogBlocks);
    freeBlocks = totalBlocks - firstFileBlock;

    for (int i = 1; i <= totalFiles; i++)                   // skip volume entry
    {
      int ptr = i * CATALOG_ENTRY_SIZE;
      int fileType = buffer[ptr + 4] & 0xFF;

      if (fileType == 2)                                    // Code
      {
        if (true)
        {
          FilePascalCode file = new FilePascalCode (this, buffer, ptr);
          addFile2 (file);
        }
        else
        {
          FilePascal file = new FilePascal (this, buffer, ptr);
          addFile2 (file);

          Buffer dataRecord = file.getFileBuffer ();
          //          BlockReader blockReader2 
          //              = new BlockReader (file.getFileName (), file.read ());
          BlockReader blockReader2 =
              new BlockReader (file.getFileName (), dataRecord.data ());
          blockReader2.setParameters (512, AddressType.BLOCK, 0, 0);
          FsPascalCode fs = new FsPascalCode (blockReader2);
          file.embedFileSystem (fs);
        }
      }
      else
      {
        FilePascal file = new FilePascal (this, buffer, ptr);
        addFile2 (file);
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  private void addFile2 (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    super.addFile (file);
    freeBlocks -= file.getTotalBlocks ();
  }

  // ---------------------------------------------------------------------------------//
  public List<AppleBlock> getCatalogBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogBlocks;
  }

  // ---------------------------------------------------------------------------------//
  public int getVolumeTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  public String getVolumeName ()
  // ---------------------------------------------------------------------------------//
  {
    return volumeName;
  }

  // ---------------------------------------------------------------------------------//
  public LocalDate getDate ()
  // ---------------------------------------------------------------------------------//
  {
    return date;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    String line = "----   ---------------   ----   --------  -------   ----   ----";

    String date = getDate () == null ? "--" : getDate ().format (dtf);

    text.append (String.format ("Volume : %s%n", getVolumeName ()));
    text.append (String.format ("Date   : %s%n%n", date));
    text.append ("Blks   Name              Type     Date     Length   Frst   Last\n");
    text.append (line);
    text.append ("\n");

    for (AppleFile file : getFiles ())
    {
      text.append (file.getCatalogLine ());
      text.append ("\n");
    }

    text.append (line);
    text.append (
        String.format ("%nBlocks free : %3d  Blocks used : %3d  Total blocks : %3d",
            getTotalFreeBlocks (), getTotalBlocks () - getTotalFreeBlocks (), getTotalBlocks ()));

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("---- Pascal Header ----\n");
    text.append (String.format ("Volume name ........... %s%n", volumeName));
    text.append (String.format ("Directory ............. %d : %d%n", firstCatalogBlock,
        firstFileBlock - 1));
    text.append (String.format ("Entry type ............ %,d%n", entryType));
    text.append (String.format ("Total blocks .......... %,d%n", totalBlocks));
    text.append (String.format ("Total files ........... %,d%n", totalFiles));
    text.append (String.format ("First block ........... %,d%n", firstBlock));
    text.append (String.format ("Date .................. %s", date));

    return text.toString ();
  }
}
/*
Blocks 0 and 1 are the boot blocks.  The directory occupies blocks 2
through 5.  It contains 78 entries, each 26 bytes long.  Block boundaries
are ignored--the entire directory is treated as a single contiguous
2048-byte array.

The first entry is the volume name.  It has the following format:

     +0   word: block number of 1st directory block (always 0)
     +2   word: block number of last directory block +1 (always 6)
     +4   word: entry type (0=volume header)
     +6   string[7]: volume name (with length byte)
     +14  word: number of blocks on disk
     +16  word: number of files on disk
     +18  word: first block of volume
     +20  word: most recent date setting
     +22  4 bytes: reserved

The remaining entries are file entries:

     +0   word: block number of file's 1st block
     +2   word: block number of file's last block +1
     +4   word: bits 0-3: file type
                    1=xdskfile (for bad blocks)
                    2=codefile
                    3=textfile
                    4=infofile
                    5=datafile
                    6=graffile
                    7=fotofile
                    8=securedir (whatever that means)
                bits 4-14: reserved
                bit 15: used by Filer for wildcards
     +6   string[15]: file name (with length byte)
     +22  word: number of bytes used in file's last block
     +24  word: file modification date

The last 20 bytes of the directory are unused.

The date setting and last modification date are stored in a word as
follows:

     Bits 0-3: month (1-12)
     Bits 4-8: day (1-31)
     Bits 9-15: year (0-99)

When you find an entry whose name is the null string, you've reached the
end of the directory.  There are no special "deleted file" entries--when a
file is deleted, it is "squeezed out" of the directory by moving the
following entries one slot forward.

Files are stored contiguously on the disk...that is, each file occupies
all the blocks starting at the first block number listed in its directory
entry, and ending at one less than the last block number listed in its
directory entry.  Directory entries are sorted in order of increasing block
number.  There is no block map--due to the contiguous allocation scheme,
free space can easily be located by scanning the directory:  if a
directory entry's last-block-number field doesn't match the next entry's
first-block-number field, then you've found some free space.

All "word" entries are, of course, stored in standard Apple II
low-byte-first order.
*/
