package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------------//
public class FsPascal extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final int CATALOG_ENTRY_SIZE = 26;

  String volumeName;
  int blocks;         // size of disk
  int files;          // no of files on disk

  // ---------------------------------------------------------------------------------//
  public FsPascal (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length);
  }

  // ---------------------------------------------------------------------------------//
  public FsPascal (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length);
    setFileSystemName ("Pascal");
  }

  // ---------------------------------------------------------------------------------//
  void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    assert catalogBlocks == 0;

    AppleBlock vtoc = getBlock (2);
    byte[] buffer = vtoc.read ();

    int blockFrom = unsignedShort (buffer, 0);
    int blockTo = unsignedShort (buffer, 2);
    if (blockFrom != 0 || blockTo != 6)
      throw new FileFormatException (
          String.format ("from: %d, to: %d%n", blockFrom, blockTo));

    int nameLength = buffer[6] & 0xFF;
    if (nameLength < 1 || nameLength > 7)
      throw new FileFormatException ("bad name length : " + nameLength);
    volumeName = AbstractFileSystem.string (buffer, 7, nameLength);

    blocks = unsignedShort (buffer, 14);
    files = unsignedShort (buffer, 16);
    catalogBlocks = blockTo - 2;

    int max = Math.min (blockTo, totalBlocks);
    List<AppleBlock> addresses = new ArrayList<> ();
    for (int i = 2; i < max; i++)
      addresses.add (getBlock (i));

    buffer = readBlocks (addresses);

    for (int i = 1; i <= files; i++)      // skip first entry
    {
      int ptr = i * CATALOG_ENTRY_SIZE;
      AppleFile file = new FilePascal (this, buffer, ptr);
      this.addFile (file);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("Volume name ........... %s%n", volumeName));
    text.append (String.format ("Total blocks .......... %s%n", blocks));
    text.append (String.format ("Total files ........... %s%n", files));

    return text.toString ();
  }
}
/*
 * Blocks 0 and 1 are the boot blocks.  The directory occupies blocks 2
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
 * */
