package com.bytezone.filesystem;

import com.bytezone.filesystem.BlockReader.AddressType;
import com.bytezone.utility.Utility;

class FsDosMaster extends AbstractFileSystem
{
  // ---------------------------------------------------------------------------------//
  FsDosMaster (BlockReader diskReader, BlockReader fileReader)
  // ---------------------------------------------------------------------------------//
  {
    super (diskReader, FileSystemType.HYBRID);

    byte[] buffer = fileReader.getDiskBuffer ().data ();
    byte[] diskBuffer = diskReader.getDiskBuffer ().data ();

    //    System.out.printf ("Disk length: %,d%n", diskBuffer.length);
    //    System.out.printf ("File length: %,d%n", buffer.length);

    byte[] copyright = Utility.setHiBits ("Copyright 1988 by Glen Bredon");
    int pos = Utility.find (buffer, copyright);
    if (pos < 0)
      return;

    //    System.out.println (Utility.format (buffer, pos, copyright.length));

    int slots = 3 * 16 + 8;
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

      //      System.out.printf ("Slot %d, drive %d has ", s / 16, dr + 1);

      int ptr = v0 + 2 * d0 + 2 * dr;
      int st = Utility.unsignedShort (buffer, ptr);
      int sz = Utility.unsignedShort (buffer, vsize + d0);
      int v = Utility.unsignedShort (buffer, size + d0);

      if (st > v)
      {
        System.out.println ("xx");
        st -= 16 * 4096;
      }
      int num = (v - st) / sz - 1;
      //      System.out.printf ("%3d volumes of %4d sectors%n", num, sz * 2);
    }

    for (int slot = 0; slot < 15; slot++)
    {
      BlockReader slotReader =
          new BlockReader ("Slot " + slot, diskBuffer, 286_720 + slot * 143360, 143360);
      slotReader.setParameters (256, AddressType.SECTOR, 0, 16);

      try
      {
        FsDos3 fs = new FsDos3 (slotReader);
        if (fs != null && fs.files.size () > 0)
          addFileSystem (fs);
      }
      catch (FileFormatException e)
      {
        //        System.out.println (e);
      }
    }
  }
}
