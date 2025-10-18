package com.bytezone.woz;

import static com.bytezone.utility.Utility.formatText;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class WozFile
//-----------------------------------------------------------------------------------//
{
  private static final byte[] address16prologue =
      { (byte) 0xD5, (byte) 0xAA, (byte) 0x96 };
  private static final byte[] address13prologue =
      { (byte) 0xD5, (byte) 0xAA, (byte) 0xB5 };
  private static final byte[] dataPrologue = { (byte) 0xD5, (byte) 0xAA, (byte) 0xAD };
  private static final byte[] epilogue = { (byte) 0xDE, (byte) 0xAA, (byte) 0xEB };
  // apparently it can be DE AA Ex

  private static final int BLOCK_SIZE = 512;
  private static final int SECTOR_SIZE = 256;

  private static final int TRK_SIZE = 0x1A00;
  private static final int DATA_SIZE = TRK_SIZE - 10;

  private static int[][] interleave = //
      { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 },         // 13 sector
          { 0, 7, 14, 6, 13, 5, 12, 4, 11, 3, 10, 2, 9, 1, 8, 15 } };     // 16 sector

  private Info info;
  private Meta meta;
  private int diskSectors;

  private byte[] addressPrologue;
  private byte[] diskBuffer;
  private List<Track> tracks;

  private final boolean debug1 = false;
  private final boolean showTracks = false;

  private final ByteTranslator6and2 byteTranslator6and2 = new ByteTranslator6and2 ();

  // ---------------------------------------------------------------------------------//
  public WozFile (byte[] buffer) throws DiskNibbleException
  // ---------------------------------------------------------------------------------//
  {
    String header = new String (buffer, 0, 4);
    if (!"WOZ1".equals (header) && !"WOZ2".equals (header))
      throw new DiskNibbleException ("Header error");

    int checksum1 = val32 (buffer, 8);
    int checksum2 = Utility.crc32 (buffer, 12, buffer.length - 12);
    if (checksum1 != checksum2)
    {
      System.out.printf ("Stored checksum     : %08X%n", checksum1);
      System.out.printf ("Calculated checksum : %08X%n", checksum2);
      throw new DiskNibbleException ("Checksum error");
    }

    int ptr = 12;
    while (ptr < buffer.length)
    {
      validateChunk (buffer, ptr);

      String chunkId = new String (buffer, ptr, 4);
      int size = val32 (buffer, ptr + 4);
      if (debug1)
        System.out.printf ("%n%s  %,9d%n", chunkId, size);

      switch (chunkId)
      {
        case "INFO":                            // 60 bytes
          info = new Info (buffer, ptr);
          if (info.wozVersion >= 2)
            setPrologue (info.bootSectorFormat == 2 ? 13 : 16);
          break;
        case "TMAP":                            // 160 bytes
          tmap (buffer, ptr);
          break;
        case "TRKS":                            // starts at 248, data at 256
          tracks = trks (buffer, ptr);
          break;
        case "META":
          meta = new Meta (buffer, ptr, size);
          break;
        case "WRIT":
          break;
        default:
          break;
      }
      ptr += size + 8;
    }

    if (info.diskType == 1)                   // 5.25"
    {
      diskBuffer = new byte[tracks.size () * diskSectors * SECTOR_SIZE];

      for (Track track : tracks)
        track.pack (diskBuffer);
    }
    else if (info.diskType == 2)              // 3.5"
    {
      List<Sector> sectors = new ArrayList<> ();
      for (Track track : tracks)
        sectors.addAll (track.sectors);
      Collections.sort (sectors);

      diskBuffer = new byte[800 * info.sides * BLOCK_SIZE];
      ptr = 0;

      for (Sector sector : sectors)
      {
        sector.pack35 (diskBuffer, ptr);
        ptr += BLOCK_SIZE;
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  private boolean validateChunk (byte[] buffer, int ptr) throws DiskNibbleException
  // ---------------------------------------------------------------------------------//
  {
    int size = val32 (buffer, ptr + 4);
    if (size <= 0 || size + ptr + 8 > buffer.length)
    {
      //      if (info != null)
      //        System.out.println (info);
      throw new DiskNibbleException (String.format ("Invalid chunk size: %08X%n", size));
    }

    for (int i = 0; i < 4; i++)
    {
      int val = buffer[ptr + i] & 0xFF;
      if (val < 'A' || val > 'Z')               // not uppercase ascii
      {
        //        if (info != null)
        //          System.out.println (info);
        throw new DiskNibbleException (
            String.format ("Invalid chunk name character: %02X%n", val));
      }
    }

    return true;
  }

  // ---------------------------------------------------------------------------------//
  public byte[] getDiskBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return diskBuffer;
  }

  // ---------------------------------------------------------------------------------//
  public int getDiskType ()
  // ---------------------------------------------------------------------------------//
  {
    return info.diskType;
  }

  // ---------------------------------------------------------------------------------//
  public int getSides ()
  // ---------------------------------------------------------------------------------//
  {
    return info.sides;
  }

  // ---------------------------------------------------------------------------------//
  public int getTracks ()
  // ---------------------------------------------------------------------------------//
  {
    return tracks.size ();
  }

  // ---------------------------------------------------------------------------------//
  public int getSectorsPerTrack ()
  // ---------------------------------------------------------------------------------//
  {
    return diskSectors;
  }

  // ---------------------------------------------------------------------------------//
  private void setPrologue (int diskSectors)
  // ---------------------------------------------------------------------------------//
  {
    this.diskSectors = diskSectors;
    addressPrologue = diskSectors == 13 ? address13prologue : address16prologue;
  }

  // ---------------------------------------------------------------------------------//
  private void tmap (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    ptr += 8;
  }

  // ---------------------------------------------------------------------------------//
  private List<Track> trks (byte[] rawBuffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    List<Track> tracks = new ArrayList<> ();
    ptr += 8;

    int reclen = info.wozVersion == 1 ? TRK_SIZE : 8;
    int max = info.wozVersion == 1 ? 35 : 160;

    for (int i = 0; i < max; i++)
    {
      try
      {
        Track trk = new Track (i, rawBuffer, ptr);
        if (trk.bitCount == 0)
          break;
        tracks.add (trk);
        if (showTracks)
          System.out.printf ("%n$%02X  %s%n", i, trk);
      }
      catch (DiskNibbleException e)
      {
        e.printStackTrace ();
      }
      ptr += reclen;
    }
    return tracks;
  }

  // ---------------------------------------------------------------------------------//
  private int val8 (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    return (buffer[ptr] & 0xFF);
  }

  // ---------------------------------------------------------------------------------//
  private int val16 (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    return (buffer[ptr] & 0xFF)                 //
        | ((buffer[ptr + 1] & 0xFF) << 8);
  }

  // ---------------------------------------------------------------------------------//
  private int val32 (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    return (buffer[ptr] & 0xFF)                 //
        | ((buffer[ptr + 1] & 0xFF) << 8)       //
        | ((buffer[ptr + 2] & 0xFF) << 16)      //
        | ((buffer[ptr + 3] & 0xFF) << 24);
  }

  // ---------------------------------------------------------------------------------//
  private int decode4and4 (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    int odds = ((buffer[offset] & 0xFF) << 1) | 0x01;
    int evens = buffer[offset + 1] & 0xFF;
    return odds & evens;
  }

  // ---------------------------------------------------------------------------------//
  private static byte[] readFile (File file)
  // ---------------------------------------------------------------------------------//
  {
    try (BufferedInputStream in = new BufferedInputStream (new FileInputStream (file)))
    {
      return in.readAllBytes ();
    }
    catch (IOException e)
    {
      e.printStackTrace ();
      return null;
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    if (meta != null)
      return info.toString () + "\n\n" + meta.toString ();
    return info.toString ();
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args)
  // ---------------------------------------------------------------------------------//
  {
    String home = "/Users/denismolony/";
    String wozBase1 = home + "Dropbox/Examples/woz test images/WOZ 1.0/";
    String wozBase2 = home + "Dropbox/Examples/woz test images/WOZ 2.0/";
    String wozBase3 = home + "Dropbox/Examples/woz test images/WOZ 2.0/3.5/";
    File[] files = { new File (home + "code/python/wozardry-2.0/bill.woz"),
        new File (wozBase2 + "DOS 3.3 System Master.woz"),
        new File (wozBase1 + "DOS 3.3 System Master.woz"),
        new File (wozBase3 + "Apple IIgs System Disk 1.1.woz") };
    try
    {
      new WozFile (readFile (files[3]));
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
  }

  // ---------------------------------------------------------------------------------//
  class Info
  // ---------------------------------------------------------------------------------//
  {
    int wozVersion;
    int diskType;
    int writeProtected;
    int synchronised;
    int cleaned;
    String creator;

    int sides;
    int bootSectorFormat;
    int optimalBitTiming;
    int compatibleHardware;
    int requiredRam;
    int largestTrack;

    // ---------------------------------------------------------------------------------//
    Info (byte[] buffer, int ptr)
    // ---------------------------------------------------------------------------------//
    {
      wozVersion = val8 (buffer, ptr + 8);

      diskType = val8 (buffer, ptr + 9);
      writeProtected = val8 (buffer, ptr + 10);
      synchronised = val8 (buffer, ptr + 11);
      cleaned = val8 (buffer, ptr + 12);
      creator = new String (buffer, ptr + 13, 32);

      if (wozVersion >= 2)
      {
        sides = val8 (buffer, ptr + 45);
        bootSectorFormat = val8 (buffer, ptr + 46);
        optimalBitTiming = val8 (buffer, ptr + 47);
        compatibleHardware = val16 (buffer, ptr + 48);
        requiredRam = val16 (buffer, ptr + 50);
        largestTrack = val16 (buffer, ptr + 52);
      }

      if (debug1)
        System.out.println (this);
    }

    // ---------------------------------------------------------------------------------//
    @Override
    public String toString ()
    // ---------------------------------------------------------------------------------//
    {
      StringBuilder text = new StringBuilder ("WOZ info:\n\n");

      String diskTypeText = diskType == 1 ? "5.25" : "3.5";

      formatText (text, "Version", 2, wozVersion);
      formatText (text, "Disk type", 2, diskType, diskTypeText);
      formatText (text, "Write protected", 2, writeProtected);
      formatText (text, "Synchronized", 2, synchronised);
      formatText (text, "Cleaned", 2, cleaned);
      formatText (text, "Creator", creator);

      if (wozVersion > 1)
      {
        String bootSectorFormatText =
            bootSectorFormat == 0 ? "Unknown" : bootSectorFormat == 1 ? "16 sector"
                : bootSectorFormat == 2 ? "13 sector" : "Hybrid";

        formatText (text, "Sides", 2, sides);
        formatText (text, "Boot sector format", 2, bootSectorFormat,
            bootSectorFormatText);
        formatText (text, "Optimal bit timing", 2, optimalBitTiming);
        formatText (text, "Compatible hardware", 2, compatibleHardware);
        formatText (text, "Required RAM", 2, requiredRam);
        formatText (text, "Largest track", 2, largestTrack);
      }

      return Utility.rtrim (text);
    }
  }

  // -----------------------------------------------------------------------------------//
  class Meta
  // -----------------------------------------------------------------------------------//
  {
    List<String> lines = new ArrayList<> ();

    // ---------------------------------------------------------------------------------//
    Meta (byte[] buffer, int ptr, int length)
    // ---------------------------------------------------------------------------------//
    {
      String dots = " ......................";
      String metaData = new String (buffer, ptr + 8, length);
      String[] chunks = metaData.split ("\n");
      for (String chunk : chunks)
      {
        String[] parts = chunk.split ("\t");
        if (parts.length >= 2)
          lines.add (String.format ("%-21.21s %s", parts[0] + dots, parts[1]));
        else
          lines.add (String.format ("%-21.21s", parts[0] + dots));
      }
    }

    // ---------------------------------------------------------------------------------//
    @Override
    public String toString ()
    // ---------------------------------------------------------------------------------//
    {
      StringBuilder text = new StringBuilder ("WOZ meta:\n\n");

      for (String line : lines)
        text.append (String.format ("%s%n", line));

      if (text.length () > 0)
        text.deleteCharAt (text.length () - 1);

      return text.toString ();
    }
  }

  // -----------------------------------------------------------------------------------//
  class Track implements Iterable<Sector>
  // -----------------------------------------------------------------------------------//
  {
    private int trackNo;
    private int startingBlock;
    private int blockCount;        // WOZ2
    private int bitCount;
    private int bytesUsed;         // WOZ1

    private byte[] rawBuffer;
    private byte[] newBuffer;

    private int bitIndex;
    private int byteIndex;
    private int trackIndex;
    private int revolutions;

    List<Sector> sectors = new ArrayList<> ();

    // ---------------------------------------------------------------------------------//
    public Track (int trackNo, byte[] rawBuffer, int ptr) throws DiskNibbleException
    // ---------------------------------------------------------------------------------//
    {
      this.rawBuffer = rawBuffer;
      this.trackNo = trackNo;

      if (info.wozVersion == 1)
      {
        bytesUsed = val16 (rawBuffer, ptr + DATA_SIZE);
        bitCount = val16 (rawBuffer, ptr + DATA_SIZE + 2);

        if (debug1)
          System.out.println (
              (String.format ("Bytes: %2d,  Bits: %,8d%n%n", bytesUsed, bitCount)));
      }
      else
      {
        startingBlock = val16 (rawBuffer, ptr);
        blockCount = val16 (rawBuffer, ptr + 2);
        bitCount = val32 (rawBuffer, ptr + 4);

        if (debug1)
          System.out.println ((String.format ("%nStart: %4d,  Blocks: %2d,  Bits: %,8d%n",
              startingBlock, blockCount, bitCount)));
      }

      if (bitCount == 0)
        return;

      resetIndex ();

      if (addressPrologue == null)                                 // WOZ1
        if (findNext (address16prologue, ptr) > 0)
          setPrologue (16);
        else if (findNext (address13prologue, ptr) > 0)
          setPrologue (13);
        else
          throw new DiskNibbleException ("No address prologue found");

      int offset = -1;

      while (sectors.size () < diskSectors)
      {
        offset = findNext (addressPrologue, offset + 1);
        if (offset < 0)
          break;

        Sector sector = new Sector (this, offset);
        if (isDuplicate (sector))
          break;
        sectors.add (sector);
      }
    }

    // ---------------------------------------------------------------------------------//
    private boolean isDuplicate (Sector newSector)
    // ---------------------------------------------------------------------------------//
    {
      for (Sector sector : sectors)
        if (sector.sectorNo == newSector.sectorNo)
          return true;

      return false;
    }

    // ---------------------------------------------------------------------------------//
    private void resetIndex ()
    // ---------------------------------------------------------------------------------//
    {
      trackIndex = 0;
      bitIndex = 0;

      if (info.wozVersion == 1)
        byteIndex = 256 + trackNo * TRK_SIZE;
      else
        byteIndex = startingBlock * BLOCK_SIZE;
    }

    // ---------------------------------------------------------------------------------//
    boolean nextBit ()
    // ---------------------------------------------------------------------------------//
    {
      boolean bit = (rawBuffer[byteIndex] & (0x80 >>> bitIndex)) != 0;

      if (++trackIndex >= bitCount)
      {
        ++revolutions;
        resetIndex ();
      }
      else if (++bitIndex >= 8)
      {
        ++byteIndex;
        bitIndex = 0;
      }

      return bit;
    }

    // ---------------------------------------------------------------------------------//
    int nextByte ()
    // ---------------------------------------------------------------------------------//
    {
      byte b = 0;
      while ((b & 0x80) == 0)
      {
        b <<= 1;
        if (nextBit ())
          b |= 0x01;
      }

      return b;
    }

    // ---------------------------------------------------------------------------------//
    void readTrack ()
    // ---------------------------------------------------------------------------------//
    {
      if (newBuffer != null)
        return;

      int max = (bitCount - 1) / 8 + 1;
      max += 600;
      newBuffer = new byte[max];

      for (int i = 0; i < max; i++)
        newBuffer[i] = (byte) nextByte ();
    }

    // ---------------------------------------------------------------------------------//
    int findNext (byte[] key, int start)
    // ---------------------------------------------------------------------------------//
    {
      readTrack ();

      int max = newBuffer.length - key.length;
      outer: for (int ptr = start; ptr < max; ptr++)
      {
        for (int keyPtr = 0; keyPtr < key.length; keyPtr++)
          if (newBuffer[ptr + keyPtr] != key[keyPtr])
            continue outer;
        return ptr;
      }

      return -1;
    }

    // ---------------------------------------------------------------------------------//
    void pack (byte[] diskBuffer) throws DiskNibbleException
    // ---------------------------------------------------------------------------------//
    {
      int ndx = diskSectors == 13 ? 0 : 1;
      DiskReader diskReader = DiskReader.getInstance (diskSectors);

      for (Sector sector : sectors)
        if (sector.dataOffset > 0)
        {
          byte[] decodedBuffer =
              diskReader.decodeSector (newBuffer, sector.dataOffset + 3);
          int ptr = SECTOR_SIZE
              * (sector.trackNo * diskSectors + interleave[ndx][sector.sectorNo]);
          System.arraycopy (decodedBuffer, 0, diskBuffer, ptr, decodedBuffer.length);
        }
    }

    // ---------------------------------------------------------------------------------//
    @Override
    public String toString ()
    // ---------------------------------------------------------------------------------//
    {
      StringBuilder text = new StringBuilder ();

      if (info.wozVersion == 1)
        text.append (
            String.format ("WOZ1: Bytes: %2d,  Bits: %,8d%n%n", bytesUsed, bitCount));
      else
        text.append (String.format ("WOZ2: Start: %4d,  Blocks: %2d,  Bits: %,8d%n%n",
            startingBlock, blockCount, bitCount));

      int count = 0;
      for (Sector sector : sectors)
        text.append (String.format ("%2d  %s%n", count++, sector));
      text.deleteCharAt (text.length () - 1);

      return text.toString ();
    }

    // ---------------------------------------------------------------------------------//
    @Override
    public Iterator<Sector> iterator ()
    // ---------------------------------------------------------------------------------//
    {
      return sectors.iterator ();
    }
  }

  // ---------------------------------------------------------------------------------//
  public class Sector implements Comparable<Sector>
  // ---------------------------------------------------------------------------------//
  {
    private final Track track;
    private int trackNo, sectorNo, volume, checksum;
    private final int addressOffset;
    private int dataOffset;

    // ---------------------------------------------------------------------------------//
    Sector (Track track, int addressOffset)
    // ---------------------------------------------------------------------------------//
    {
      this.track = track;

      if (info.diskType == 1)
      {
        volume = decode4and4 (track.newBuffer, addressOffset + 3);
        trackNo = decode4and4 (track.newBuffer, addressOffset + 5);
        sectorNo = decode4and4 (track.newBuffer, addressOffset + 7);
        checksum = decode4and4 (track.newBuffer, addressOffset + 9);
      }
      else
      {
        // http://apple2.guidero.us/doku.php/articles/iicplus_smartport_secrets
        // SWIM Chip User's Ref pp 6
        // uPD72070.pdf
        try
        {
          int b1 = byteTranslator6and2.decode (track.newBuffer[addressOffset + 3]);
          sectorNo = byteTranslator6and2.decode (track.newBuffer[addressOffset + 4]);
          int b3 = byteTranslator6and2.decode (track.newBuffer[addressOffset + 5]);
          int format = byteTranslator6and2.decode (track.newBuffer[addressOffset + 6]);
          checksum = byteTranslator6and2.decode (track.newBuffer[addressOffset + 7]);

          trackNo = (b1 & 0x3F) | ((b3 & 0x1F) << 6);
          volume = (b3 & 0x20) >>> 5;       // side

          int chk = b1 ^ sectorNo ^ b3 ^ format;
          assert chk == checksum;
        }
        catch (DiskNibbleException e)
        {
          e.printStackTrace ();
        }
      }

      //      int epiloguePtr = track.findNext (epilogue, addressOffset + 11);
      //      assert epiloguePtr == addressOffset + 11;

      this.addressOffset = addressOffset;
      dataOffset = track.findNext (dataPrologue, addressOffset + 11);
      if (dataOffset > addressOffset + 200)
        dataOffset = -1;
    }

    // ---------------------------------------------------------------------------------//
    void pack35 (byte[] diskBuffer, int ptr) throws DiskNibbleException
    // ---------------------------------------------------------------------------------//
    {
      DiskReader diskReader = DiskReader.getInstance (0);

      // start decoding from 4 bytes past the data prologue (3 bytes for prologue
      // itself, and another byte for the sector number)
      byte[] decodedBuffer = diskReader.decodeSector (track.newBuffer, dataOffset + 4);

      // return 512 bytes (ignore the 12 tag bytes)
      System.arraycopy (decodedBuffer, DiskReaderGCR.TAG_SIZE, diskBuffer, ptr, 512);
    }

    // ---------------------------------------------------------------------------------//
    @Override
    public String toString ()
    // ---------------------------------------------------------------------------------//
    {
      String fld = info.diskType == 1 ? "Vol" : info.diskType == 2 ? "Sde" : "???";
      String dataOffsetText = dataOffset < 0 ? "" : String.format ("%04X", dataOffset);

      return String.format (
          "%s: %02X  Trk: %02X  Sct: %02X  Chk: %02X  Add: %04X  Dat: %s", fld, volume,
          trackNo, sectorNo, checksum, addressOffset, dataOffsetText);
    }

    // ---------------------------------------------------------------------------------//
    @Override
    public int compareTo (Sector o)
    // ---------------------------------------------------------------------------------//
    {
      if (this.trackNo != o.trackNo)
        return this.trackNo - o.trackNo;
      if (this.volume != o.volume)              // side
        return this.volume - o.volume;
      return this.sectorNo - o.sectorNo;
    }
  }
}
