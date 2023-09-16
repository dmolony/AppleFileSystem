package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileCpm extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private final int userNumber;

  private final boolean readOnly;
  private final boolean systemFile;

  private final int extentCounterLo;
  private final int extentCounterHi;
  private final int reserved;
  private int recordCount;                      // records used in this extent

  List<AppleBlock> dataBlocks = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  FileCpm (FsCpm parent, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    // hi-bits of type are used for flags
    readOnly = (buffer[ptr + 9] & 0x80) != 0;
    systemFile = (buffer[ptr + 10] & 0x80) != 0;

    byte[] typeBuffer = new byte[3];
    typeBuffer[0] = (byte) (buffer[ptr + 9] & 0x7F);
    typeBuffer[1] = (byte) (buffer[ptr + 10] & 0x7F);
    typeBuffer[2] = (byte) (buffer[ptr + 11] & 0x7F);
    fileTypeText = new String (typeBuffer).trim ();
    //    if (fileTypeText.length () == 0)
    //      fileTypeText = "???";

    userNumber = buffer[ptr] & 0xFF;
    fileName = new String (buffer, ptr + 1, 8).trim ();

    extentCounterLo = buffer[ptr + 12] & 0xFF;
    reserved = buffer[ptr + 13] & 0xFF;
    extentCounterHi = buffer[ptr + 14] & 0xFF;

    append (buffer, ptr);
  }

  // ---------------------------------------------------------------------------------//
  void append (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    recordCount = buffer[ptr + 15] & 0xFF;

    ptr += 16;
    for (int i = 0; i < 16; i++)
    {
      byte b = buffer[ptr + i];
      if (b == 0)
        break;

      if (false)
        if ((b & 0x80) != 0)
          System.out.printf ("%s CPM hi bit set%n", getFileName ());

      int blockNumber = ((b & 0x80) == 0) ? (b + 12) : (b & 0x7F);
      dataBlocks.add (getParentFileSystem ().getBlock (blockNumber, BlockType.FILE_DATA));
    }
  }

  // ---------------------------------------------------------------------------------//
  boolean isComplete ()
  // ---------------------------------------------------------------------------------//
  {
    return (recordCount & 0x80) == 0;
  }

  // ---------------------------------------------------------------------------------//
  public boolean isReadOnly ()
  // ---------------------------------------------------------------------------------//
  {
    return readOnly;
  }

  // ---------------------------------------------------------------------------------//
  public boolean isSystemFile ()
  // ---------------------------------------------------------------------------------//
  {
    return systemFile;
  }

  // ---------------------------------------------------------------------------------//
  public int getExtentCounterLo ()
  // ---------------------------------------------------------------------------------//
  {
    return extentCounterLo;
  }

  // ---------------------------------------------------------------------------------//
  public int getExtentCounterHi ()
  // ---------------------------------------------------------------------------------//
  {
    return extentCounterHi;
  }

  // ---------------------------------------------------------------------------------//
  public int getReserved ()
  // ---------------------------------------------------------------------------------//
  {
    return reserved;
  }

  // ---------------------------------------------------------------------------------//
  public int getRecordCount ()
  // ---------------------------------------------------------------------------------//
  {
    return recordCount;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFileSystem.readBlocks (dataBlocks);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size () * getParentFileSystem ().getBlockSize ();
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
  public int getTotalBlocks ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileTypeText.isEmpty () ? fileName : fileName + "." + fileTypeText;
  }

  // ---------------------------------------------------------------------------------//
  public int getUserNumber ()
  // ---------------------------------------------------------------------------------//
  {
    return userNumber;
  }

  // ---------------------------------------------------------------------------------//
  public String getShortName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return readOnly;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    char ro = isReadOnly () ? '*' : ' ';
    char sf = isSystemFile () ? '*' : ' ';

    return String.format ("%3d   %-8s   %-3s  %s %s   %03d", getUserNumber (),
        getShortName (), getFileTypeText (), ro, sf, getTotalBlocks ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("Read only ............. %s%n", readOnly));
    text.append (String.format ("System file ........... %s%n", systemFile));
    text.append (String.format ("User number ........... %,d%n", userNumber));
    text.append (String.format ("Extent lo ............. %,d%n", extentCounterLo));
    text.append (String.format ("Extent hi ............. %,d%n", extentCounterHi));
    text.append (String.format ("Data blocks ........... %,d%n", dataBlocks.size ()));
    text.append (String.format ("Records ............... %,d", recordCount));

    return Utility.rtrim (text);
  }
}
