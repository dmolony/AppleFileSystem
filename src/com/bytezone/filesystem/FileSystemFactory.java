package com.bytezone.filesystem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.BlockReader.AddressType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileSystemFactory
// -----------------------------------------------------------------------------------//
{
  private static final int SECTOR_13_SIZE = 116_480;
  private static final int SECTOR_16_SIZE = 143_360;
  private static final int UNIDOS_SIZE = 409_600;
  private static final int CPAM_SIZE = 819_200;

  private List<AppleFileSystem> fileSystems;
  private boolean debug = false;

  // ---------------------------------------------------------------------------------//
  public AppleFileSystem getFileSystem (Path path)
  // ---------------------------------------------------------------------------------//
  {
    return getFileSystem (new BlockReader (path));
  }

  // ---------------------------------------------------------------------------------//
  //  AppleFileSystem getFileSystem (AppleFile file)
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return getFileSystem (file.read ());
  //  }

  // ---------------------------------------------------------------------------------//
  //  public AppleFileSystem getFileSystem (byte[] buffer)
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return getFileSystem (new BlockReader (buffer, 0, buffer.length));
  //  }

  // ---------------------------------------------------------------------------------//
  public AppleFileSystem getFileSystem (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    fileSystems = new ArrayList<> ();

    if (debug)
    {
      System.out.printf ("Checking: %s%n", blockReader.getPath ());
      System.out.printf ("Length  : %,d%n", blockReader.getDiskLength ());
      System.out.println (
          Utility.format (blockReader.getDiskBuffer (), blockReader.getDiskOffset (), 100));
    }

    getDos31 (blockReader);
    getDos33 (blockReader);
    getDos4 (blockReader);
    getProdos (blockReader);
    getPascal (blockReader);
    getCpm (blockReader);

    if (fileSystems.size () == 0)         // these filesystems cannot be hybrids
      getDos31 (blockReader);
    if (fileSystems.size () == 0)
      getCpm2 (blockReader);
    if (fileSystems.size () == 0)
      getLbr (blockReader);
    if (fileSystems.size () == 0)
      getNuFx (blockReader);
    if (fileSystems.size () == 0)
      getBinary2 (blockReader);
    if (fileSystems.size () == 0)
      getZip (blockReader);
    if (fileSystems.size () == 0)
      getGZip (blockReader);
    if (fileSystems.size () == 0)
      get2img (blockReader);
    if (fileSystems.size () == 0)
      getUnidos (blockReader);
    if (fileSystems.size () == 0)
      getWoz (blockReader);

    switch (fileSystems.size ())
    {
      case 0:
        blockReader.setParameters (256, AddressType.SECTOR, 0, 16);
        return new FsData (blockReader);

      case 1:
        return fileSystems.get (0);

      default:
        return new FsHybrid (fileSystems);
    }
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
  public List<String> getSuffixes ()
  // ---------------------------------------------------------------------------------//
  {
    return Utility.getSuffixes ();
  }

  // ---------------------------------------------------------------------------------//
  public int getSuffixesSize ()
  // ---------------------------------------------------------------------------------//
  {
    return Utility.getSuffixes ().size ();
  }

  // ---------------------------------------------------------------------------------//
  private void getDos31 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (blockReader.getDiskLength () == SECTOR_13_SIZE)
    {
      try
      {
        BlockReader dos31Reader = new BlockReader (blockReader);
        dos31Reader.setParameters (256, AddressType.SECTOR, 0, 13);

        FsDos fs = new FsDos (dos31Reader);

        if (fs.getTotalCatalogBlocks () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  private void getDos33 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    List<FsDos> fsList = new ArrayList<> (2);

    if (blockReader.getDiskLength () == SECTOR_16_SIZE)
      for (int i = 0; i < 2; i++)
        try
        {
          BlockReader dos33Reader = new BlockReader (blockReader);
          dos33Reader.setParameters (256, AddressType.SECTOR, i, 16);

          FsDos fs = new FsDos (dos33Reader);

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
  private void getDos4 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (blockReader.getDiskLength () == SECTOR_16_SIZE)
      try
      {
        BlockReader dos4Reader = new BlockReader (blockReader);
        dos4Reader.setParameters (256, AddressType.SECTOR, 0, 16);

        FsDos4 fs = new FsDos4 (dos4Reader);

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
  private void getUnidos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (blockReader.getDiskLength () == UNIDOS_SIZE * 2)
      try
      {
        BlockReader unidosReader = new BlockReader (blockReader);
        blockReader.setParameters (256, AddressType.SECTOR, 0, 32);

        FsUnidos fs = new FsUnidos (unidosReader);

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
  private void getProdos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    // should check for common HD sizes
    if (blockReader.getDiskLength () >= SECTOR_16_SIZE)
      for (int i = 0; i < 2; i++)
        try
        {
          BlockReader prodosReader = new BlockReader (blockReader);
          prodosReader.setParameters (512, AddressType.BLOCK, i, i * 8);

          FsProdos fs = new FsProdos (prodosReader);

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
  private void getPascal (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    // should check for common HD sizes
    if (blockReader.getDiskLength () >= SECTOR_16_SIZE)
      for (int i = 0; i < 2; i++)
        try
        {
          BlockReader pascalReader = new BlockReader (blockReader);
          pascalReader.setParameters (512, AddressType.BLOCK, i, i * 8);

          FsPascal fs = new FsPascal (pascalReader);

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
  private void getCpm (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (blockReader.getDiskLength () == SECTOR_16_SIZE)
      try
      {
        BlockReader cpmReader = new BlockReader (blockReader);
        cpmReader.setParameters (1024, AddressType.BLOCK, 2, 4);

        FsCpm fs = new FsCpm (cpmReader);

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
  private void getCpm2 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (blockReader.getDiskLength () == CPAM_SIZE)
      try
      {
        BlockReader cpamReader = new BlockReader (blockReader);
        cpamReader.setParameters (1024, AddressType.BLOCK, 0, 8);

        FsCpm fs = new FsCpm (cpamReader);

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
  private void getLbr (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      BlockReader lbrReader = new BlockReader (blockReader);
      lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);

      FsLbr fs = new FsLbr (lbrReader);

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
  private void getBinary2 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (blockReader.isMagic (0, FsBinary2.BIN2) && blockReader.byteAt (18, (byte) 0x02))
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);

        FsBinary2 fs = new FsBinary2 (lbrReader);

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
  private void getNuFx (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (blockReader.isMagic (0, FsNuFX.NuFile))
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);

        FsNuFX fs = new FsNuFX (lbrReader);

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
  private void get2img (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (blockReader.isMagic (0, Fs2img.TWO_IMG))
      try
      {
        if (debug)
          System.out.println ("Checking 2img");

        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);

        Fs2img fs = new Fs2img (lbrReader);

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
  private void getZip (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (blockReader.isMagic (0, FsZip.ZIP))
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);

        FsZip fs = new FsZip (lbrReader);

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
  private void getGZip (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (blockReader.isMagic (0, FsGzip.GZIP))
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);

        FsGzip fs = new FsGzip (lbrReader);

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
  private void getWoz (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (blockReader.isMagic (0, FsWoz.WOZ_1) || blockReader.isMagic (0, FsWoz.WOZ_2))
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);

        FsWoz fs = new FsWoz (lbrReader);

        if (fs.getFiles ().size () > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        if (debug)
          System.out.println (e);
      }
  }
}
