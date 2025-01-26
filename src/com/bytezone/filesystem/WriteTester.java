package com.bytezone.filesystem;

import java.nio.file.Path;

// -----------------------------------------------------------------------------------//
public class WriteTester
// -----------------------------------------------------------------------------------//
{
  String base = System.getProperty ("user.home") + "/Documents/Examples/";
  String adi = base + "Apple Disk Images/";

  String[] fileNames = {                             //
      adi + "DENIS.DSK",                         // 0: 3.3 intl 0
  };

  // ---------------------------------------------------------------------------------//
  public WriteTester ()
  // ---------------------------------------------------------------------------------//
  {
    FileSystemFactory factory = new FileSystemFactory ();

    AppleFileSystem fs = factory.getFileSystem (Path.of (fileNames[0]));
    if (fs == null)
    {
      System.out.println ("disk not found");
      return;
    }

    String newDisk = "blanketyblank.dsk";

    AppleFileSystem newFs =
        factory.getFileSystem (newDisk, fs.getDiskBuffer ().copyData ());

    //    System.out.println (newFs);

    for (AppleFile file : newFs.getFiles ())
    {
      System.out.printf ("deleting %s%n", file.getFileName ());
      newFs.deleteFile (file);
    }

    String fileName = adi + newDisk;
    newFs.create (fileName);
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args)
  // ---------------------------------------------------------------------------------//
  {
    new WriteTester ();
  }
}
