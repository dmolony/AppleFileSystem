package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileDosMaster extends AbstractAppleFile implements AppleContainer
// -----------------------------------------------------------------------------------//
{
  private static final int MAX_PARTITIONS = 8;

  int[] slot = new int[MAX_PARTITIONS];
  int[] drive = new int[MAX_PARTITIONS];
  int[] partitionStart = new int[MAX_PARTITIONS];
  int[] blocks = new int[MAX_PARTITIONS];
  int[] partitionEnd = new int[MAX_PARTITIONS];
  int[] volumes = new int[MAX_PARTITIONS];

  protected List<AppleFileSystem> fileSystems = new ArrayList<> ();
  protected List<AppleFile> files = new ArrayList<> ();
  protected List<AppleBlock> indexBlocks = new ArrayList<> ();
  protected String helloProgram = "";

  byte[] dos33Buffer;

  boolean debug = false;

  // ---------------------------------------------------------------------------------//
  FileDosMaster (FsProdos parentFileSystem, AppleBlock dos33FirstBlock)
  // ---------------------------------------------------------------------------------//
  {
    super (parentFileSystem);

    BlockReader br = parentFileSystem.getBlockReader ();

    byte[] diskBuffer = br.getDiskBuffer ().data ();
    int dataPtr = br.getDiskBuffer ().offset ();

    indexBlocks.add (dos33FirstBlock);

    dos33Buffer = dos33FirstBlock.getBuffer ();
    helloProgram = Utility.getPascalString (dos33Buffer, 6);
    analyse (dos33Buffer);

    for (int i = 0; i < MAX_PARTITIONS; i++)
    {
      if (slot[i] == 0 || volumes[i] == 0)
        continue;

      int offset = (partitionStart[i] + blocks[i]) * ProdosConstants.BLOCK_SIZE;
      int diskLength = blocks[i] * ProdosConstants.BLOCK_SIZE;      // in bytes

      addBlocks (i, br);

      for (int vol = 0; vol < volumes[i]; vol++)
      {
        if (debug)
          System.out.printf ("Slot %d : ", vol);

        String name = String.format ("S%d D%d Volume %d", slot[i], drive[i], (vol + 1));
        BlockReader slotReader = new BlockReader (name, diskBuffer,
            dataPtr + offset + vol * diskLength, diskLength);
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
      for (AppleFileSystem afs : getFileSystems ())
        System.out.println (afs.getBlockReader ());

    isFolder = true;
  }

  // ---------------------------------------------------------------------------------//
  private void addBlocks (int i, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    int blockNo = partitionStart[i] + blocks[i];
    int max = blocks[i] * volumes[i];

    for (int block = 0; block < max; block++)
    {
      AppleBlock appleBlock = blockReader.getBlock (parentFileSystem, blockNo++);

      if (dataBlocks.contains (appleBlock))
        break;

      appleBlock.setBlockType (BlockType.FILE_DATA);
      dataBlocks.add (appleBlock);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return "DosMaster";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return "DIR";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return true;
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
      //      int adrs = buffer[ptr + 40 + i] & 0xFF;
      //      int adrs2 = Utility.unsignedShort (buffer, ptr + 40 + ndx);
      //      System.out.printf ("%02X  %<d%n", adrs);
      //      System.out.printf ("%04X  %<d%n", adrs2);

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
    return "";
  }

  // ---------------------------------------------------------------------------------//
  public String getDebugText ()
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

    text.append ("\n");
    text.append (Utility.format (dos33Buffer, 3, 101));
    text.append ("\n\n");

    for (int i = 3; i < 6; i += 2)
      text.append (String.format ("%3d : %04X  %<,6d %n", i,
          Utility.unsignedShort (dos33Buffer, i)));

    text.append ("\n");

    text.append (Utility.getPascalString (dos33Buffer, 6));       // hello text
    text.append ("\n\n");

    for (int i = 38; i < 104; i += 2)
    {
      if (i == 0x38 || i == 0x40 || i == 0x50 || i == 0x58 || i == 0x60)
        text.append ("\n");
      text.append (String.format ("%02X : %04X  %<,6d %n", i,
          Utility.unsignedShort (dos33Buffer, i)));
    }

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  public Optional<AppleFile> getHelloProgram ()
  // ---------------------------------------------------------------------------------//
  {
    if (!helloProgram.isEmpty () && fileSystems.size () > 0)
      return fileSystems.get (0).getFile (helloProgram);

    return Optional.empty ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "addFile() not implemented in " + getFileName ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()
  // ---------------------------------------------------------------------------------//
  {
    return files;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Optional<AppleFile> getFile (String fileName)
  // ---------------------------------------------------------------------------------//
  {
    return Optional.empty ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFileSystem (AppleFileSystem fileSystem)
  // ---------------------------------------------------------------------------------//
  {
    fileSystems.add (fileSystem);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFileSystem> getFileSystems ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystems;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return parentFileSystem.getPath () + "/" + getFileName ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void sort ()
  // ---------------------------------------------------------------------------------//
  {
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
