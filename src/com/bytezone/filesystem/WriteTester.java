package com.bytezone.filesystem;

import java.nio.file.Path;

// -----------------------------------------------------------------------------------//
public class WriteTester extends Tester
// -----------------------------------------------------------------------------------//
{
  String indent = "                        ";

  // ---------------------------------------------------------------------------------//
  public WriteTester ()
  // ---------------------------------------------------------------------------------//
  {
    FileSystemFactory factory = new FileSystemFactory ();

    AppleFileSystem fs = factory.getFileSystem (Path.of (fileNames[14]));
    if (fs == null)
    {
      System.out.println ("disk not found");
      return;
    }

    String newDiskName = "blanketyblank.dsk";
    System.out.printf ("Copying : %s%n", fs.getFileName ());

    AppleFileSystem newFs =
        factory.getFileSystem (newDiskName, fs.getDiskBuffer ().copyData ());

    deleteFiles (newFs, 0);
    newFs.cleanDisk ();

    newFs.create (adi + newDiskName);
  }

  // ---------------------------------------------------------------------------------//
  private void deleteFiles (AppleContainer container, int depth)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleFile file : container.getFiles ())
    {
      if (file.getFileName ().equals ("PRODOS"))
        continue;
      if (file.getFileName ().equals ("HELLO"))
        continue;
      if (file.getFileName ().equals ("SYSTEM.CHARSET"))
        continue;
      //      if (file.getFileName ().equals ("SYSTEM.PASCAL"))
      //        continue;

      System.out.printf ("%s %s %s%n", indent.substring (0, depth * 2),
          file.getFileName (), file.isForkedFile () ? "*** Forked ***" : "");

      if (file.isFolder ())
        deleteFiles ((AppleContainer) file, depth + 1);

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
