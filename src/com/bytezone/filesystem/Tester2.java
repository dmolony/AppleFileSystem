package com.bytezone.filesystem;

import static com.bytezone.filesystem.BlockReader.AddressType.BLOCK;
import static com.bytezone.filesystem.BlockReader.AddressType.SECTOR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------------//
public class Tester2 extends Tester
// -----------------------------------------------------------------------------------//
{
  private static final int UNIDOS_SIZE = 409_600;

  static BlockReader blockReader0 = new BlockReader (512, BLOCK, 0, 0);     // Prodos
  static BlockReader blockReader1 = new BlockReader (512, BLOCK, 1, 8);     // Prodos
  static BlockReader cpmReader = new BlockReader (1024, BLOCK, 2, 4);       // CPM
  static BlockReader dos31Reader = new BlockReader (256, SECTOR, 0, 13);    // Dos 3.1
  static BlockReader dos33Reader0 = new BlockReader (256, SECTOR, 0, 16);   // Dos 3.3
  static BlockReader dos33Reader1 = new BlockReader (256, SECTOR, 1, 16);   // Dos 3.3
  static BlockReader unidosReader = new BlockReader (256, SECTOR, 0, 32);   // UniDos

  String base = "/Users/denismolony/Documents/Examples/";
  String adi = base + "Apple Disk Images/";
  String adav = base + "apple_dos_all_versions/";
  //    String pdav = adi + "prodos_all_versions/";
  String intl = base + "interleave/";
  String euro = base + "Apple_IIgs_European_Disk_Collection/";
  String hybr = base + "AppleHybrid/";

  String[] fileNames = //
      { base + "dos/Assembler.dsk",                    // 0: 3.3 intl 0
        adi + "Apple disks/DOS33.dsk",                 // 1: 3.3 intl 1
        adi + "Toolkit.do",                            // 2: 3.3 128 bytes too long
        adi + "My Stuff/DENIS.DSK",                    // 3: 3.3 sparse text file
        adav + "Apple DOS 3.1 Master.d13",             // 4: 3.1
        base + "DOS 4.1/DOS4.1.SourceH.dsk",           // 5: 4.1
        base + "prodos/extra level/VBMP.po",           // 6: Prodos block
        base + "incoming/EDASM.DSK",                   // 7: prodos sector
        base + "800K/BRCC_A13.po",                     // 8: prodos 800K
        base + "HDV/8-bit games.hdv",                  // 9: prodos HD
        base + "HDV/UCSD.hdv",                         // 10: Prodos HD with Pascal area
        base + "mg/crypto.libs.2mg",                   // 11: 2mg prodos 800K
//        base + "DosMaster/DOSMaster16mCF.hdv",         // 12: DosMaster
        base + "DosMaster/Testing/Vol003.dsk",         // 13: DosMaster bad catalog
        intl + "pascal/SANE Disk 2.po",                // 14: pascal floppy blocks
        base + "pascal/Apple Pascal - Disk 0.dsk",     // 15: pascal floppy sectors
        base + "cpm/CPM_C_2_2.dsk",                    // 16: CPM floppy
        euro + "UniDOS 3.3.po",                        // 17: Unidos 32 sector
        hybr + "HybridHuffin/IAC20.DSK",               // 18: hybrid pascal/dos
        hybr + "cpm/HYBRID.DSK",                       // 19: hybrid cpm/dos
        base + "dual dos/AAL-8603.DSK",                // 20: hybrid prodos/dos
      };

  // ---------------------------------------------------------------------------------//
  Tester2 ()
  // ---------------------------------------------------------------------------------//
  {
    List<AppleFileSystem> fileSystems = new ArrayList<> ();

    for (int fileNo = 0; fileNo < fileNames.length; fileNo++)
    {
      fileSystems.clear ();

      Path path = Path.of (fileNames[fileNo]);
//      heading (path);
      byte[] buffer = read (path);

      int offset = 0;
      int length = buffer.length;

      String prefix = new String (buffer, 0, 4);
      if ("2IMG".equals (prefix))
      {
        offset = getWord (buffer, 8);
        length -= offset;
      }

      try
      {
        if (length >= 143_360 && length <= 143_488)
        {
          // Dos3.3
          FsDos dos = getDos (path, buffer, offset, length);
          if (dos != null && dos.catalogBlocks > 0)
            fileSystems.add (dos);

          // Pascal
          FsPascal pascal = getPascal (path, buffer, offset, length);
          if (pascal != null)
            fileSystems.add (pascal);

          // CPM
          FsCpm cpm = getCpm (path, buffer, offset, length);
          if (cpm != null)
            fileSystems.add (cpm);
        }

        // Dos3.1
        if (length == 116_480)
        {
          FsDos dos = new FsDos (path.toFile ().getName (), buffer, offset, length);
          dos.setBlockReader (dos31Reader);
          dos.readCatalog ();
          if (dos.catalogBlocks > 0)
            fileSystems.add (dos);
        }

        // Dos4
        FsDos4 dos4 = new FsDos4 (path.toFile ().getName (), buffer, offset, length);
        dos4.setBlockReader (dos33Reader0);
        dos4.readCatalog ();
        if (dos4.catalogBlocks > 0)
          fileSystems.add (dos4);

        // Unidos
        if (length == UNIDOS_SIZE * 2)
        {
          FsDos fs = getDos (path, buffer, 0, UNIDOS_SIZE);
          fs.setBlockReader (unidosReader);
          fs.readCatalog ();
          if (fs.catalogBlocks > 0)
            fileSystems.add (fs);

          fs = getDos (path, buffer, UNIDOS_SIZE, UNIDOS_SIZE);
          fs.setBlockReader (unidosReader);
          fs.readCatalog ();
          if (fs.catalogBlocks > 0)
            fileSystems.add (fs);
        }

        // Prodos
        FsProdos fs = getProdos (path, buffer, offset, length);
        if (fs != null && fs.catalogBlocks > 0)
          fileSystems.add (fs);
      }
      catch (FileFormatException e)
      {
        System.out.println (e);
      }

      for (AppleFileSystem fs : fileSystems)
        System.out.println (fs.toText ());
    }
  }

  // ---------------------------------------------------------------------------------//
  private void heading (Path path)
  // ---------------------------------------------------------------------------------//
  {
    File file = path.toFile ();
    long fileLength = file.length ();

    System.out.println ("-------------------------------------------------------------");
    System.out.printf ("File name   : %s%n", file.getName ());
    System.out.printf ("File length : %,d%n", fileLength);
    System.out.println ("-------------------------------------------------------------");
  }

  // ---------------------------------------------------------------------------------//
  private byte[] read (Path fileName)
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

  // ---------------------------------------------------------------------------------//
//  private FsDos getDos (Path path, byte[] buffer)
//  // ---------------------------------------------------------------------------------//
//  {
//    return getDos (path, buffer, 0, buffer.length);
//  }

  // ---------------------------------------------------------------------------------//
  private FsDos getDos (Path path, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    List<FsDos> disks = new ArrayList<> (2);

    for (int i = 0; i < 2; i++)
      try
      {
        FsDos fs = new FsDos (path.toFile ().getName (), buffer, offset, length);
        fs.setBlockReader (i == 0 ? dos33Reader0 : dos33Reader1);
        fs.readCatalog ();
        disks.add (fs);
      }
      catch (FileFormatException e)
      {
        System.out.println (e);       // loop around
      }

    if (disks.size () == 0)
      return null;

    if (disks.size () == 1)
      return disks.get (0);

    return disks.get (0).catalogBlocks > disks.get (1).catalogBlocks ? disks.get (0)
        : disks.get (1);
  }

  // ---------------------------------------------------------------------------------//
  private FsProdos getProdos (Path path, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    FsProdos prodos = new FsProdos (path.toFile ().getName (), buffer, offset, length);

    for (int i = 0; i < 2; i++)
      try
      {
        prodos.setBlockReader (i == 0 ? blockReader0 : blockReader1);
        prodos.readCatalog ();
        return prodos;
      }
      catch (FileFormatException e)
      {
        //        System.out.println (e);
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsPascal getPascal (Path path, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    FsPascal pascal = new FsPascal (path.toFile ().getName (), buffer, offset, length);

    for (int i = 0; i < 2; i++)
      try
      {
        pascal.setBlockReader (i == 0 ? blockReader0 : blockReader1);
        pascal.readCatalog ();
        return pascal;
      }
      catch (FileFormatException e)
      {
        //          System.out.println (e);
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  private FsCpm getCpm (Path path, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      FsCpm cpm = new FsCpm (path.toFile ().getName (), buffer, offset, length);
      cpm.setBlockReader (cpmReader);
      cpm.readCatalog ();
      return cpm;
    }
    catch (FileFormatException e)
    {
      //          System.out.println (e);
    }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  public static int getWord (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    int a = (buffer[ptr + 1] & 0xFF) << 8;
    int b = buffer[ptr] & 0xFF;
    return a + b;
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args) throws IOException
  // ---------------------------------------------------------------------------------//
  {
    new Tester2 ();
  }
}
