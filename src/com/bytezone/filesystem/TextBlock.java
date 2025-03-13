package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
public class TextBlock
// -----------------------------------------------------------------------------------//
{
  protected AppleFileSystem fs;
  protected List<AppleBlock> blocks;      // an island of data blocks within the file
  protected int startBlockNo;             // block number within the file

  protected byte[] buffer;
  protected int firstLogicalByte;

  // ---------------------------------------------------------------------------------//
  public TextBlock (AppleFileSystem fs, List<AppleBlock> blocks, int startBlockNo)
  // ---------------------------------------------------------------------------------//
  {
    this.fs = fs;
    this.blocks = blocks;
    this.startBlockNo = startBlockNo;
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
    return firstLogicalByte;
  }

  // ---------------------------------------------------------------------------------//
  public String getText ()
  // ---------------------------------------------------------------------------------//
  {
    return "override me";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (
        String.format ("File system .............. %s%n", fs.getFileSystemType ()));
    text.append (String.format ("Block size ............... %,7d%n", fs.getBlockSize ()));
    text.append (String.format ("First logical byte ....... %,7d%n", firstLogicalByte));

    return text.toString ();
  }
}
