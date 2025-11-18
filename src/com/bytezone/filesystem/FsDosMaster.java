package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class FsDosMaster extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final int MAX_PARTITIONS = 8;

  int[] slot = new int[MAX_PARTITIONS];
  int[] drive = new int[MAX_PARTITIONS];
  int[] partitionStart = new int[MAX_PARTITIONS];
  int[] blocks = new int[MAX_PARTITIONS];
  int[] partitionEnd = new int[MAX_PARTITIONS];
  int[] volumes = new int[MAX_PARTITIONS];

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

    for (int i = 0; i < MAX_PARTITIONS; i++)
    {
      if (slot[i] == 0 || volumes[i] == 0)
        continue;

      int offset = (partitionStart[i] + blocks[i]) * ProdosConstants.BLOCK_SIZE;
      int diskLength = blocks[i] * ProdosConstants.BLOCK_SIZE;

      for (int vol = 0; vol < volumes[i]; vol++)
      {
        if (debug)
          System.out.printf ("Slot %d : ", vol);

        String name = String.format ("S%d D%d Volume %d", slot[i], drive[i], (vol + 1));
        BlockReader slotReader =
            new BlockReader (name, diskBuffer, offset + vol * diskLength, diskLength);
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
    }

    if (debug)
      for (AppleFileSystem afs : fileSystems)
        System.out.println (afs.getBlockReader ());
  }

  // Based on REVISE.DM
  // ---------------------------------------------------------------------------------//
  private void analyse (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    int ptr = 0x38;

    if (debug)
      System.out.println ("#  S  D  ?   start     end   blocks  sectors  volumes");

    for (int i = 0; i < MAX_PARTITIONS; i++)        // 4 slots * 2 drives
    {
      slot[i] = (buffer[ptr + i] & 0x70) >>> 4;     // ignore the drive bit
      if (slot[i] == 0)
        continue;

      drive[i] = i % 2;
      int ndx = i / 2 * 2;                          // 0, 0, 2, 2, 4, 4, 6, 6

      partitionStart[i] = Utility.unsignedShort (buffer, ptr + 8 + i * 2);
      partitionEnd[i] = Utility.unsignedShort (buffer, ptr + 24 + ndx);
      blocks[i] = Utility.unsignedShort (buffer, ptr + 32 + ndx);

      if (partitionStart[i] > partitionEnd[i])      // no idea
        partitionStart[i] -= 0x10000;

      volumes[i] = (partitionEnd[i] - partitionStart[i]) / blocks[i] - 1;

      if (debug)
        System.out.printf ("%d  %d  %d  %d  %,6d  %,6d  %,6d   %,6d    %,4d %n", i,
            slot[i], drive[i], ndx, partitionStart[i], partitionEnd[i], blocks[i],
            blocks[i] * 2, volumes[i]);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("#  S  D    start     end   blocks  sectors  volumes\n");

    for (int i = 0; i < MAX_PARTITIONS; i++)
    {
      if (slot[i] == 0)
        break;

      text.append (String.format ("%d  %d  %d   %,6d  %,6d  %,6d   %,6d    %,4d %n", i,
          slot[i], drive[i], partitionStart[i], partitionEnd[i], blocks[i], blocks[i] * 2,
          volumes[i]));
    }

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("----- DOS Master ------\n");

    for (int i = 0; i < MAX_PARTITIONS; i++)
      if (slot[i] > 0)
      {
        formatText (text, "Partition", 2, i);
        formatText (text, "Slot", 2, slot[i]);
        formatText (text, "Drive", 2, drive[i]);
        formatText (text, "Volumes", 2, volumes[i]);
        formatText (text, "Vol start", 4, partitionStart[i]);
        formatText (text, "Vol end", 4, partitionEnd[i]);

        String sectors = String.format ("(%d sectors)", blocks[i] * 2);
        formatText (text, "Blocks", 4, blocks[i], sectors);
        text.append ("\n");
      }

    return Utility.rtrim (text);
  }
}
