package com.bytezone.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// -----------------------------------------------------------------------------------//
public class Tester1 extends Tester
// -----------------------------------------------------------------------------------//
{

  // ---------------------------------------------------------------------------------//
  Tester1 ()
  // ---------------------------------------------------------------------------------//
  {
    // Dos 3.3
    for (int fileNo = 0; fileNo <= 3; fileNo++)
    {
      Path path = Path.of (fileNames[fileNo]);
      byte[] buffer = read (path);

      AppleFileSystem fs = getDos (path.toFile ().getName (), buffer);
      System.out.println (fs.toText ());
    }

    // Dos 3.1
    for (int fileNo = 4; fileNo <= 4; fileNo++)
    {
      Path path = Path.of (fileNames[fileNo]);
      byte[] buffer = read (path);

      try
      {
        FsDos fs = new FsDos (path.toFile ().getName (), buffer);
        fs.setBlockReader (dos31Reader);
        fs.readCatalog ();
        System.out.println (fs.toText ());
      }
      catch (FileFormatException e)
      {
        System.out.println (e);
      }
    }

    // Dos 4.1
    for (int fileNo = 5; fileNo <= 5; fileNo++)
    {
      Path path = Path.of (fileNames[fileNo]);
      byte[] buffer = read (path);

      FsDos4 dos = new FsDos4 (path.toFile ().getName (), buffer);
      dos.setBlockReader (dos33Reader0);
      dos.readCatalog ();
      System.out.println (dos.toText ());
    }

    // Prodos
    for (int fileNo = 6; fileNo <= 11; fileNo++)
    {
      Path path = Path.of (fileNames[fileNo]);
      byte[] buffer = read (path);

      int offset = 0;
      int length = buffer.length;

      String prefix = new String (buffer, 0, 4);
      if ("2IMG".equals (prefix))
      {
        offset = getWord (buffer, 8);
        length -= offset;
      }

      AppleFileSystem fs = getProdos (path.toFile ().getName (), buffer, offset, length);
      System.out.println (fs.toText ());
    }

    // dosmaster
    Path path = Path.of (fileNames[12]);
    byte[] buffer = read (path);

    int offset = 0;
    int length = buffer.length;

    AppleFileSystem fs = getProdos (path.toFile ().getName (), buffer, offset, length);
    System.out.println (fs.toText ());

    // Pascal
    for (int fileNo = 14; fileNo <= 15; fileNo++)
    {
      path = Path.of (fileNames[fileNo]);
      buffer = read (path);

      fs = getPascal (path.toFile ().getName (), buffer, offset, length);
      System.out.println (fs.toText ());
    }

    // CPM
    path = Path.of (fileNames[16]);
    buffer = read (path);

    FsCpm cpm = new FsCpm (path.toFile ().getName (), buffer, offset, length);
    cpm.setBlockReader (cpmReader);
    cpm.readCatalog ();
    System.out.println (cpm.toText ());

    // Unidos
    path = Path.of (fileNames[17]);
    buffer = read (path);

    fs = getDos (path.toFile ().getName (), buffer, 0, UNIDOS_SIZE);
    fs.setBlockReader (unidosReader);
    ((FsDos) fs).readCatalog ();
    System.out.println (fs.toText ());

    fs = getDos (path.toFile ().getName (), buffer, UNIDOS_SIZE, UNIDOS_SIZE);
    fs.setBlockReader (unidosReader);
    ((FsDos) fs).readCatalog ();
    System.out.println (fs.toText ());

    // hybrid Pascal/Dos
    path = Path.of (fileNames[18]);
    buffer = read (path);

    fs = getDos (path.toFile ().getName (), buffer);
    System.out.println (fs.toText ());

    fs = getPascal (path.toFile ().getName (), buffer, offset, length);
    System.out.println (fs.toText ());

    // hybrid CPM/Dos
    path = Path.of (fileNames[19]);
    buffer = read (path);

    fs = getDos (path.toFile ().getName (), buffer);
    System.out.println (fs.toText ());

    cpm = new FsCpm (path.toFile ().getName (), buffer);
    cpm.setBlockReader (cpmReader);
    cpm.readCatalog ();
    System.out.println (cpm.toText ());

    // hybrid Prodos/Dos
    path = Path.of (fileNames[20]);
    buffer = read (path);

    fs = getDos (path.toFile ().getName (), buffer);
    System.out.println (fs.toText ());

    fs = getProdos (path.toFile ().getName (), buffer, 0, buffer.length);
    System.out.println (fs.toText ());
  }

  // ---------------------------------------------------------------------------------//
  private void heading (File file)
  // ---------------------------------------------------------------------------------//
  {
    long fileLength = file.length ();

    System.out.println ("-------------------------------------------------------------");
    System.out.printf ("File name   : %s%n", file.getName ());
    System.out.printf ("File length : %,d%n", fileLength);
    System.out.println ("-------------------------------------------------------------");
  }

  // ---------------------------------------------------------------------------------//
  private byte[] read (Path fileName)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      return Files.readAllBytes (fileName);
    }
    catch (IOException e)
    {
      e.printStackTrace ();
      return null;
    }
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args) throws IOException
  // ---------------------------------------------------------------------------------//
  {
    new Tester1 ();
  }
}
