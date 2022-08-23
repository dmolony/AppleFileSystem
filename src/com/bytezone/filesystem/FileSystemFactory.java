package com.bytezone.filesystem;

import static com.bytezone.filesystem.BlockReader.AddressType.BLOCK;
import static com.bytezone.filesystem.BlockReader.AddressType.SECTOR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileSystemFactory
// -----------------------------------------------------------------------------------//
{
  private static final int DOS31_SIZE = 116_480;
  private static final int DOS33_SIZE = 143_360;
  private static final int UNIDOS_SIZE = 409_600;
  private static final int CPAM_SIZE = 819_200;

  static BlockReader blockReader0 = new BlockReader (512, BLOCK, 0, 0);     // Prodos
  static BlockReader blockReader1 = new BlockReader (512, BLOCK, 1, 8);     // Prodos
  static BlockReader cpmReader0 = new BlockReader (1024, BLOCK, 2, 4);      // CPM
  static BlockReader cpmReader1 = new BlockReader (1024, BLOCK, 0, 8);      // CPAM
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
  public AppleFileSystem getFileSystem (File file)
  // ---------------------------------------------------------------------------------//
  {
    return getFileSystem (file.getName (), readAllBytes (file.toPath ()));
  }

  // ---------------------------------------------------------------------------------//
  AppleFileSystem getFileSystem (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    return getFileSystem (file.getName (), file.read ());
  }

  // ---------------------------------------------------------------------------------//
  public AppleFileSystem getFileSystem (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    return getFileSystem (name, buffer, 0, buffer.length);
  }

  // ---------------------------------------------------------------------------------//
  public AppleFileSystem getFileSystem (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    fileSystems = new ArrayList<> ();

    if (debug)
    {
      System.out.printf ("Checking: %s%n", name);
      System.out.printf ("Length  : %,d%n", length);
      System.out.println (Utility.format (buffer, offset, 100));
    }

    if (length == 143_488)
      length = 143_360;

    assert offset + length <= buffer.length;

    getDos31 (name, buffer, offset, length);
    getDos33 (name, buffer, offset, length);
    getDos4 (name, buffer, offset, length);
    getProdos (name, buffer, offset, length);
    getPascal (name, buffer, offset, length);
    getCpm (name, buffer, offset, length);

    if (fileSystems.size () == 0)         // these filesystems cannot be hybrids
    {
      getCpm2 (name, buffer, offset, length);
      getLbr (name, buffer, offset, length);
      getNuFx (name, buffer, offset, length);
      getBinary2 (name, buffer, offset, length);
      getZip (name, buffer, offset, length);
      getGZip (name, buffer, offset, length);
      get2img (name, buffer, offset, length);
      getUnidos (name, buffer, offset, length);
      getWoz (name, buffer, offset, length);
    }

    return switch (fileSystems.size ())
    {
      case 0 -> new FsData (name, buffer, blockReader0);
      case 1 -> fileSystems.get (0);
      default -> new FsHybrid (fileSystems);
    };
  }

  // ---------------------------------------------------------------------------------//
  public String getSuffix (String name)
  // ---------------------------------------------------------------------------------//
  {
    return Utility.getSuffix (name);
  }

  // ---------------------------------------------------------------------------------//
  public int getSuffixNumber (String name)
  // ---------------------------------------------------------------------------------//
  {
    return Utility.getSuffixNo (name);
  }

  // ---------------------------------------------------------------------------------//
  private void getDos31 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == DOS31_SIZE)
      try
      {
        FsDos fs = new FsDos (name, buffer, offset, length, dos31Reader);
        fs.readCatalog ();

        if (fs.getTotalCatalogBlocks () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private void getDos33 (String name, byte[] buffer, int offset, int length)
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

    switch (fsList.size ())
    {
      case 1:
        fileSystems.add (fsList.get (0));
        break;

      case 2:
        if (fsList.get (0).getTotalCatalogBlocks () > fsList.get (1).getTotalCatalogBlocks ())
          fileSystems.add (fsList.get (0));
        else
          fileSystems.add (fsList.get (1));
    }
  }

  // ---------------------------------------------------------------------------------//
  private void getDos4 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == DOS33_SIZE)
      try
      {
        FsDos4 fs = new FsDos4 (name, buffer, offset, length, dos33Reader0);
        fs.readCatalog ();

        if (fs.getTotalCatalogBlocks () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private void getUnidos (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == UNIDOS_SIZE * 2)
      try
      {
        FsUnidos fs = new FsUnidos (name, buffer, offset, length, unidosReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private void getProdos (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    for (BlockReader reader : blockReaders)
      try
      {
        FsProdos fs = new FsProdos (name, buffer, offset, length, reader);
        fs.readCatalog ();

        if (fs.getTotalCatalogBlocks () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private void getPascal (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    for (BlockReader reader : blockReaders)
      try
      {
        FsPascal fs = new FsPascal (name, buffer, offset, length, reader);
        fs.readCatalog ();

        if (fs.getTotalCatalogBlocks () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private void getCpm (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == DOS33_SIZE)
      try
      {
        FsCpm fs = new FsCpm (name, buffer, offset, length, cpmReader0);
        fs.readCatalog ();

        if (fs.getTotalCatalogBlocks () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private void getCpm2 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (length == CPAM_SIZE)
      try
      {
        FsCpm fs = new FsCpm (name, buffer, offset, length, cpmReader1);
        fs.readCatalog ();

        if (fs.getTotalCatalogBlocks () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private void getLbr (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      FsLbr fs = new FsLbr (name, buffer, offset, length, lbrReader);
      fs.readCatalog ();

      if (fs.getTotalCatalogBlocks () > 0)
        fileSystems.add (fs);
    }
    catch (FileFormatException e)
    {
      if (debug)
        System.out.println (e);
    }
  }

  // ---------------------------------------------------------------------------------//
  private void getBinary2 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, offset, FsBinary2.BIN2) && buffer[offset + 18] == 0x02)
      try
      {
        FsBinary2 fs = new FsBinary2 (name, buffer, offset, length, lbrReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private void getNuFx (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, offset, FsNuFX.NuFile))
      try
      {
        FsNuFX fs = new FsNuFX (name, buffer, offset, length, lbrReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private void get2img (String name, byte[] buffer, int offset, int length)
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
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private void getZip (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, offset, FsZip.ZIP))
      try
      {
        FsZip fs = new FsZip (name, buffer, offset, length, lbrReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private void getGZip (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, offset, FsGzip.GZIP))
      try
      {
        FsGzip fs = new FsGzip (name, buffer, offset, length, lbrReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private void getWoz (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    if (Utility.isMagic (buffer, 0, FsWoz.WOZ_1) || Utility.isMagic (buffer, offset, FsWoz.WOZ_2))
      try
      {
        FsWoz fs = new FsWoz (name, buffer, offset, length, lbrReader);
        fs.readCatalog ();

        if (fs.getFiles ().size () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }

  // ---------------------------------------------------------------------------------//
  private byte[] readAllBytes (Path fileName)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      return Files.readAllBytes (fileName);
    }
    catch (IOException e)
    {
      e.printStackTrace ();
      return null;
    }
  }
}
