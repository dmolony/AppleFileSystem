package com.bytezone.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.bytezone.utility.Utility;

// see: https://github.com/zeek/bromagic/blob/master/database/apple
// -----------------------------------------------------------------------------------//
public class Tester
// -----------------------------------------------------------------------------------//
{
  String base = System.getProperty ("user.home") + "/Documents/Examples/";
  String adi = base + "Apple Disk Images/";
  String adav = base + "apple_dos_all_versions/";
  String intl = base + "interleave/";
  String euro = base + "Apple_IIgs_European_Disk_Collection/";
  String hybr = base + "AppleHybrid/";
  String cpm = base + "cpm/CPM collection (37 disks) and more/";
  String woz = base + "woz/wozaday_Wizardry/";
  String wiz = base + "Wizardry/";
  String cmp = base + "compressed/";
  String sdk = cmp + "SDK/";
  String shk = cmp + "SHK/";
  String lbr = cmp + "LBR/";
  String bny = cmp + "BNY/";
  String bxy = cmp + "BXY/";
  String bqy = cmp + "BQY/";
  String bsq = cmp + "BSQ/";

  String[] fileNames = {                             //
      base + "dos/Assembler.dsk",                    // 0: 3.3 intl 0
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
      wiz + "Wizardry/murasama.dsk",                 // 21: pascal wizardry
      woz + "Wizardry PGMO.woz",                     // 22: pascal wizardry woz
      wiz + "wizardry_IV/Version A/wiz4_d1.dsk",     // 23: wizardry IV disk 1
      cmp + "ARCHIVES 7.2mg",                        // 24: 2mg / prodos / NuFX LBR files
      sdk + "SCASM.II4.0.SDK",                       // 25: NuFX / Dos
      sdk + "tdbt12d1.sdk",                          // 26: NuFX / CPM
      shk + "DosMaster.shk",                         // 27: DosMaster NuFX
      lbr + "crnch24s.lbr",                          // 28: LBR
      bny + "GBBS.UTILS.BNY",                        // 29: binary II
      bxy + "GWFTP11B2.BXY",                         // 30: binary II / NuFX
      bqy + "NW.PROTALK5.BQY",                       // 31: binary II / Squeeze
      //      shk + "mousetrap.shk",                           // 32:
  };

  // ---------------------------------------------------------------------------------//
  Tester ()
  // ---------------------------------------------------------------------------------//
  {
    FileSystemFactory factory = new FileSystemFactory ();

    //    for (int fileNo = 0; fileNo < fileNames.length; fileNo++)
    for (int fileNo = 31; fileNo <= 31; fileNo++)
    {
      //      System.out.printf ("%n%d %s%n", fileNo, fileNames[fileNo].substring (base.length ()));

      Path path = Path.of (fileNames[fileNo]);
      String name = path.toFile ().getName ();

      List<AppleFileSystem> fsList = factory.getFileSystems (name, readAllBytes (path));

      if (fsList.size () == 0)
      {
        System.out.println ("Unknown FS format: " + name);
        continue;
      }

      if (fileNo == 31)
      {
        for (AppleFileSystem fs : fsList)
        {
          System.out.println ();
          System.out.println (fs.catalog ());

          for (AppleFile file : fs.getFiles ())
            if (file.getName ().equals ("NET.CONFIG.S.QQ"))
            {
              byte[] buffer = file.read ();
              System.out.println (Utility.format (buffer));
              break;
            }
        }
      }

      System.out.println ();
      listFileSystems (fsList, 0);
    }
  }

  // ---------------------------------------------------------------------------------//
  private void listFileSystems (List<? extends AppleFile> files, int depth)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleFile fs : files)
    {
      if (fs.isFileSystem ())
      {
        System.out.printf ("%2d  %s%n", depth, fs);
        listFileSystems (fs.getFiles (), depth + 1);
      }
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
      System.exit (1);
      return null;            // stupid editor
    }
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args) throws IOException
  // ---------------------------------------------------------------------------------//
  {
    new Tester ();
  }
}
