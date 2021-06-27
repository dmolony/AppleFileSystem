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
public class Tester
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
        base + "DosMaster/DOSMaster16mCF.hdv",         // 12: DosMaster
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
  Tester ()
  // ---------------------------------------------------------------------------------//
  {
    // Dos 3.3
    for (int fileNo = 0; fileNo <= 3; fileNo++)
    {
      Path path = Path.of (fileNames[fileNo]);
      byte[] buffer = read (path);

      AppleFileSystem fs = getDos (path, buffer);
      System.out.println (fs.toText ());
    }

    // Dos 3.1
    for (int fileNo = 4; fileNo <= 4; fileNo++)
    {
      Path path = Path.of (fileNames[fileNo]);
      byte[] buffer = read (path);

      try
      {
        FsDos fs = new FsDos (path.toFile ().getName (), buffer);
        fs.setBlockReader (dos31Reader);
        fs.readCatalog ();
        System.out.println (fs.toText ());
      }
      catch (FileFormatException e)
      {
        System.out.println (e);
      }
    }

    // Dos 4.1
    for (int fileNo = 5; fileNo <= 5; fileNo++)
    {
      Path path = Path.of (fileNames[fileNo]);
      byte[] buffer = read (path);

      FsDos4 dos = new FsDos4 (path.toFile ().getName (), buffer);
      dos.setBlockReader (dos33Reader0);
      dos.readCatalog ();
      System.out.println (dos.toText ());
    }

    // Prodos
    for (int fileNo = 6; fileNo <= 11; fileNo++)
    {
      Path path = Path.of (fileNames[fileNo]);
      byte[] buffer = read (path);

      int offset = 0;
      int length = buffer.length;

      String prefix = new String (buffer, 0, 4);
      if ("2IMG".equals (prefix))
      {
        offset = getWord (buffer, 8);
        length -= offset;
      }

      AppleFileSystem fs = getProdos (path, buffer, offset, length);
      System.out.println (fs.toText ());
    }

    // dosmaster
    Path path = Path.of (fileNames[12]);
    byte[] buffer = read (path);

    int offset = 0;
    int length = buffer.length;

    AppleFileSystem fs = getProdos (path, buffer, offset, length);
    System.out.println (fs.toText ());

    // Pascal
    for (int fileNo = 14; fileNo <= 15; fileNo++)
    {
      path = Path.of (fileNames[fileNo]);
      buffer = read (path);

      fs = getPascal (path, buffer);
      System.out.println (fs.toText ());
    }

    // CPM
    path = Path.of (fileNames[16]);
    buffer = read (path);

    FsCpm cpm = new FsCpm (path.toFile ().getName (), buffer);
    cpm.setBlockReader (cpmReader);
    cpm.readCatalog ();
    System.out.println (cpm.toText ());

    // Unidos
    path = Path.of (fileNames[17]);
    buffer = read (path);

    fs = getDos (path, buffer, 0, UNIDOS_SIZE);
    fs.setBlockReader (unidosReader);
    ((FsDos) fs).readCatalog ();
    System.out.println (fs.toText ());

    fs = getDos (path, buffer, UNIDOS_SIZE, UNIDOS_SIZE);
    fs.setBlockReader (unidosReader);
    ((FsDos) fs).readCatalog ();
    System.out.println (fs.toText ());

    // hybrid Pascal/Dos
    path = Path.of (fileNames[18]);
    buffer = read (path);

    fs = getDos (path, buffer);
    System.out.println (fs.toText ());

    fs = getPascal (path, buffer);
    System.out.println (fs.toText ());

    // hybrid CPM/Dos
    path = Path.of (fileNames[19]);
    buffer = read (path);

    fs = getDos (path, buffer);
    System.out.println (fs.toText ());

    cpm = new FsCpm (path.toFile ().getName (), buffer);
    cpm.setBlockReader (cpmReader);
    cpm.readCatalog ();
    System.out.println (cpm.toText ());

    // hybrid Prodos/Dos
    path = Path.of (fileNames[20]);
    buffer = read (path);

    fs = getDos (path, buffer);
    System.out.println (fs.toText ());

    fs = getProdos (path, buffer, 0, buffer.length);
    System.out.println (fs.toText ());
  }

  // ---------------------------------------------------------------------------------//
  private void heading (File file)
  // ---------------------------------------------------------------------------------//
  {
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
  private AppleFileSystem getDos (Path path, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    return getDos (path, buffer, 0, buffer.length);
  }

  // ---------------------------------------------------------------------------------//
  private AppleFileSystem getDos (Path path, byte[] buffer, int offset, int length)
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
  private AppleFileSystem getProdos (Path path, byte[] buffer, int offset, int length)
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
  private AppleFileSystem getPascal (Path path, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    FsPascal pascal = new FsPascal (path.toFile ().getName (), buffer);

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
    new Tester ();
  }
}
