package com.bytezone.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.BlockReader.AddressType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileSystemFactory
// -----------------------------------------------------------------------------------//
{
  private static final int DOS31_SIZE = 116_480;
  private static final int DOS33_SIZE = 143_360;
  private static final int UNIDOS_SIZE = 409_600;
  private static final int CPAM_SIZE = 819_200;

  //  static BlockReader lbrReader = new BlockReader (128, BLOCK, 0, 0);        // LBR
  //  static BlockReader dos31Reader = new BlockReader (256, SECTOR, 0, 13);    // Dos 3.1
  //  static BlockReader dos33Reader0 = new BlockReader (256, SECTOR, 0, 16);   // Dos 3.3
  //  static BlockReader dos33Reader1 = new BlockReader (256, SECTOR, 1, 16);   // Dos 3.3
  //  static BlockReader unidosReader = new BlockReader (256, SECTOR, 0, 32);   // UniDos
  //  static BlockReader blockReader0 = new BlockReader (512, BLOCK, 0, 0);     // Prodos, Pascal
  //  static BlockReader blockReader1 = new BlockReader (512, BLOCK, 1, 8);     // Prodos, Pascal
  //  static BlockReader cpmReader0 = new BlockReader (1024, BLOCK, 2, 4);      // CPM
  //  static BlockReader cpmReader1 = new BlockReader (1024, BLOCK, 0, 8);      // CPAM

  //  static BlockReader[] dos33Readers = { dos33Reader0, dos33Reader1 };
  //  static BlockReader[] blockReaders = { blockReader0, blockReader1 };

  List<AppleFileSystem> fileSystems;
  private boolean debug = false;
  private Path path;

  // ---------------------------------------------------------------------------------//
  public AppleFileSystem getFileSystem (Path path)
  // ---------------------------------------------------------------------------------//
  {
    this.path = path;
    AppleFileSystem fs = getFileSystem (readAllBytes (path));

    return fs;
  }

  // ---------------------------------------------------------------------------------//
  AppleFileSystem getFileSystem (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    path = null;
    return getFileSystem (file.read ());
  }

  // ---------------------------------------------------------------------------------//
  public AppleFileSystem getFileSystem (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    BlockReader blockReader = new BlockReader (buffer, 0, buffer.length);
    return getFileSystem (blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public AppleFileSystem getFileSystem (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    fileSystems = new ArrayList<> ();

    if (debug)
    {
      //      System.out.printf ("Checking: %s%n", name);
      System.out.printf ("Length  : %,d%n", blockReader.length);
      System.out.println (Utility.format (blockReader.diskBuffer, blockReader.diskOffset, 100));
    }

    //    if (length == 143_488)
    //      length = 143_360;

    //    assert offset + length <= buffer.length;

    getDos31 (blockReader);
    getDos33 (blockReader);
    getDos4 (blockReader);
    getProdos (blockReader);
    getPascal (blockReader);
    getCpm (blockReader);

    if (fileSystems.size () == 0)         // these filesystems cannot be hybrids
    {
      getCpm2 (blockReader);
      getLbr (blockReader);
      getNuFx (blockReader);
      getBinary2 (blockReader);
      getZip (blockReader);
      getGZip (blockReader);
      get2img (blockReader);
      getUnidos (blockReader);
      getWoz (blockReader);
    }

    switch (fileSystems.size ())
    {
      case 0:
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
    if (blockReader.length == DOS31_SIZE)
    {
      try
      {
        BlockReader dos31Reader = new BlockReader (blockReader);
        dos31Reader.setParameters (256, AddressType.SECTOR, 0, 13);
        FsDos fs = new FsDos (path, dos31Reader);

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

    if (blockReader.length == DOS33_SIZE)
      for (int i = 0; i < 2; i++)
        try
        {
          BlockReader dos33Reader = new BlockReader (blockReader);
          dos33Reader.setParameters (256, AddressType.SECTOR, i, 16);

          FsDos fs = new FsDos (path, dos33Reader);

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
    if (blockReader.length == DOS33_SIZE)
      try
      {
        BlockReader dos4Reader = new BlockReader (blockReader);
        dos4Reader.setParameters (256, AddressType.SECTOR, 0, 16);
        FsDos4 fs = new FsDos4 (path, dos4Reader);

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
    if (blockReader.length == UNIDOS_SIZE * 2)
      try
      {
        BlockReader unidosReader = new BlockReader (blockReader);
        blockReader.setParameters (256, AddressType.SECTOR, 0, 32);
        FsUnidos fs = new FsUnidos (path, unidosReader);

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
    if (blockReader.length >= DOS33_SIZE)
      for (int i = 0; i < 2; i++)
        try
        {
          BlockReader prodosReader = new BlockReader (blockReader);
          prodosReader.setParameters (512, AddressType.BLOCK, i, i * 8);

          FsProdos fs = new FsProdos (path, prodosReader);

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
    if (blockReader.length >= DOS33_SIZE)
      for (int i = 0; i < 2; i++)
        try
        {
          BlockReader pascalReader = new BlockReader (blockReader);
          pascalReader.setParameters (512, AddressType.BLOCK, i, i * 8);
          FsPascal fs = new FsPascal (path, pascalReader);

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
    if (blockReader.length == DOS33_SIZE)
      try
      {
        BlockReader cpmReader = new BlockReader (blockReader);
        cpmReader.setParameters (1024, AddressType.BLOCK, 2, 4);
        FsCpm fs = new FsCpm (path, cpmReader);

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
    if (blockReader.length == CPAM_SIZE)
      try
      {
        BlockReader cpamReader = new BlockReader (blockReader);
        cpamReader.setParameters (1024, AddressType.BLOCK, 0, 8);
        FsCpm fs = new FsCpm (path, cpamReader);

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
      FsLbr fs = new FsLbr (path, lbrReader);

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
    if (Utility.isMagic (blockReader.diskBuffer, blockReader.diskOffset, FsBinary2.BIN2)
        && blockReader.diskBuffer[blockReader.diskOffset + 18] == 0x02)
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);
        FsBinary2 fs = new FsBinary2 (path, lbrReader);

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
    if (Utility.isMagic (blockReader.diskBuffer, blockReader.diskOffset, FsNuFX.NuFile))
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);
        FsNuFX fs = new FsNuFX (path, lbrReader);

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
    if (Utility.isMagic (blockReader.diskBuffer, blockReader.diskOffset, Fs2img.TWO_IMG))
      try
      {
        if (debug)
          System.out.println ("Checking 2img");
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);
        Fs2img fs = new Fs2img (path, lbrReader);

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
    if (Utility.isMagic (blockReader.diskBuffer, blockReader.diskOffset, FsZip.ZIP))
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);
        FsZip fs = new FsZip (path, lbrReader);

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
    if (Utility.isMagic (blockReader.diskBuffer, blockReader.diskOffset, FsGzip.GZIP))
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);
        FsGzip fs = new FsGzip (path, lbrReader);

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
    if (Utility.isMagic (blockReader.diskBuffer, blockReader.diskOffset, FsWoz.WOZ_1)
        || Utility.isMagic (blockReader.diskBuffer, blockReader.diskOffset, FsWoz.WOZ_2))
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);
        FsWoz fs = new FsWoz (path, lbrReader);

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
