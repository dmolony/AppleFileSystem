package com.bytezone.test;

import java.io.IOException;
import java.nio.file.Path;

import com.bytezone.filesystem.AppleContainer;
import com.bytezone.filesystem.AppleFile;
import com.bytezone.filesystem.AppleFileSystem;
import com.bytezone.filesystem.FileSystemFactory;

// see: https://github.com/zeek/bromagic/blob/master/database/apple
// -----------------------------------------------------------------------------------//
public class TestRead extends Tester
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  TestRead ()
  // ---------------------------------------------------------------------------------//
  {
    FileSystemFactory factory = new FileSystemFactory ();

    int index = 47;
    for (int fileNo = index; fileNo <= index; fileNo++)
    //    for (int fileNo = 0; fileNo < fileNames.length; fileNo++)
    {
      //      System.out.printf ("%d %s%n", fileNo, fileNames[fileNo].substring (base.length ()));

      AppleFileSystem fs = factory.getFileSystem (Path.of (fileNames[fileNo]));

      if (fs == null)
      {
        System.out.println ("Unknown FS format: " + fileNames[fileNo]);
        continue;
      }

      System.out.printf ("%2d  %s  %s%n", fileNo, line (fs),
          fileNames[fileNo].substring (base.length ()));

      for (AppleFileSystem fs2 : fs.getFileSystems ())
        System.out.printf ("    %s%n", line (fs2));

      for (AppleFile file : fs.getFiles ())
        if (file.hasEmbeddedFileSystem ())
          System.out.printf ("    %s%n", line (file.getEmbeddedFileSystem ()));
    }
  }

  // ---------------------------------------------------------------------------------//
  private String line (AppleFileSystem fs)
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-6s  %2d %,5d", fs.getFileSystemType (),
        fs.getFileSystems ().size (), fs.getFiles ().size ());
  }

  // ---------------------------------------------------------------------------------//
  private void listFiles (AppleContainer container, int depth)
  // ---------------------------------------------------------------------------------//
  {
    if (container instanceof AppleFileSystem afs)
    {
      System.out.printf ("%2d  %04d  %-6s   %s%n", depth, afs.getTotalBlocks (),
          afs.getFileSystemType (), afs.getFileName ());
      //      System.out.println (container);
    }

    for (AppleFile file : container.getFiles ())
    {
      //      System.out.println (file.getFileName ());
      int totalBlocks;
      if (file.hasEmbeddedFileSystem ())
        totalBlocks = file.getEmbeddedFileSystem ().getTotalBlocks ();
      else
        totalBlocks = file.getTotalBlocks ();

      System.out.printf ("%2d  %04d  %-6s %s %s%n", depth + 1, totalBlocks,
          file.getFileTypeText (), file.isLocked () ? "*" : " ", file.getFileName ());

      if (file instanceof AppleContainer ac)                    // folder
        listFiles (ac, depth + 1);
      else if (file.hasEmbeddedFileSystem ())                    // PAR, LBR
        listFiles (file.getEmbeddedFileSystem (), depth + 1);
    }

    if (container.getFileSystems () != null)
      for (AppleFileSystem fileSystem : container.getFileSystems ())
      {
        // System.out.printf ("%2d  %04d  %-4s   %s%n", depth, fileSystem.getTotalBlocks (),
        //            fileSystem.getFileSystemType (), fileSystem.getFileName ());
        listFiles (fileSystem, depth + 1);
      }
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args) throws IOException
  // ---------------------------------------------------------------------------------//
  {
    new TestRead ();
  }
}
