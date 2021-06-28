package com.bytezone.filesystem;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractFileSystem implements AppleFileSystem
// -----------------------------------------------------------------------------------//
{
  private String fileSystemName;        // DosX.X, Prodos, Pascal, CPM
  private String fileName;

  final byte[] diskBuffer;      // entire buffer including any header or other disks
  final int diskOffset;         // start of this disk
  final int diskLength;         // length of this disk

  private BlockReader blockReader;

  int totalBlocks;
  int catalogBlocks;

  List<AppleFile> files = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  public AbstractFileSystem (String fileName, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    this.fileName = fileName;
    this.diskBuffer = buffer;
    this.diskOffset = offset;
    this.diskLength = length;

    assert offset + length <= diskBuffer.length : String.format (
        "Disk length: %,d too small for offset %,d + length %,d", diskBuffer.length,
        offset, length);
  }

  // ---------------------------------------------------------------------------------//
  void setFileSystemName (String fileSystemName)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystemName = fileSystemName;
  }

  // ---------------------------------------------------------------------------------//
  int getTotalCatalogBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogBlocks;
  }

  // ---------------------------------------------------------------------------------//
  boolean isValidBlockNo (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockNo >= 0 && blockNo < getSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void setBlockReader (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this.blockReader = blockReader;
//    System.out.printf ("Setting BlockReader: %s%n", blockReader);
//    System.out.printf ("in FS: %s%n", this.toText ());

    totalBlocks = diskLength / blockReader.blockSize;
    catalogBlocks = 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock allocate ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("allocate() not implemented");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AddressType getType ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.addressType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlocksPerTrack ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.blocksPerTrack;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock getBlock (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getBlock (this, blockNo);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock getSector (int track, int sector)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getSector (this, track, sector);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] readBlock (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.read (diskBuffer, diskOffset, block);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] readBlocks (List<AppleBlock> blocks)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.read (diskBuffer, diskOffset, blocks);
  }

  // AppleFile methods

  // ---------------------------------------------------------------------------------//
  @Override
  public String getName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isDirectory ()
  // ---------------------------------------------------------------------------------//
  {
    return false;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFile ()
  // ---------------------------------------------------------------------------------//
  {
    return false;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    files.add (file);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()
  // ---------------------------------------------------------------------------------//
  {
    return files;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlockSize ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.blockSize;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("Cannot read() a file system");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void write (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("Cannot write() a file system");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()
  // ---------------------------------------------------------------------------------//
  {
    return diskLength;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()
  // ---------------------------------------------------------------------------------//
  {
    return totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("Cannot call getBlocks() on a file system");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-20.20s %-6s %,8d  %d %,7d  %2d  %3d ", fileName,
        fileSystemName, diskOffset, blockReader.interleave, totalBlocks,
        getTotalCatalogBlocks (), files.size ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File system ........... %s%n", fileSystemName));
    text.append (String.format ("Disk offset ........... %d%n", diskOffset));
    text.append (String.format ("Disk length ........... %,d%n", diskLength));
    text.append (String.format ("Total blocks .......... %,d%n", totalBlocks));
    text.append (String.format ("Catalog blocks ........ %d%n", catalogBlocks));
    text.append (String.format ("Block size ............ %d%n", blockReader.blockSize));
    text.append (String.format ("Interleave ............ %d%n", blockReader.interleave));
    text.append (String.format ("Total files ........... %d%n", files.size ()));

    return text.toString ();
  }

  // to remove later

  // ---------------------------------------------------------------------------------//
  public static int unsignedShort (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    if (ptr >= buffer.length - 1)
    {
      System.out.println ("Index out of range (unsigned short): " + ptr);
      return 0;
    }
    return (buffer[ptr] & 0xFF) | ((buffer[ptr + 1] & 0xFF) << 8);
  }

  // ---------------------------------------------------------------------------------//
  public static int unsignedTriple (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    return (buffer[ptr] & 0xFF) | (buffer[ptr + 1] & 0xFF) << 8
        | (buffer[ptr + 2] & 0xFF) << 16;
  }

  // ---------------------------------------------------------------------------------//
  static String format (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    return format (buffer, 0, buffer.length, true, 0);
  }

  private static String[] hex =
      { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

  // ---------------------------------------------------------------------------------//
  static String format (byte[] buffer, int offset, int length, boolean header,
      int startingAddress)
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder line = new StringBuilder ();
    int[] freq = new int[256];
    boolean startedOnBoundary = offset % 0x100 == 0;

    if (header)
    {
      line.append ("      ");
      for (int i = 0; i < 16; i++)
        line.append ("  " + hex[i]);
      if (offset == 0)
        line.append ("\n");
    }

    for (int i = offset; i < offset + length; i += 16)
    {
      if (line.length () > 0 && i > 0)
        line.append ("\n");
      if (i > offset && startedOnBoundary && (i % 0x200) == 0)
        line.append ("\n");

      // print offset
      line.append (String.format ("%05X : ", (startingAddress + i - offset)));

      // print hex values
      StringBuffer trans = new StringBuffer ();
      StringBuffer hexLine = new StringBuffer ();

      int max = Math.min (i + 16, offset + length);
      max = Math.min (max, buffer.length);
      for (int j = i; j < max; j++)
      {
        int c = buffer[j] & 0xFF;
        freq[c]++;
        hexLine.append (String.format ("%02X ", c));

        if (c > 127)
        {
          if (c < 160)
            c -= 64;
          else
            c -= 128;
        }
        if (c < 32 || c == 127)         // non-printable
          trans.append (".");
        else                            // standard ascii
          trans.append ((char) c);
      }
      while (hexLine.length () < 48)
        hexLine.append (" ");

      line.append (hexLine.toString () + ": " + trans.toString ());
    }

    if (false)
    {
      line.append ("\n\n");
      int totalBits = 0;
      for (int i = 0; i < freq.length; i++)
        if (freq[i] > 0)
        {
          totalBits += (Integer.bitCount (i) * freq[i]);
          line.append (
              String.format ("%02X  %3d   %d%n", i, freq[i], Integer.bitCount (i)));
        }
      line.append (String.format ("%nTotal bits : %d%n", totalBits));
    }
    return line.toString ();
  }

  // ---------------------------------------------------------------------------------//
  public static String string (byte[] buffer, int ptr, int nameLength)
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();
    for (int i = 0; i < nameLength; i++)
    {
      int c = buffer[ptr + i] & 0x7F;
      if (c < 32)
        c += 64;
      //      if (c >= 32)
      text.append ((char) c);
      //      else
      //        text.append (".");
    }
    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  public static LocalDateTime getAppleDate (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    int yymmdd = unsignedShort (buffer, offset);
    if (yymmdd != 0)
    {
      int year = (yymmdd & 0xFE00) >> 9;
      int month = (yymmdd & 0x01E0) >> 5;
      int day = yymmdd & 0x001F;

      int minute = buffer[offset + 2] & 0x3F;
      int hour = buffer[offset + 3] & 0x1F;

      if (year < 70)
        year += 2000;
      else
        year += 1900;

      try
      {
        return LocalDateTime.of (year, month, day, hour, minute);
      }
      catch (DateTimeException e)
      {
        System.out.printf ("Bad date/time: %d %d %d %d %d %n", year, month, day, hour,
            minute);
      }
    }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  public static LocalDate getPascalDate (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    int date = AbstractFileSystem.unsignedShort (buffer, offset);
    int month = date & 0x0F;
    int day = (date & 0x1F0) >>> 4;
    int year = (date & 0xFE00) >>> 9;

    if (year < 70)
      year += 2000;
    else
      year += 1900;

    return LocalDate.of (year, month, day);
  }
}
