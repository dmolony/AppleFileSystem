package com.bytezone.filesystem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bytezone.filesystem.AppleFileSystem.FileSystemType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileSystemFactory
// -----------------------------------------------------------------------------------//
{
  private static final int SECTOR_35_13_SIZE = 116_480;
  private static final int SECTOR_35_16_SIZE = 143_360;
  private static final int SECTOR_35_32_SIZE = 286_720;
  private static final int SECTOR_40_16_SIZE = 163_840;
  private static final int SECTOR_40_32_SIZE = 327_680;
  private static final int SECTOR_48_16_SIZE = 196_608;
  private static final int SECTOR_48_32_SIZE = 393_216;
  private static final int UNIDOS_SIZE = 819_200;
  private static final int CPAM_SIZE = 819_200;

  public static final DiskParameters dos1 = new DiskParameters (256, 0, 16);
  public static final DiskParameters dos2 = new DiskParameters (256, 1, 16);
  public static final DiskParameters dos31 = new DiskParameters (256, 0, 13);
  public static final DiskParameters dos4 = new DiskParameters (256, 0, 32);

  public static final DiskParameters prodos1 = new DiskParameters (512, 0, 8);
  public static final DiskParameters prodos2 = new DiskParameters (512, 1, 8);

  public static final DiskParameters cpm = new DiskParameters (1024, 2, 4);
  public static final DiskParameters cpam = new DiskParameters (1024, 0, 4);

  public static final DiskParameters bin = new DiskParameters (128, 0, 0);

  private List<AppleFileSystem> fileSystems;
  private List<String> errorMessages;

  private boolean debug = false;

  // ---------------------------------------------------------------------------------//
  public AppleFileSystem getFileSystem (Path path)
  // ---------------------------------------------------------------------------------//
  {
    if (!path.toFile ().exists ())
      throw new FileFormatException (String.format ("Path %s does not exist%n", path));

    return getFileSystem (new BlockReader (path));
  }

  // ---------------------------------------------------------------------------------//
  public AppleFileSystem getFileSystem (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    return getFileSystem (new BlockReader (name, buffer));
  }

  // ---------------------------------------------------------------------------------//
  public AppleFileSystem getFileSystem (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    Objects.requireNonNull (blockReader);

    fileSystems = new ArrayList<> ();
    errorMessages = new ArrayList<> ();

    if (debug)
    {
      Buffer diskBuffer = blockReader.getDiskBuffer ();
      System.out.println ("-------------------------------------------------------");
      System.out.printf ("Length  : %,d%n", diskBuffer.length ());
      System.out.println (Utility.format (diskBuffer.data (), diskBuffer.offset (), 100));
      System.out.println ("-------------------------------------------------------");
    }

    // keep checking for disk headers until there are none
    List<DiskHeader> diskHeaders = new ArrayList<> ();

    while (true)
    {
      if (DiskHeader2img.isValid (blockReader))
      {
        DiskHeader diskHeader = new DiskHeader2img (blockReader);
        blockReader = diskHeader.getBlockReader ();
        diskHeaders.add (diskHeader);
        continue;
      }

      if (DiskHeaderDiskCopy.isValid (blockReader))
      {
        DiskHeader diskHeader = new DiskHeaderDiskCopy (blockReader);
        blockReader = diskHeader.getBlockReader ();
        diskHeaders.add (diskHeader);
        continue;
      }

      break;
    }

    getDos33 (blockReader);

    if (fileSystems.size () == 0)
      getDos4 (blockReader);

    getProdos (blockReader);
    getPascal (blockReader);
    getCpm (blockReader);

    if (fileSystems.size () == 0)         // these file systems cannot be hybrids
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
      getUnidos (blockReader);
    if (fileSystems.size () == 0)
      getWoz (blockReader);

    for (AppleFileSystem fs : fileSystems)
      fs.setDiskHeaders (diskHeaders);

    switch (fileSystems.size ())
    {
      case 0:
        blockReader.setParameters (dos1);
        AppleFileSystem fs = new FsData (blockReader);
        if (errorMessages.size () > 0)
          fs.setErrorMessage (errorMessages.get (0));
        return fs;

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
    if (blockReader.getDiskBuffer ().length () == SECTOR_35_13_SIZE)
    {
      try
      {
        BlockReader dos31Reader = new BlockReader (blockReader);
        dos31Reader.setParameters (dos31);

        FsDos3 fs = new FsDos3 (dos31Reader);

        if (fs.getTotalCatalogBlocks () > 0)
        {
          fs.readCatalogBlocks ();
          fileSystems.add (fs);
        }
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
    if (debug)
      System.out.println ("Checking Dos33");

    List<FsDos3> fsList = new ArrayList<> (2);

    if (blockReader.getDiskBuffer ().length () == SECTOR_35_16_SIZE)
      for (int i = 0; i < 2; i++)
        try
        {
          BlockReader dos33Reader = new BlockReader (blockReader);
          dos33Reader.setParameters (i == 0 ? dos1 : dos2);

          FsDos3 fs = new FsDos3 (dos33Reader);

          if (debug)
            System.out.printf ("Found %d catalog blocks%n", fs.getTotalCatalogBlocks ());

          if (fs.getTotalCatalogBlocks () > 0)
          {
            fsList.add (fs);
            if (fs.getTotalCatalogBlocks () >= 15)        // best possible result
              break;
          }
        }
        catch (FileFormatException e)
        {
          if (debug)
            System.out.println (e);
        }

    if (debug)
      System.out.println ("Tried both interleaves");

    switch (fsList.size ())
    {
      case 1:
        FsDos3 fs = fsList.get (0);
        fileSystems.add (fs);
        fs.readCatalogBlocks ();
        break;

      case 2:
        FsDos3 fs0 = fsList.get (0);
        FsDos3 fs1 = fsList.get (1);

        if (fs0.getTotalCatalogBlocks () > fs1.getTotalCatalogBlocks ())
        {
          fileSystems.add (fs0);
          fs0.readCatalogBlocks ();
        }
        else
        {
          fileSystems.add (fs1);
          fs1.readCatalogBlocks ();
        }
    }

    if (debug)
      System.out.println ("Finished Dos33");
  }

  // ---------------------------------------------------------------------------------//
  private void getDos4 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      BlockReader dos4Reader = new BlockReader (blockReader);

      switch (blockReader.getDiskBuffer ().length ())
      {
        case SECTOR_35_16_SIZE:
        case SECTOR_40_16_SIZE:
        case SECTOR_48_16_SIZE:
          dos4Reader.setParameters (dos1);
          break;

        case SECTOR_35_32_SIZE:
        case SECTOR_40_32_SIZE:
        case SECTOR_48_32_SIZE:
          dos4Reader.setParameters (dos4);
          break;

        default:
          return;
      }

      FsDos4 fs = new FsDos4 (dos4Reader);

      if (fs.getTotalCatalogBlocks () > 0)
      {
        fileSystems.add (fs);
        fs.readCatalogBlocks ();
      }
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
    if (blockReader.getDiskBuffer ().length () == UNIDOS_SIZE)
      try
      {
        BlockReader unidosReader = new BlockReader (blockReader);
        unidosReader.setParameters (dos4);

        FsUnidos fs = new FsUnidos (unidosReader);

        if (fs.getFileSystems ().size () == 2)  // should be exactly 2 dos file systems
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
    if (blockReader.getDiskBuffer ().length () >= SECTOR_35_16_SIZE)
      for (int i = 0; i < 2; i++)
        try
        {
          BlockReader prodosReader = new BlockReader (blockReader);
          prodosReader.setParameters (i == 0 ? prodos1 : prodos2);

          FsProdos fs = new FsProdos (prodosReader);

          if (fs.getTotalCatalogBlocks () > 0)
          {
            fileSystems.add (fs);
            if (debug)
              System.out.println ("Adding Prodos");
            break;
          }
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
    if (blockReader.getDiskBuffer ().length () >= SECTOR_35_16_SIZE)
      for (int i = 0; i < 2; i++)
        try
        {
          if (debug)
            System.out.printf ("Pascal attempt %d%n", i);
          BlockReader pascalReader = new BlockReader (blockReader);
          pascalReader.setParameters (i == 0 ? prodos1 : prodos2);

          FsPascal fs = new FsPascal (pascalReader);

          if (fs.getTotalCatalogBlocks () > 0)
          {
            fileSystems.add (fs);
            if (debug)
              System.out.println ("Adding Pascal");
            break;
          }
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
    if (blockReader.getDiskBuffer ().length () == SECTOR_35_16_SIZE)
      try
      {
        BlockReader cpmReader = new BlockReader (blockReader);
        cpmReader.setParameters (cpm);

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

  // this is not fully working yet
  // ---------------------------------------------------------------------------------//
  private void getCpm2 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    if (blockReader.getDiskBuffer ().length () == CPAM_SIZE)
      try
      {
        BlockReader cpamReader = new BlockReader (blockReader);
        cpamReader.setParameters (cpam);

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
      lbrReader.setParameters (bin);

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
    Buffer diskBuffer = blockReader.getDiskBuffer ();
    if (diskBuffer.isMagic (0, FsBinary2.BIN2) && diskBuffer.byteAt (18, (byte) 0x02))
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (bin);

        if (debug)
          System.out.println ("Bin2 magic OK");

        FsBinary2 fs = new FsBinary2 (lbrReader);

        if (fs.getFileSystems ().size () > 0 || fs.getFiles ().size () > 0)
        {
          fileSystems.add (fs);
          if (debug)
            System.out.println ("Adding Bin2");
        }
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
    if (Utility.isMagic (blockReader.getDiskBuffer (), 0, FsNuFX.NuFile))
      try
      {
        BlockReader nufxReader = new BlockReader (blockReader);
        //        lbrReader.setParameters (128, AddressType.BLOCK, 0, 0);
        nufxReader.setParameters (bin);

        FsNuFX fs = new FsNuFX (nufxReader);

        if (fs.getFileSystems ().size () > 0 || fs.getFiles ().size () > 0)
        {
          fileSystems.add (fs);
          if (debug)
            System.out.println ("Adding NuFX");
        }
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
    if (Utility.isMagic (blockReader.getDiskBuffer (), 0, FsZip.ZIP))
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (bin);

        FsZip fs = new FsZip (lbrReader);

        if (fs.getFiles ().size () > 0 || fs.getFileSystems ().size () > 0)
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
    if (Utility.isMagic (blockReader.getDiskBuffer (), 0, FsGzip.GZIP))
      try
      {
        BlockReader lbrReader = new BlockReader (blockReader);
        lbrReader.setParameters (bin);

        FsGzip fs = new FsGzip (lbrReader);

        if (fs.getFiles ().size () > 0 || fs.getFileSystems ().size () > 0)
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
    FileSystemType fileSystemType =
        Utility.isMagic (blockReader.getDiskBuffer (), 0, FsWoz.WOZ_1)
            ? FileSystemType.WOZ1
            : Utility.isMagic (blockReader.getDiskBuffer (), 0, FsWoz.WOZ_2)
                ? FileSystemType.WOZ2 : null;

    if (fileSystemType == null)
      return;

    try
    {
      BlockReader lbrReader = new BlockReader (blockReader);
      lbrReader.setParameters (bin);

      FsWoz fs = new FsWoz (lbrReader, fileSystemType);

      if (fs.getFileSystems ().size () > 0 || fs.getFiles ().size () > 0)
        fileSystems.add (fs);
    }
    catch (Exception e)
    {
      if (debug)
        System.out.println (e);

      errorMessages.add (e.toString ());
    }
  }

  //  search/1/t  FiLeStArTfIlEsTaRt  binscii (apple ][) text
  //  string    \x0aGL            Binary II (apple ][) data
  //  string    \x76\xff          Squeezed (apple ][) data
  //  string    NuFile            NuFile archive (apple ][) data
  //  string    N\xf5F\xe9l\xe5   NuFile archive (apple ][) data
  //  belong    0x00051600        AppleSingle encoded Macintosh file
  //  belong    0x00051607        AppleDouble encoded Macintosh file
}
