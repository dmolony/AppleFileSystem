package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import java.util.List;

import com.bytezone.utility.Utility;

class FsDosMaster extends AbstractFileSystem
{
  int ptr;

  int[] slot = new int[8];
  int[] drive = new int[8];
  int[] volStart = new int[8];
  int[] sectors = new int[8];
  int[] volEnd = new int[8];
  int[] volumes = new int[8];

  boolean debug = false;

  // ---------------------------------------------------------------------------------//
  FsDosMaster (BlockReader diskReader, AppleFile dos33)
  // ---------------------------------------------------------------------------------//
  {
    super (diskReader, FileSystemType.HYBRID);

    byte[] diskBuffer = diskReader.getDiskBuffer ().data ();

    List<AppleBlock> dos33Blocks = dos33.getDataBlocks ();
    byte[] buffer2 = dos33Blocks.get (0).getBuffer ();

    analyse (buffer2);

    int offset = 560 * 512;                     // always seems to start in block 560
    int partitionSize = 560;                    // 560 / 640 / 800 / 1600 blocks
    int diskLength = partitionSize * 256;

    for (int slot = 0; slot < 110; slot++)
    {
      if (debug)
        System.out.printf ("Slot %d : ", slot);

      BlockReader slotReader = new BlockReader ("Volume " + (slot + 1), diskBuffer,
          offset + slot * diskLength, diskLength);
      slotReader.setParameters (FileSystemFactory.dos1);

      try
      {
        FsDos3 fs = new FsDos3 (slotReader);

        if (fs != null)
        {
          fs.readCatalogBlocks ();
          if (fs.getFiles ().size () > 0)
            addFileSystem (fs);

          if (debug)
            System.out.printf ("found %s  files : %d%n", fs.fileSystemType,
                fs.files.size ());
        }
        else if (debug)
          System.out.println ("not found");
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
    }

    if (debug)
      for (AppleFileSystem afs : fileSystems)
      {
        System.out.println (afs.getBlockReader ());
      }
  }

  // Based on REVISE.DM
  // ---------------------------------------------------------------------------------//
  private void analyse (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    //    byte[] copyright = Utility.setHiBits ("Copyright 1988 by Glen Bredon");
    //    int pos = Utility.find (buffer, copyright);
    //    if (pos < 0)
    //      return;

    //    System.out.println (Utility.format (buffer, 0, 100));

    int slots = 3 * 16 + 8;         // offset from load address 0x2000
    int v0 = slots + 8;
    int size = v0 + 16;
    int vsize = size + 8;
    int adrs = vsize + 8;

    for (int d = 0; d < 8; d++)
    {
      int d0 = d / 2 * 2;
      int s = buffer[slots + d] & 0xFF;
      if (s == 0)
        continue;

      int dr = 0;
      if (s >= 128)
      {
        s -= 128;
        dr = 1;
      }

      int ptr = v0 + 2 * d0 + 2 * dr;
      int st = Utility.unsignedShort (buffer, ptr);
      int sz = Utility.unsignedShort (buffer, vsize + d0);
      int v = Utility.unsignedShort (buffer, size + d0);

      if (st > v)
      {
        if (debug)
          System.out.print ("xx");
        st -= 16 * 4096;
      }

      int num = (v - st) / sz - 1;

      slot[d] = s / 16;
      volStart[d] = st;
      volEnd[d] = v;
      sectors[d] = sz;
      volumes[d] = (v - st) / sz - 1;
      drive[d] = dr;

      if (debug)
      {
        System.out.printf ("Ptr           %d%n", ptr);
        System.out.printf ("Slot          %d  (/16 = %d)%n", s, s / 16);
        System.out.printf ("Drive         %d%n", dr);
        System.out.printf ("Volume Start  %d  (*2 = %d)%n", st, st * 2);
        System.out.printf ("Disk Sectors  %d  (*2 = %d)%n", sz, sz * 2);
        System.out.printf ("Volume End    %d%n", v);
        System.out.printf ("Volumes       %d  ((%d - %d) / %d - 1)%n", num, v, st, sz);
      }

      if (debug)
        System.out.printf ("%nSlot %d, drive %d has %3d volumes of %,d sectors%n%n",
            s / 16, dr + 1, num, sz * 2);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("----- DOS Master ------\n");

    for (int d = 0; d < 8; d++)
      if (slot[d] > 0)
      {
        formatText (text, "Partition", 2, d);
        formatText (text, "Slot", 2, slot[d]);
        formatText (text, "Drive", 2, drive[d]);
        formatText (text, "Volumes", 2, volumes[d]);
        formatText (text, "Vol start", 4, volStart[d]);
        formatText (text, "Vol end", 4, volEnd[d]);
        formatText (text, "Sectors", 4, sectors[d]);
        text.append ("\n");
      }

    return Utility.rtrim (text);
  }
}
