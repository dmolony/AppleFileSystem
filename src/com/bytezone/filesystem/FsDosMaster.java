package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

class FsDosMaster extends AbstractFileSystem
{
  boolean debug = false;

  // ---------------------------------------------------------------------------------//
  FsDosMaster (BlockReader diskReader)
  // ---------------------------------------------------------------------------------//
  {
    super (diskReader, FileSystemType.HYBRID);

    byte[] diskBuffer = diskReader.getDiskBuffer ().data ();

    int offset = 560 * 512;                     // always seems to start in block 560
    int partitionSize = 560;                    // 560 / 640 / 800 / 1600 blocks
    int diskLength = partitionSize * 256;

    for (int slot = 0; slot < 100; slot++)
    {
      if (debug)
        System.out.printf ("Slot %d : ", slot);

      BlockReader slotReader = new BlockReader ("Slot " + slot, diskBuffer,
          offset + slot * diskLength, diskLength);
      slotReader.setParameters (FileSystemFactory.dos1);

      try
      {
        FsDos3 fs = new FsDos3 (slotReader);
        if (fs != null)
        {
          addFileSystem (fs);
          fs.readCatalogBlocks ();

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

  // I never got this working correctly, and it doesn't seem to be needed anyway. It
  // uses the contents of file DOS.3.3.
  // ---------------------------------------------------------------------------------//
  private void analyse (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    byte[] copyright = Utility.setHiBits ("Copyright 1988 by Glen Bredon");
    int pos = Utility.find (buffer, copyright);
    if (pos < 0)
      return;

    int slots = 3 * 16 + 8;
    //    int slots = 2 * 4096 + 3 * 16 + 8;    // this goes weird
    int v0 = slots + 8;
    int size = v0 + 16;
    int vsize = size + 8;
    int adrs = vsize + 8;

    for (int d = 0; d < 8; d++)
    {
      int d0 = d / 2;
      int s = buffer[slots + d] & 0xFF;
      if (s == 0)
        continue;
      int dr = 0;
      if (s >= 128)
      {
        s -= 128;
        dr = 1;
      }

      if (debug)
        System.out.printf ("Slot %d, drive %d has ", s / 16, dr + 1);

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
      if (debug)
        System.out.printf ("%3d volumes of %4d sectors%n", num, sz * 2);
    }
  }
}
