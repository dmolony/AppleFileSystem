package com.bytezone.filesystem;

import static com.bytezone.filesystem.BlockReader.AddressType.BLOCK;
import static com.bytezone.filesystem.BlockReader.AddressType.SECTOR;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileSystemFactory
// -----------------------------------------------------------------------------------//
{
  private static final byte[] WOZ_1 = { 0x57, 0x4F, 0x5A, 0x32, (byte) 0xFF, 0x0A, 0x0D, 0x0A };
  private static final byte[] WOZ_2 = { 0x57, 0x4F, 0x5A, 0x31, (byte) 0xFF, 0x0A, 0x0D, 0x0A };

  private static final int DOS31_SIZE = 116_480;
  private static final int DOS33_SIZE = 143_360;
  private static final int UNIDOS_SIZE = 409_600;

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

  List<AppleFileSystem> fileSystems;
  private boolean debug = false;

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
    return getFileSystems (name, buffer, 0, buffer.length);
  }

  // ---------------------------------------------------------------------------------//
  public List<AppleFileSystem> getFileSystems (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    fileSystems = new ArrayList<> ();

    if (debug)
    {
      System.out.println ("Checking: " + name);
      System.out.println (Utility.format (buffer, offset, 100));
    }

    if (length == 143_488)
      length = 143_360;

    assert offset + length <= buffer.length;

    add (getProdos (name, buffer, offset, length));
    add (getPascal (name, buffer, offset, length));
    add (getDos31 (name, buffer, offset, length));
    add (getDos33 (name, buffer, offset, length));
    add (getDos4 (name, buffer, offset, length));
    add (getCpm (name, buffer, offset, length));
    add (getLbr (name, buffer, offset, length));
    add (getNuFx (name, buffer, offset, length));
    add (getBinary2 (name, buffer, offset, length));
    add (getZip (name, buffer, offset, length));
    add (getGZip (name, buffer, offset, length));
    add (get2img (name, buffer, offset, length));
    add (getUnidos (name, buffer, offset, length));
    add (getWoz (name, buffer, offset, length));

    return fileSystems;
  }

  // ---------------------------------------------------------------------------------//
  private void add (AppleFileSystem appleFileSystem)
  // ---------------------------------------------------------------------------------//
  {
    if (appleFileSystem != null)
      fileSystems.add (appleFileSystem);
  }

  // ---------------------------------------------------------------------------------//
  private FsDos getDos31 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == DOS31_SIZE)
      try
      {
        FsDos fs = new FsDos (name, buffer, offset, length, dos31Reader);
        fs.readCatalog ();

        if (fs.getTotalCatalogBlocks () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsDos getDos33 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    List<FsDos> fsList = new ArrayList<> (2);

    if (length == DOS33_SIZE)
      for (BlockReader reader : dos33Readers)
        try
        {
          FsDos fs = new FsDos (name, buffer, offset, length, reader);
          fs.readCatalog ();

          if (fs.getTotalCatalogBlocks () > 0)
            fsList.add (fs);
        }
        catch (FileFormatException e)
        {
          if (debug)
            System.out.println (e);
        }

    return switch (fsList.size ())
    {
      case 1 -> fsList.get (0);
      case 2 -> fsList.get (0).getTotalCatalogBlocks () > fsList.get (1).getTotalCatalogBlocks ()
          ? fsList.get (0) : fsList.get (1);
      default -> null;
    };
  }

  // ---------------------------------------------------------------------------------//
  private FsDos4 getDos4 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == DOS33_SIZE)
      try
      {
        FsDos4 fs = new FsDos4 (name, buffer, offset, length, dos33Reader0);
        fs.readCatalog ();

        if (fs.getTotalCatalogBlocks () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }

    return null;
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
        if (debug)
          System.out.println (e);
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
        if (debug)
          System.out.println (e);
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsCpm getCpm (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == DOS33_SIZE)
      try
      {
        FsCpm fs = new FsCpm (name, buffer, offset, length, cpmReader);
        fs.readCatalog ();

        if (fs.getTotalCatalogBlocks () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
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
      if (debug)
        System.out.println (e);
    }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsBinary2 getBinary2 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, offset, FsBinary2.BIN2) && buffer[offset + 18] == 0x02)
      try
      {
        FsBinary2 fs = new FsBinary2 (name, buffer, offset, length, lbrReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsNuFX getNuFx (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, offset, FsNuFX.NuFile))
      try
      {
        FsNuFX fs = new FsNuFX (name, buffer, offset, length, lbrReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private Fs2img get2img (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, offset, Fs2img.TWO_IMG))
      try
      {
        if (debug)
          System.out.println ("Checking 2img");
        Fs2img fs = new Fs2img (name, buffer, offset, length, lbrReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsUnidos getUnidos (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == UNIDOS_SIZE * 2)
      try
      {
        FsUnidos fs = new FsUnidos (name, buffer, offset, length, unidosReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsZip getZip (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, offset, FsZip.ZIP))
      try
      {
        FsZip fs = new FsZip (name, buffer, offset, length, lbrReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsGzip getGZip (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, offset, FsGzip.GZIP))
      try
      {
        FsGzip fs = new FsGzip (name, buffer, offset, length, lbrReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsWoz getWoz (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, 0, WOZ_1) || Utility.isMagic (buffer, offset, WOZ_2))
      try
      {
        FsWoz fs = new FsWoz (name, buffer, offset, length, lbrReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          return fs;
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }

    return null;
  }
}
