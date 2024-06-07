package com.bytezone.filesystem;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FilePascalCode extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private final static int BLOCK_SIZE = 512;
  private final static String[] segmentKind = { "Linked", "HostSeg", "SegProc", "UnitSeg",
      "SeprtSeg", "UnlinkedIntrins", "LinkedIntrins", "DataSeg" };

  private int segmentNoBody;
  final int segmentNoHeader;
  public int blockNo;
  public final int sizeInBytes;
  public final int sizeInBlocks;
  private final int segKind;
  private final int textAddress;
  private final int machineType;
  private final int version;
  private final int intrinsSegs1;
  private final int intrinsSegs2;
  private final int slot;
  private int totalProcedures;
  //  private List<PascalProcedure> procedures;

  byte[] segmentBuffer;

  boolean debug = false;

  // ---------------------------------------------------------------------------------//
  FilePascalCode (FsPascalCode fs, byte[] buffer, int seq, String name)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    fileName = name;
    slot = seq;
    fileTypeText = "SEG";

    blockNo = Utility.unsignedShort (buffer, seq * 4);
    sizeInBytes = Utility.unsignedShort (buffer, seq * 4 + 2);
    sizeInBlocks = (sizeInBytes - 1) / BLOCK_SIZE + 1;

    addDataBlocks ();

    segKind = Utility.unsignedShort (buffer, 0xC0 + seq * 2);
    textAddress = Utility.unsignedShort (buffer, 0xE0 + seq * 2);

    // segment 1 is the main segment, 2-6 are used by the system, and 7
    // onwards is for the program
    this.segmentNoHeader = buffer[0x100 + seq * 2] & 0xFF;
    int flags = buffer[0x101 + seq * 2] & 0xFF;

    // 0 unknown,
    // 1 positive byte sex p-code
    // 2 negative byte sex p-code (apple pascal)
    // 3-9 6502 code (7 = apple 6502)
    machineType = flags & 0x0F;

    version = (flags & 0xD0) >> 5;

    intrinsSegs1 = Utility.unsignedShort (buffer, 0x120 + seq * 4);
    intrinsSegs2 = Utility.unsignedShort (buffer, 0x120 + seq * 4 + 2);

    int offset = blockNo * BLOCK_SIZE;

    if (offset < 0)
      segmentBuffer = new byte[0];
    else
    {
      byte[] segmentBuffer = read ();

      totalProcedures = segmentBuffer[sizeInBytes - 1] & 0xFF;
      segmentNoBody = segmentBuffer[sizeInBytes - 2] & 0xFF;

      if (debug)
        if (segmentNoHeader == 0)
          System.out.printf ("Zero segment header in %s seq %d%n", getFileName (), seq);
        else if (segmentNoBody != segmentNoHeader)
          System.out.println (
              "Segment number mismatch : " + segmentNoBody + " / " + segmentNoHeader);
    }
  }

  // ---------------------------------------------------------------------------------//
  private void addDataBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    int max = blockNo + sizeInBlocks;

    for (int i = blockNo; i < max; i++)
    {
      AppleBlock block = getParentFileSystem ().getBlock (i, BlockType.FILE_DATA);
      block.setFileOwner (this);
      dataBlocks.add (block);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    return sizeInBytes;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format (
        " %2d  %3d  %3d  %04X  %-8s  %-15s%3d   %02X  %d   %d   %d   %d", slot, blockNo,
        sizeInBlocks, sizeInBytes, getFileName (), segmentKind[segKind], textAddress,
        segmentNoHeader, machineType, version, intrinsSegs1, intrinsSegs2);
  }
}
