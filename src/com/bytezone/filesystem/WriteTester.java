package com.bytezone.filesystem;

import java.nio.file.Path;

// -----------------------------------------------------------------------------------//
public class WriteTester
// -----------------------------------------------------------------------------------//
{
  String base = System.getProperty ("user.home") + "/Documents/Examples/";
  String adi = base + "Apple Disk Images/";

  String[] fileNames = {                             //
      adi + "blankdisk.DSK",                         // 0: 3.3 intl 0
  };

  // ---------------------------------------------------------------------------------//
  public WriteTester ()
  // ---------------------------------------------------------------------------------//
  {
    FileSystemFactory factory = new FileSystemFactory ();

    AppleFileSystem fs = factory.getFileSystem (Path.of (fileNames[0]));

    AppleFileSystem newFs =
        factory.getFileSystem ("tester.dsk", fs.getDiskBuffer ().copyData ());

    System.out.println (newFs);

    for (AppleFile file : newFs.getFiles ())
    {
      System.out.println (file);
      newFs.deleteFile (file);
    }
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args)
  // ---------------------------------------------------------------------------------//
  {
    new WriteTester ();
  }
}
