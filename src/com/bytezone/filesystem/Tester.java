package com.bytezone.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------------//
public class Tester
// -----------------------------------------------------------------------------------//
{
  String base = "/Users/denismolony/Documents/Examples/";
  String adi = base + "Apple Disk Images/";
  String adav = base + "apple_dos_all_versions/";
  //    String pdav = adi + "prodos_all_versions/";
  String intl = base + "interleave/";
  String euro = base + "Apple_IIgs_European_Disk_Collection/";
  String hybr = base + "AppleHybrid/";
  String cpm = base + "cpm/CPM collection (37 disks) and more/";
  String sdk = base + "SDK/";
  String shk = base + "SHK/";
  String woz = base + "woz/wozaday_Wizardry/";
  String wiz = base + "Wizardry/";

  String[] fileNames = {//
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
      cpm + "ARCHIVES 7.2mg",                        // 21: prodos with shk
      sdk + "SCASM.II4.0.SDK",                       // 22: NuFx
      shk + "DosMaster.shk",                         // 23: DosMaster shk
      wiz + "Wizardry/murasama.dsk",                 // 24: pascal wizardry
      woz + "Wizardry PGMO.woz",                     // 25: pascal wizardry woz
      wiz + "wizardry_IV/Version A/wiz4_d1.dsk",     // 26: wizardry IV disk 1
  };

  // ---------------------------------------------------------------------------------//
  Tester ()
  // ---------------------------------------------------------------------------------//
  {
    List<AppleFileSystem> fileSystems = new ArrayList<> ();

    for (int fileNo = 10; fileNo <= 10; fileNo++)
    {
      Path path = Path.of (fileNames[fileNo]);
      String name = path.toFile ().getName ();

      byte[] buffer = readAllBytes (path);
      List<AppleFileSystem> fsList = FileSystemFactory.getFileSystems (name, buffer);

      if (fsList.size () == 0)
        System.out.println ("Unknown disk format: " + name);
      else
        fileSystems.addAll (fsList);
    }

    for (AppleFileSystem fs : fileSystems)
    {
      System.out.println (fs.toText ());
      System.out.println ();
      System.out.println (fs.catalog ());
      System.out.println ();
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
