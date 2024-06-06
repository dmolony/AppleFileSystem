package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FilePascalCode extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private final static int BLOCK_SIZE = 512;
  static String[] segmentKind = { "Linked", "HostSeg", "SegProc", "UnitSeg", "SeprtSeg",
      "UnlinkedIntrins", "LinkedIntrins", "DataSeg" };

  private int segmentNoBody;
  final int segmentNoHeader;
  public int blockNo;
  public final int size;
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

    blockNo = Utility.unsignedShort (buffer, seq * 4);
    size = Utility.unsignedShort (buffer, seq * 4 + 2);
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
    {
      segmentBuffer = new byte[0];
    }
    else if ((offset + size) < buffer.length)
    {
      segmentBuffer = new byte[size];
      System.arraycopy (buffer, offset, segmentBuffer, 0, size);
      totalProcedures = segmentBuffer[size - 1] & 0xFF;
      segmentNoBody = segmentBuffer[size - 2] & 0xFF;

      if (debug)
        if (segmentNoHeader == 0)
          System.out.printf ("Zero segment header in %s seq %d%n", getFileName (), seq);
        else if (segmentNoBody != segmentNoHeader)
          System.out.println (
              "Segment number mismatch : " + segmentNoBody + " / " + segmentNoHeader);
    }
    else
    {
      throw new FileFormatException ("Error in PascalSegment");
    }
  }

  // ---------------------------------------------------------------------------------//
  private String getMultiDiskAddresses ()
  // ---------------------------------------------------------------------------------//
  {
    String multiDiskAddressText = "";
    //    int sizeInBlocks = (size - 1) / BLOCK_SIZE + 1;

    //    if (segmentNoHeader == 1)           // main segment
    //    {
    //      multiDiskAddressText = String.format ("1:%03X", (blockNo + blockOffset));
    //    }
    //    else
    //    if (relocator != null)
    //    {
    //      int targetBlock = blockNo + blockOffset;
    //      List<MultiDiskAddress> addresses =
    //          relocator.getMultiDiskAddress (name, targetBlock, sizeInBlocks);
    //      if (addresses.isEmpty ())
    //        multiDiskAddressText = ".";
    //      else
    //      {
    //        StringBuilder locations = new StringBuilder ();
    //        for (MultiDiskAddress multiDiskAddress : addresses)
    //          locations.append (multiDiskAddress.toString () + ", ");
    //        if (locations.length () > 2)
    //        {
    //          locations.deleteCharAt (locations.length () - 1);
    //          locations.deleteCharAt (locations.length () - 1);
    //        }
    //        multiDiskAddressText = locations.toString ();
    //      }
    //    }
    return multiDiskAddressText;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    int sizeInBlocks = (size - 1) / BLOCK_SIZE + 1;

    return String.format (
        " %2d   %02X   %02X  %04X  %-8s  %-15s%3d   " + "%02X  %d   %d   %d   %d  %s",
        slot, blockNo, sizeInBlocks, size, getFileName (), segmentKind[segKind],
        textAddress, segmentNoHeader, machineType, version, intrinsSegs1, intrinsSegs2,
        getMultiDiskAddresses ());
  }
}
