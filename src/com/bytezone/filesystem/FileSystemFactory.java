package com.bytezone.filesystem;

import static com.bytezone.filesystem.BlockReader.AddressType.BLOCK;
import static com.bytezone.filesystem.BlockReader.AddressType.SECTOR;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;
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
  private static final byte[] Crunch = { 0x76, (byte) 0xFE };
  private static final byte[] Squeeze = { 0x76, (byte) 0xFF };

  private static final int UNIDOS_SIZE = 409_600;

  private static String[] twoIMGFormats = { "Dos", "Prodos", "NIB" };

  static BlockReader blockReader0 = new BlockReader (512, BLOCK, 0, 0);     // Prodos
  static BlockReader blockReader1 = new BlockReader (512, BLOCK, 1, 8);     // Prodos
  static BlockReader cpmReader = new BlockReader (1024, BLOCK, 2, 4);       // CPM
  static BlockReader dos31Reader = new BlockReader (256, SECTOR, 0, 13);    // Dos 3.1
  static BlockReader dos33Reader0 = new BlockReader (256, SECTOR, 0, 16);   // Dos 3.3
  static BlockReader dos33Reader1 = new BlockReader (256, SECTOR, 1, 16);   // Dos 3.3
  static BlockReader unidosReader = new BlockReader (256, SECTOR, 0, 32);   // UniDos
  static BlockReader lbrReader = new BlockReader (128, BLOCK, 0, 0);        // LBR

  static BlockReader[] dos33Readers = { dos33Reader0, dos33Reader1 };
  static BlockReader[] blockReaders = { blockReader0, blockReader1 };

  List<AppleFileSystem> fileSystems = new ArrayList<> ();
  private boolean display = false;

  // ---------------------------------------------------------------------------------//
  public List<AppleFileSystem> getFileSystems (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    return getFileSystems (file.getName (), file.read ());
  }

  // ---------------------------------------------------------------------------------//
  public List<AppleFileSystem> getFileSystems (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    fileSystems.clear ();

    int offset = 0;
    int length = buffer.length;

    if (length == 143_488)
      length = 143_360;

    if (Utility.isMagic (buffer, 0, TWO_IMG))
    {
      offset = Utility.unsignedLong (buffer, 24);
      length = Utility.unsignedLong (buffer, 28);

      if (display)
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
    //    else if (Utility.isMagic (buffer, 0, BIN2) && buffer[18] == 0x02)
    //    {
    //      String id = new String (buffer, 1, 2);
    //      System.out.println ("Binary II : " + id);
    //    }
    //    else if (Utility.isMagic (buffer, 0, NuFile))
    //    {
    //      NuFX nufx = new NuFX (buffer, name);
    //      buffer = nufx.getDiskBuffer ();
    //      length = buffer.length;
    //
    //      if (display)
    //      {
    //        System.out.println ();
    //        System.out.println (nufx);
    //      }
    //    }
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
    else if (Utility.isMagic (buffer, 0, Squeeze))
    {
      System.out.println ("Squeeze?");
    }

    assert offset + length <= buffer.length;

    add (getProdos (name, buffer, offset, length));
    add (getPascal (name, buffer, offset, length));
    add (getDos31 (name, buffer, offset, length));
    add (getDos (name, buffer, offset, length));
    add (getDos4 (name, buffer, offset, length));
    add (getCpm (name, buffer, offset, length));
    add (getLbr (name, buffer, offset, length));
    add (getNuFx (name, buffer, offset, length));
    add (getBinary2 (name, buffer, offset, length));
    getUnidos (name, buffer, offset, length);

    return fileSystems;
  }

  // ---------------------------------------------------------------------------------//
  //  private boolean count (byte[] buffer, byte value, int offset, int length)
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    while (length > 0 && offset < buffer.length && buffer[offset] == value)
  //    {
  //      ++offset;
  //      --length;
  //    }
  //
  //    return length == 0;
  //  }

  // ---------------------------------------------------------------------------------//
  private void add (AppleFileSystem appleFileSystem)
  // ---------------------------------------------------------------------------------//
  {
    if (appleFileSystem != null)
      fileSystems.add (appleFileSystem);
  }

  // ---------------------------------------------------------------------------------//
  private FsDos getDos (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    List<FsDos> disks = new ArrayList<> (2);

    if (length == 143_360)
      for (BlockReader reader : dos33Readers)
        try
        {
          FsDos fs = new FsDos (name, buffer, offset, length, reader);

          fs.readCatalog ();
          if (fs.getTotalCatalogBlocks () > 0)
            disks.add (fs);
        }
        catch (FileFormatException e)
        {
        }

    switch (disks.size ())
    {
      case 1:
        return disks.get (0);

      case 2:
        return disks.get (0).getTotalCatalogBlocks () > disks.get (1).getTotalCatalogBlocks ()
            ? disks.get (0) : disks.get (1);

      default:
        return null;
    }
  }

  // ---------------------------------------------------------------------------------//
  private FsProdos getProdos (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    for (BlockReader reader : blockReaders)
      try
      {
        FsProdos fs = new FsProdos (name, buffer, offset, length, reader);

        fs.readCatalog ();
        if (fs.getTotalCatalogBlocks () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsPascal getPascal (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    for (BlockReader reader : blockReaders)
      try
      {
        FsPascal fs = new FsPascal (name, buffer, offset, length, reader);

        fs.readCatalog ();
        if (fs.getTotalCatalogBlocks () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsCpm getCpm (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == 143_360)
      try
      {
        FsCpm fs = new FsCpm (name, buffer, offset, length, cpmReader);

        fs.readCatalog ();
        if (fs.getTotalCatalogBlocks () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsLbr getLbr (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      FsLbr fs = new FsLbr (name, buffer, offset, length, lbrReader);

      fs.readCatalog ();
      if (fs.getTotalCatalogBlocks () > 0)
        return fs;
    }
    catch (FileFormatException e)
    {
    }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsBinary2 getBinary2 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, 0, BIN2) && buffer[18] == 0x02)
      try
      {
        FsBinary2 fs = new FsBinary2 (name, buffer, offset, length, lbrReader);

        fs.readCatalog ();
        if (fs.getFiles ().size () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsNuFX getNuFx (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, 0, NuFile))
      try
      {
        FsNuFX fs = new FsNuFX (name, buffer, offset, length, lbrReader);

        fs.readCatalog ();
        if (fs.getFiles ().size () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsDos getDos31 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == 116_480)                  // Dos3.1
      try
      {
        FsDos fs = new FsDos (name, buffer, offset, length, dos31Reader);

        fs.readCatalog ();
        if (fs.getTotalCatalogBlocks () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsDos4 getDos4 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == 143_360)
      try
      {
        FsDos4 fs = new FsDos4 (name, buffer, offset, length, dos33Reader0);

        fs.readCatalog ();
        if (fs.getTotalCatalogBlocks () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private void getUnidos (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == UNIDOS_SIZE * 2)
      try
      {
        FsDos fs1 = new FsDos (name, buffer, 0, UNIDOS_SIZE, unidosReader);
        fs1.readCatalog ();

        if (fs1 != null && fs1.getTotalCatalogBlocks () > 0)
        {
          FsDos fs2 = new FsDos (name, buffer, UNIDOS_SIZE, UNIDOS_SIZE, unidosReader);
          fs2.readCatalog ();

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
