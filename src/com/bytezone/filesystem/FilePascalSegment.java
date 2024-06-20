package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FilePascalSegment extends AbstractAppleFile implements AppleContainer
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

  private final List<AppleFile> procedures;
  private final List<AppleFileSystem> notPossible = new ArrayList<> (0);

  // ---------------------------------------------------------------------------------//
  FilePascalSegment (FilePascalCode parent, byte[] catalogBuffer, int seq, String name)
  // ---------------------------------------------------------------------------------//
  {
    super (parent.getParentFileSystem ());

    fileName = name;
    slot = seq;
    fileTypeText = "SEG";
    fileType = 98;                  // pascal segment

    firstBlock = Utility.unsignedShort (catalogBuffer, seq * 4);
    eof = Utility.unsignedShort (catalogBuffer, seq * 4 + 2);
    size = (eof - 1) / BLOCK_SIZE + 1;

    addDataBlocks (parent.getFirstBlock ());      // used as an offset

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

    DataRecord dataRecord = getDataRecord ();
    //    byte[] dataBuffer = read ();
    byte[] dataBuffer = dataRecord.data ();

    totalProcedures = dataBuffer[eof - 1] & 0xFF;
    segmentNoBody = dataBuffer[eof - 2] & 0xFF;

    procedures = new ArrayList<> (totalProcedures);
    int totSize = 2 + totalProcedures * 2;

    for (int procNo = 1; procNo <= totalProcedures; procNo++)
    {
      FilePascalProcedure fpp = new FilePascalProcedure (this, dataBuffer, eof, procNo);
      procedures.add (fpp);
      totSize += fpp.getFileLength ();
    }

    if (eof != totSize)
      System.out.printf ("%8.8s Eof: %,7d, Size: %,7d%n", name, eof, totSize);
  }

  // ---------------------------------------------------------------------------------//
  private void addDataBlocks (int offset)
  // ---------------------------------------------------------------------------------//
  {
    int max = offset + firstBlock + size;

    for (int i = offset + firstBlock; i < max; i++)
    {
      AppleBlock block = getParentFileSystem ().getBlock (i);
      dataBlocks.add (block);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("cannot add File to " + fileName);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()
  // ---------------------------------------------------------------------------------//
  {
    return procedures;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Optional<AppleFile> getFile (String fileName)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleFile procedure : procedures)
      if (procedure.getFileName ().equals (fileName))
        return Optional.of (procedure);

    return Optional.empty ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFileSystem (AppleFileSystem fileSystem)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("cannot add FileSystem to " + fileName);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFileSystem> getFileSystems ()
  // ---------------------------------------------------------------------------------//
  {
    return notPossible;
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
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Segment - %s%n", getFileName ()));
    text.append ("==================\n");

    String warning = segmentNoBody == segmentNoHeader ? ""
        : String.format (" (%2d in routine)", segmentNoBody);

    text.append (String.format ("Segment........     %2d%s%n", segmentNoHeader, warning));
    text.append (String.format ("Segment........     %2d%n", segmentNoBody));
    text.append (String.format ("Address........     %02X%n", firstBlock));
    text.append (String.format ("Length......... %,6d  %<04X%n", eof));
    text.append (String.format ("Machine type...     %2d%n", machineType));
    text.append (String.format ("Version........     %2d%n", version));
    text.append (String.format ("Total procs....     %2d%n%n", procedures.size ()));

    text.append ("Procedure Dictionary\n====================\n\n");
    text.append ("Slot Offset  Hdr   Lvl  Entry   Exit   Parm   Data   Size\n");
    text.append ("---- ------  ----  ---  -----   ----   ----   ----   ----\n");

    for (AppleFile procedure : procedures)
      text.append (procedure.getCatalogLine () + "\n");

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return "NO PATH";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("\n");
    //    text.append ("\n\n");
    text.append ("------- Segment -------\n");
    text.append (String.format ("Slot .................. %d%n", slot));
    text.append (String.format ("First block ........... %d%n", firstBlock));
    text.append (String.format ("Total blocks .......... %d%n", size));
    text.append (String.format ("File length ........... %,d  %<04X%n", eof));
    text.append (String.format ("Text address .......... %d%n", textAddress));
    text.append (String.format ("Segment no header ..... %d%n", segmentNoHeader));
    text.append (String.format ("Segment no body ....... %d%n", segmentNoBody));
    text.append (String.format ("Machine type .......... %d  %s%n", machineType,
        machineTypes[machineType]));
    text.append (String.format ("Version ............... %d%n", version));
    text.append (String.format ("Segment kind .......... %s%n", segmentKind[segKind]));
    text.append (String.format ("Total procedures ...... %d%n", totalProcedures));

    return Utility.rtrim (text);
  }
}
