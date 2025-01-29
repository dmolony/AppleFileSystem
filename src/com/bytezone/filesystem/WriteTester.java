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
      base + "HDV/8-bit Games.hdv",                  // 
  };

  // ---------------------------------------------------------------------------------//
  public WriteTester ()
  // ---------------------------------------------------------------------------------//
  {
    FileSystemFactory factory = new FileSystemFactory ();

    //    System.out.println ("Original disk");
    AppleFileSystem fs = factory.getFileSystem (Path.of (fileNames[2]));
    if (fs == null)
    {
      System.out.println ("disk not found");
      return;
    }

    String newDiskName = "blanketyblank.dsk";

    //    System.out.println ("creating copy");
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
