package com.bytezone.filesystem;

import static com.bytezone.filesystem.BlockReader.AddressType.BLOCK;
import static com.bytezone.filesystem.BlockReader.AddressType.SECTOR;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.nufx.NuFX;
import com.bytezone.woz.DiskNibbleException;
import com.bytezone.woz.WozFile;

// -----------------------------------------------------------------------------------//
public class FileSystemFactory
// -----------------------------------------------------------------------------------//
{
  private static final byte[] NuFile = { 0x4E, (byte) 0xF5, 0x46, (byte) 0xE9, 0x6C, (byte) 0xE5 };
  private static final byte[] BIN2 = { 0x0A, 0x47, 0x4C };
  private static final byte[] TWO_IMG = { 0x32, 0x49, 0x4D, 0x47 };
  private static final byte[] WOZ_1 = { 0x57, 0x4F, 0x5A, 0x32, (byte) 0xFF, 0x0A, 0x0D, 0x0A };
  private static final byte[] WOZ_2 = { 0x57, 0x4F, 0x5A, 0x31, (byte) 0xFF, 0x0A, 0x0D, 0x0A };
  private static final int UNIDOS_SIZE = 409_600;

  private static String[] twoIMGFormats = { "Dos", "Prodos", "NIB" };

  static BlockReader blockReader0 = new BlockReader (512, BLOCK, 0, 0);     // Prodos
  static BlockReader blockReader1 = new BlockReader (512, BLOCK, 1, 8);     // Prodos
  static BlockReader cpmReader = new BlockReader (1024, BLOCK, 2, 4);       // CPM
  static BlockReader dos31Reader = new BlockReader (256, SECTOR, 0, 13);    // Dos 3.1
  static BlockReader dos33Reader0 = new BlockReader (256, SECTOR, 0, 16);   // Dos 3.3
  static BlockReader dos33Reader1 = new BlockReader (256, SECTOR, 1, 16);   // Dos 3.3
  static BlockReader unidosReader = new BlockReader (256, SECTOR, 0, 32);   // UniDos

  static BlockReader[] dos33Readers = { dos33Reader0, dos33Reader1 };
  static BlockReader[] blockReaders = { blockReader0, blockReader1 };

  // ---------------------------------------------------------------------------------//
  private FileSystemFactory ()
  // ---------------------------------------------------------------------------------//
  {
  }

  // ---------------------------------------------------------------------------------//
  public static List<AppleFileSystem> getFileSystems (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    int offset = 0;
    int length = buffer.length;

    if (length == 143_488)
      length = 143_360;

    List<AppleFileSystem> fileSystems = new ArrayList<> ();

    if (Utility.isMagic (buffer, 0, TWO_IMG))
    {
      offset = Utility.unsignedLong (buffer, 24);
      length = Utility.unsignedLong (buffer, 28);

      if (false)
      {
        int headerSize = Utility.unsignedShort (buffer, 8);
        int version = Utility.unsignedShort (buffer, 10);
        int format = Utility.unsignedLong (buffer, 12);
        int prodosBlocks = Utility.unsignedLong (buffer, 20);

        System.out.println ();
        System.out.printf ("Header size ... %d%n", headerSize);
        System.out.printf ("Version ....... %d%n", version);
        System.out.printf ("Format ........ %d  %s%n", format, twoIMGFormats[format]);
        System.out.printf ("Blocks ........ %,d%n", prodosBlocks);
        System.out.printf ("Data offset ... %d%n", offset);
        System.out.printf ("Data size ..... %,d%n", length);
      }
    }
    else if (Utility.isMagic (buffer, 0, NuFile))
    {
      NuFX nufx = new NuFX (buffer, name);
      buffer = nufx.getDiskBuffer ();
      length = buffer.length;
    }
    else if (Utility.isMagic (buffer, 0, WOZ_1) || Utility.isMagic (buffer, 0, WOZ_2))
    {
      try
      {
        buffer = new WozFile (buffer).getDiskBuffer ();
        length = buffer.length;
      }
      catch (DiskNibbleException e)
      {
        e.printStackTrace ();
      }
    }

    assert offset + length <= buffer.length;

    add (fileSystems, getProdos (name, buffer, offset, length));
    add (fileSystems, getPascal (name, buffer, offset, length));
    add (fileSystems, getDos31 (name, buffer, offset, length));
    add (fileSystems, getDos (name, buffer, offset, length));
    add (fileSystems, getDos4 (name, buffer, offset, length));
    add (fileSystems, getCpm (name, buffer, offset, length));
    getUnidos (fileSystems, name, buffer, offset, length);

    return fileSystems;
  }

  // ---------------------------------------------------------------------------------//
  private static void add (List<AppleFileSystem> fileSystems, AppleFileSystem appleFileSystem)
  // ---------------------------------------------------------------------------------//
  {
    if (appleFileSystem != null)
      fileSystems.add (appleFileSystem);
  }

  // ---------------------------------------------------------------------------------//
  static FsDos getDos (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    return getDos (name, buffer, 0, buffer.length);
  }

  // ---------------------------------------------------------------------------------//
  static FsDos getDos (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    List<FsDos> disks = new ArrayList<> (2);

    if (length == 143_360)
      for (BlockReader reader : dos33Readers)
        try
        {
          FsDos fs = new FsDos (name, buffer, offset, length, reader);

          if (fs.getTotalCatalogBlocks () > 0)
            disks.add (fs);
        }
        catch (FileFormatException e)
        {
        }

    if (disks.size () == 0)
      return null;

    if (disks.size () == 1)
      return disks.get (0);

    return disks.get (0).getTotalCatalogBlocks () > disks.get (1).getTotalCatalogBlocks ()
        ? disks.get (0) : disks.get (1);
  }

  // ---------------------------------------------------------------------------------//
  static FsProdos getProdos (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    for (BlockReader reader : blockReaders)
      try
      {
        FsProdos prodos = new FsProdos (name, buffer, offset, length, reader);

        if (prodos.getTotalCatalogBlocks () > 0)
          return prodos;
      }
      catch (FileFormatException e)
      {
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  static FsPascal getPascal (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  // This can be called from FsProdos if a PASCAL_ON_PROFILE is found
  {
    for (BlockReader reader : blockReaders)
      try
      {
        FsPascal pascal = new FsPascal (name, buffer, offset, length, reader);

        if (pascal.getTotalCatalogBlocks () > 0)
          return pascal;
      }
      catch (FileFormatException e)
      {
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  static FsCpm getCpm (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == 143_360)
      try
      {
        FsCpm cpm = new FsCpm (name, buffer, offset, length, cpmReader);

        if (cpm.getTotalCatalogBlocks () > 0)
          return cpm;
      }
      catch (FileFormatException e)
      {
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  static FsDos getDos31 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == 116_480)                  // Dos3.1
      try
      {
        FsDos dos = new FsDos (name, buffer, offset, length, dos31Reader);
        if (dos.getTotalCatalogBlocks () > 0)
          return dos;
      }
      catch (FileFormatException e)
      {
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  static FsDos4 getDos4 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == 143_360)
      try
      {
        FsDos4 dos4 = new FsDos4 (name, buffer, offset, length, dos33Reader0);
        if (dos4.getTotalCatalogBlocks () > 0)
          return dos4;
      }
      catch (FileFormatException e)
      {
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  static void getUnidos (List<AppleFileSystem> fileSystems, String name, byte[] buffer, int offset,
      int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == UNIDOS_SIZE * 2)
      try
      {
        FsDos fs1 = new FsDos (name, buffer, 0, UNIDOS_SIZE, unidosReader);
        if (fs1 != null && fs1.getTotalCatalogBlocks () > 0)
        {
          FsDos fs2 = new FsDos (name, buffer, UNIDOS_SIZE, UNIDOS_SIZE, unidosReader);
          if (fs2 != null && fs2.getTotalCatalogBlocks () > 0)
          {
            fileSystems.add (fs1);
            fileSystems.add (fs2);
          }
        }
      }
      catch (FileFormatException e)
      {
      }
  }
}
