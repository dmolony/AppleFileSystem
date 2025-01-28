package com.bytezone.filesystem;

import java.nio.file.Path;

// -----------------------------------------------------------------------------------//
public class WriteTester
// -----------------------------------------------------------------------------------//
{
  String base = System.getProperty ("user.home") + "/Documents/Examples/";
  String adi = base + "Apple Disk Images/";

  String[] fileNames = {                             //
      adi + "DENIS.DSK",                             // 0: 3.3 intl 0
      base + "prodos/View-Sector.dsk",               // 1: Prodos block
      base + "incoming/EDASM.DSK",                   // 2: prodos sector
  };

  // ---------------------------------------------------------------------------------//
  public WriteTester ()
  // ---------------------------------------------------------------------------------//
  {
    FileSystemFactory factory = new FileSystemFactory ();

    AppleFileSystem fs = factory.getFileSystem (Path.of (fileNames[1]));
    if (fs == null)
    {
      System.out.println ("disk not found");
      return;
    }

    String newDiskName = "blanketyblank.dsk";

    AppleFileSystem newFs =
        factory.getFileSystem (newDiskName, fs.getDiskBuffer ().copyData ());

    deleteFiles (newFs, 0);

    newFs.create (adi + newDiskName);
  }

  // ---------------------------------------------------------------------------------//
  private void deleteFiles (AppleContainer container, int depth)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleFile file : container.getFiles ())
    {
      System.out.printf ("%02d  %s%n", depth, file.getFileName ());

      if (file instanceof AppleContainer ac)                    // folder
        deleteFiles (ac, depth + 1);

      file.delete ();
    }
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args)
  // ---------------------------------------------------------------------------------//
  {
    new WriteTester ();
  }
}
