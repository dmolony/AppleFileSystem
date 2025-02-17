package com.bytezone.filesystem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsPascal extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter dtf =
      DateTimeFormatter.ofLocalizedDate (FormatStyle.SHORT);

  private static final int CATALOG_ENTRY_SIZE = 26;
  private CatalogEntryPascal[] fileEntries;

  private List<AppleBlock> catalogBlocks = new ArrayList<> ();
  private byte[] catalogBuffer;

  private boolean debug = false;

  // ---------------------------------------------------------------------------------//
  public FsPascal (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.PASCAL);

    AppleBlock vtoc = getBlock (2, BlockType.FS_DATA);
    vtoc.setBlockSubType ("CATALOG");
    byte[] buffer = vtoc.getBuffer ();

    CatalogEntryPascal.checkVolumeHeaderFormat (buffer);

    int firstFileBlock = Utility.unsignedShort (buffer, 2);

    // build the catalog buffer (usually blocks 2/3/4/5)
    for (int i = 2; i < firstFileBlock; i++)
    {
      AppleBlock block = getBlock (i, BlockType.FS_DATA);
      block.setBlockSubType ("CATALOG");
      catalogBlocks.add (block);
    }

    catalogBuffer = readBlocks (catalogBlocks);
    setTotalCatalogBlocks (firstFileBlock - 2);

    // read all potential catalog entries (78 in 4 blocks)
    int maxEntries = totalCatalogBlocks * getBlockSize () / CATALOG_ENTRY_SIZE;
    fileEntries = new CatalogEntryPascal[maxEntries];

    for (int i = 0; i < maxEntries; i++)
      fileEntries[i] = new CatalogEntryPascal (catalogBlocks, catalogBuffer, i);

    CatalogEntryPascal volumeEntry = fileEntries[0];
    freeBlocks = volumeEntry.totalBlocks - firstFileBlock;

    volumeBitMap = new BitSet (volumeEntry.totalBlocks);    // initially all off (used)
    volumeBitMap.set (firstFileBlock, volumeEntry.totalBlocks);   // on = free

    // process each file in the catalog (skip the volume entry)
    for (int i = 1, count = 0; count < volumeEntry.totalFiles; i++)
    {
      CatalogEntryPascal catalogEntry = fileEntries[i];

      if (catalogEntry.firstBlock == 0)
        continue;

      ++count;

      //      if (catalogEntry.fileType == 2)                           // Code
      //        addFile (new FilePascalCode (this, catalogEntry, i));
      //      else
      addFile (new FilePascal (this, catalogEntry, i));
    }

    if (debug)
      listSlots ();
  }

  // ---------------------------------------------------------------------------------//
  private void addFile (FilePascal file)
  // ---------------------------------------------------------------------------------//
  {
    super.addFile (file);

    freeBlocks -= file.getTotalBlocks ();
    volumeBitMap.clear (file.getFirstBlock (), file.getLastBlock ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFree (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    return volumeBitMap.get (block.getBlockNo ());
  }

  // ---------------------------------------------------------------------------------//
  public List<AppleBlock> getCatalogBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogBlocks;
  }

  // ---------------------------------------------------------------------------------//
  public byte[] getCatalogBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogBuffer;
  }

  // ---------------------------------------------------------------------------------//
  public int getVolumeTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntries[0].totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  public String getVolumeName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntries[0].volumeName;
  }

  // ---------------------------------------------------------------------------------//
  public LocalDate getDate ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntries[0].volumeDate;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void cleanDisk ()
  // ---------------------------------------------------------------------------------//
  {
    crunch ();                    // shuffle files to the front
    super.cleanDisk ();           // zero out unused blocks
  }

  // ---------------------------------------------------------------------------------//
  void crunch ()
  // ---------------------------------------------------------------------------------//
  {
    int count = 0;
    CatalogEntryPascal volumeEntry = fileEntries[0];
    int nextBlock = volumeEntry.lastCatalogBlock;         // where to store the next file

    for (int i = 1; i < fileEntries.length; i++)          // skip volume header
    {
      if (count++ == volumeEntry.totalFiles)
        break;

      if (fileEntries[i].firstBlock == 0)                 // found a gap
      {
        if (debug)
          System.out.printf ("Crunching slot %d%n", i);
        moveFile (findNextSlot (i), i, nextBlock);        // fill the gap
      }

      nextBlock += fileEntries[i].length ();              // now has valid data
    }

    writeCatalogBlocks ();                   // move catalog buffers to disk buffer
  }

  // ---------------------------------------------------------------------------------//
  private void moveFile (int slotFrom, int slotTo, int nextDataBlock)
  // ---------------------------------------------------------------------------------//
  {
    FilePascal file = findFile (slotFrom);

    List<AppleBlock> oldBlocks = file.getBlocks ();
    List<AppleBlock> newBlocks = new ArrayList<> (oldBlocks.size ());

    if (debug)
    {
      System.out.printf ("Moving slot %d (%s) to slot %d%n", slotFrom,
          file.getFileName (), slotTo);
      CatalogEntryPascal from = fileEntries[slotFrom];
      System.out.printf ("  Blocks %3d:%3d -> %3d:%3d%n", from.firstBlock,
          from.lastBlock - 1, nextDataBlock, nextDataBlock + oldBlocks.size () - 1);
    }

    // copy blocks
    int blockPtr = nextDataBlock;
    for (AppleBlock oldBlock : oldBlocks)
    {
      // get destination block
      AppleBlock newBlock = getBlock (blockPtr++);
      newBlocks.add (newBlock);

      if (debug)
        System.out.printf ("  %3d -> %3d%n", oldBlock.getBlockNo (),
            newBlock.getBlockNo ());

      // copy data
      byte[] fromBuffer = oldBlock.getBuffer ();
      byte[] toBuffer = newBlock.getBuffer ();
      System.arraycopy (fromBuffer, 0, toBuffer, 0, fromBuffer.length);
      newBlock.markDirty ();

      // set free/used
      volumeBitMap.set (oldBlock.getBlockNo ());
      volumeBitMap.clear (newBlock.getBlockNo ());
    }

    // remove file's reference to old blocks, add new blocks
    oldBlocks.clear ();
    oldBlocks.addAll (newBlocks);

    // move catalog data to new slot
    fileEntries[slotTo].copyFileEntry (fileEntries[slotFrom], nextDataBlock);

    // mark old slot as unused
    fileEntries[slotFrom].clearFileEntry ();

    // update catalog buffer
    fileEntries[slotFrom].write ();
    fileEntries[slotTo].write ();

    if (debug)
    {
      System.out.printf ("After crunching slot %d%n", slotFrom);
      listSlots ();
    }
  }

  // find the file referenced by the slot
  // ---------------------------------------------------------------------------------//
  private FilePascal findFile (int slot)
  // ---------------------------------------------------------------------------------//
  {
    CatalogEntryPascal fileEntry = fileEntries[slot];

    for (AppleFile file : files)
      if (file.getFileName ().equals (fileEntry.fileName))
        return (FilePascal) file;

    System.out.println ("not found");
    return null;
  }

  // find the slot containing an existing file
  // ---------------------------------------------------------------------------------//
  private int findSlot (FilePascal file)
  // ---------------------------------------------------------------------------------//
  {
    for (int i = 1; i < fileEntries.length; i++)
      if (fileEntries[i].fileName.equals (file.getFileName ()))
        return i;

    System.out.println (file.getFileName () + " not found");
    return -1;
  }

  // search list for next empty slot
  // ---------------------------------------------------------------------------------//
  private int findNextSlot (int from)
  // ---------------------------------------------------------------------------------//
  {
    while (fileEntries[from].firstBlock == 0)
      ++from;

    return from;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void deleteFile (AppleFile appleFile)
  // ---------------------------------------------------------------------------------//
  {
    if (appleFile.getParentFileSystem () != this)
      throw new InvalidParentFileSystemException ("file not part of this File System");

    FilePascal file = (FilePascal) appleFile;

    if (debug)
      System.out.printf ("Deleting %s%n", file.getFileName ());

    for (AppleBlock block : file.getBlocks ())
      volumeBitMap.set (block.getBlockNo ());               // on = free

    fileEntries[0].clearFileEntry ();
    fileEntries[findSlot (file)].clearFileEntry ();

    files.remove (file);

    writeCatalogBlocks ();                        // move catalog buffers to disk buffer

    if (debug)
      listSlots ();
  }

  // ---------------------------------------------------------------------------------//
  private void writeCatalogBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    // put catalog buffer back into its blocks
    int ptr = 0;

    for (AppleBlock catalogBlock : catalogBlocks)
    {
      byte[] buffer = catalogBlock.getBuffer ();
      System.arraycopy (catalogBuffer, ptr, buffer, 0, buffer.length);

      ptr += buffer.length;
      catalogBlock.markDirty ();
    }
  }

  // ---------------------------------------------------------------------------------//
  private void listSlots ()
  // ---------------------------------------------------------------------------------//
  {
    int count = 0;
    int max = fileEntries[0].totalFiles;

    for (int i = 0; count < max; i++)
    {
      System.out.println (fileEntries[i].getLine ());
      if (fileEntries[i].firstBlock > 0)
        ++count;
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    String line = "----   ---------------   ----   --------  -------   ----   ----";

    CatalogEntryPascal volumeEntry = fileEntries[0];
    String date = getDate () == null ? "--" : getDate ().format (dtf);

    text.append (String.format ("Volume : %s%n", getVolumeName ()));
    text.append (String.format ("Date   : %s%n", date));
    text.append (String.format ("Files  : %s%n%n", volumeEntry.totalFiles));
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
            getTotalFreeBlocks (), getTotalBlocks () - getTotalFreeBlocks (),
            getTotalBlocks ()));

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (fileEntries[0]);
    //    text.append ("\n\n");

    return Utility.rtrim (text);
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
