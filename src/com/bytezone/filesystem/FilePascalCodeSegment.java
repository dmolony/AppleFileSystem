package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// ***************** obsolete ********************
// -----------------------------------------------------------------------------------//
public class FilePascalCodeSegment extends AbstractAppleFile
    implements Iterable<PascalProcedure>
// -----------------------------------------------------------------------------------//
{
  private final static int BLOCK_SIZE = 512;
  private final static String[] segmentKind = { "Linked", "HostSeg", "SegProc", "UnitSeg",
      "SeprtSeg", "UnlinkedIntrins", "LinkedIntrins", "DataSeg" };
  private final static String[] machineTypes = { "Unknown", "Positive byte sex p-code",
      "Negative byte sex p-code (apple pascal)", "6502 code (3)", "6502 code (4)",
      "6502 code (5)", "6502 code (6)", "apple 6502", "6502 code (8)", "6502 code (9)" };

  private final int segmentNoBody;
  private final int segmentNoHeader;
  private final int firstBlock;
  private final int eof;
  private final int size;
  private final int segKind;
  private final int textAddress;
  private final int machineType;
  private final int version;
  private final int intrinsSegs1;
  private final int intrinsSegs2;
  private final int slot;
  private final int totalProcedures;

  private List<PascalProcedure> procedures;

  boolean debug = false;

  // ---------------------------------------------------------------------------------//
  FilePascalCodeSegment (FsPascalCode fs, byte[] catalogBuffer, int seq, String name)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    fileName = name;
    slot = seq;
    fileTypeText = "SEG";

    firstBlock = Utility.unsignedShort (catalogBuffer, seq * 4);
    eof = Utility.unsignedShort (catalogBuffer, seq * 4 + 2);
    size = (eof - 1) / BLOCK_SIZE + 1;

    addDataBlocks ();

    segKind = Utility.unsignedShort (catalogBuffer, 0xC0 + seq * 2);
    textAddress = Utility.unsignedShort (catalogBuffer, 0xE0 + seq * 2);

    // segment 1 is the main segment, 2-6 are used by the system, and 7
    // onwards is for the program
    this.segmentNoHeader = catalogBuffer[0x100 + seq * 2] & 0xFF;
    int flags = catalogBuffer[0x101 + seq * 2] & 0xFF;

    // 0 unknown,
    // 1 positive byte sex p-code
    // 2 negative byte sex p-code (apple pascal)
    // 3-9 6502 code (7 = apple 6502)
    machineType = flags & 0x0F;
    version = (flags & 0xD0) >> 5;

    intrinsSegs1 = Utility.unsignedShort (catalogBuffer, 0x120 + seq * 4);
    intrinsSegs2 = Utility.unsignedShort (catalogBuffer, 0x120 + seq * 4 + 2);

    byte[] dataBuffer = read ();

    totalProcedures = dataBuffer[eof - 1] & 0xFF;
    segmentNoBody = dataBuffer[eof - 2] & 0xFF;

    if (debug)
      if (segmentNoHeader == 0)
        System.out.printf ("Zero segment header in %s seq %d%n", getFileName (), seq);
      else if (segmentNoBody != segmentNoHeader)
        System.out.println (
            "Segment number mismatch : " + segmentNoBody + " / " + segmentNoHeader);

    procedures = new ArrayList<> (totalProcedures);

    for (int procNo = 1; procNo <= totalProcedures; procNo++)
      procedures.add (new PascalProcedure (dataBuffer, eof, procNo));
  }

  // ---------------------------------------------------------------------------------//
  private void addDataBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    int max = firstBlock + size;

    for (int i = firstBlock; i < max; i++)
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
    return eof;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format (
        " %2d  %3d  %3d  %04X  %-8s  %-15s%3d   %02X  %d   %d   %d   %d  %4d", slot,
        firstBlock, size, eof, getFileName (), segmentKind[segKind], textAddress,
        segmentNoHeader, machineType, version, intrinsSegs1, intrinsSegs2,
        totalProcedures);
  }

  // ---------------------------------------------------------------------------------//
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("Procedure List\n==================\n\n");
    text.append ("Heading\n");
    text.append (
        "---- ---- ---- ----  --------  --------------- --- --- --- --- --- --- ----\n");

    for (PascalProcedure procedure : procedures)
      text.append (procedure.getCatalogLine () + "\n");

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("Slot .................. %d%n", slot));
    text.append (String.format ("First block ........... %d%n", firstBlock));
    text.append (String.format ("Total blocks .......... %d%n", dataBlocks.size ()));
    text.append (String.format ("File length ........... %,d%n", eof));
    text.append (String.format ("Text address .......... %d%n", textAddress));
    text.append (String.format ("Segment no header ..... %d%n", segmentNoHeader));
    text.append (String.format ("Segment no body ....... %d%n", segmentNoBody));
    text.append (String.format ("Machine type .......... %d  %s%n", machineType,
        machineTypes[machineType]));
    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Total procedures ...... %d%n", totalProcedures));
    text.append (String.format ("Segment kind .......... %s%n", segmentKind[segKind]));

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Iterator<PascalProcedure> iterator ()
  // ---------------------------------------------------------------------------------//
  {
    return procedures.iterator ();
  }
}
