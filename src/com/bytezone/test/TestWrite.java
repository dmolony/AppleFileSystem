package com.bytezone.test;

import java.nio.file.Path;

import com.bytezone.filesystem.AppleContainer;
import com.bytezone.filesystem.AppleFile;
import com.bytezone.filesystem.AppleFileSystem;
import com.bytezone.filesystem.FileLockedException;
import com.bytezone.filesystem.FileSystemFactory;

// -----------------------------------------------------------------------------------//
public class TestWrite extends Tester
// -----------------------------------------------------------------------------------//
{
  String indent = "                        ";

  // ---------------------------------------------------------------------------------//
  public TestWrite ()
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
    System.out.printf ("Copying : %s%n", fs.getFileName ());

    AppleFileSystem newFs =
        factory.getFileSystem (newDiskName, fs.getDiskBuffer ().copyData ());

    deleteFiles (newFs, 0);                 // recursively delete files and folders
    newFs.cleanDisk ();                     // zero out unused blocks

    newFs.create (adi + newDiskName);
  }

  // ---------------------------------------------------------------------------------//
  private void deleteFiles (AppleContainer container, int depth)
  // ---------------------------------------------------------------------------------//
  {
    int count = 0;
    for (AppleFile file : container.getFiles ())
    {
      //      if (file.getFileName ().equals ("PRODOS"))
      //        continue;
      if (file.getFileName ().equals ("HELLO"))
        continue;
      //      if (file.getFileName ().equals ("SYSTEM.CHARSET"))
      //        continue;
      //      if (file.getFileName ().equals ("SYSTEM.PASCAL"))
      //        continue;

      //      ++count;
      //      if (count == 1 || count == 6)
      {
        System.out.printf ("%s %s %s%n", indent.substring (0, depth * 2),
            file.getFileName (), file.isForkedFile () ? "*** Forked ***" : "");

        if (file.isFolder ())
          deleteFiles ((AppleContainer) file, depth + 1);

        try
        {
          file.delete (true);
        }
        catch (FileLockedException e)
        {
          System.out.printf ("Locked file: %s%n", file.getFileName ());
        }
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args)
  // ---------------------------------------------------------------------------------//
  {
    new TestWrite ();
  }
}
