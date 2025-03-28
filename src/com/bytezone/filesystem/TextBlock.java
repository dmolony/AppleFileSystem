package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bytezone.filesystem.TextBlock.TextRecord;

// -----------------------------------------------------------------------------------//
public class TextBlock implements Iterable<TextRecord>
// -----------------------------------------------------------------------------------//
{
  protected AppleFileSystem fs;
  protected AppleFile appleFile;
  protected List<AppleBlock> blocks;      // an island of data blocks within the file
  protected int startBlockNo;             // first block number in the island

  protected byte[] buffer;
  protected int firstByteNumber;

  protected List<TextRecord> records = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  public TextBlock (AppleFileSystem fs, AppleFile appleFile, List<AppleBlock> blocks,
      int startBlockNo)
  // ---------------------------------------------------------------------------------//
  {
    this.fs = fs;
    this.appleFile = appleFile;
    this.blocks = blocks;
    this.startBlockNo = startBlockNo;

    firstByteNumber = startBlockNo * fs.getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  public byte[] getBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    if (buffer == null)
      buffer = fs.readBlocks (blocks);

    return buffer;
  }

  // ---------------------------------------------------------------------------------//
  public int getStartByte ()
  // ---------------------------------------------------------------------------------//
  {
    return firstByteNumber;
  }

  // ---------------------------------------------------------------------------------//
  public int getTotalRecords ()
  // ---------------------------------------------------------------------------------//
  {
    return records.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Iterator<TextRecord> iterator ()
  // ---------------------------------------------------------------------------------//
  {
    return records.iterator ();
  }

  public record TextRecord (int offset, int length)
  {
  };

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (
        String.format ("File system .............. %s%n", fs.getFileSystemType ()));
    text.append (
        String.format ("File ..................... %s%n", appleFile.getFileName ()));
    text.append (
        String.format ("Total records ............ %04X  %<,9d%n", records.size ()));
    text.append (
        String.format ("Block size ............... %04X  %<,9d%n", fs.getBlockSize ()));
    text.append (
        String.format ("First logical byte ....... %04X  %<,9d%n", firstByteNumber));

    return text.toString ();
  }
}
