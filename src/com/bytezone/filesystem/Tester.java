package com.bytezone.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
  String sdk = base + "SDK/";
  String shk = base + "SHK/";
  String woz = base + "woz/wozaday_Wizardry/";
  String wiz = base + "Wizardry/";
  String lbr = base + "LBR/";
  String bin = base + "Binary II/";

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
      cpm + "ARCHIVES 7.2mg",                        // 21: 2mg / prodos / NuFX LBR files
      sdk + "SCASM.II4.0.SDK",                       // 22: NuFX / Dos
      shk + "DosMaster.shk",                         // 23: DosMaster NuFX
      wiz + "Wizardry/murasama.dsk",                 // 24: pascal wizardry
      woz + "Wizardry PGMO.woz",                     // 25: pascal wizardry woz
      wiz + "wizardry_IV/Version A/wiz4_d1.dsk",     // 26: wizardry IV disk 1
      lbr + "crnch24s.lbr",                          // 27: LBR
      bin + "GBBS.UTILS.BNY",                        // 28: binary II
      bin + "GWFTP11B2.BXY",                         // 29: binary II / NuFX
  };

  // ---------------------------------------------------------------------------------//
  Tester ()
  // ---------------------------------------------------------------------------------//
  {
    FileSystemFactory factory = new FileSystemFactory ();

    for (int fileNo = 0; fileNo < fileNames.length; fileNo++)
    {
      System.out.printf ("%n%d %s%n", fileNo, fileNames[fileNo].substring (base.length ()));

      Path path = Path.of (fileNames[fileNo]);
      String name = path.toFile ().getName ();

      List<AppleFileSystem> fsList = factory.getFileSystems (name, readAllBytes (path));

      if (fsList.size () == 0)
      {
        System.out.println ("Unknown FS format: " + name);
        continue;
      }

      if (fileNo == 99)
      {
        for (AppleFileSystem fs : fsList)
        {
          System.out.println ();
          System.out.println (fs.catalog ());
        }
      }

      for (AppleFileSystem fs : fsList)
      {
        System.out.println ();
        System.out.println (fs.toText ());
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
