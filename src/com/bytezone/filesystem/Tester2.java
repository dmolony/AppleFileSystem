package com.bytezone.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------------//
public class Tester2 extends Tester
// -----------------------------------------------------------------------------------//
{

  // ---------------------------------------------------------------------------------//
  Tester2 ()
  // ---------------------------------------------------------------------------------//
  {
    List<AppleFileSystem> fileSystems = new ArrayList<> ();

    for (int fileNo = 0; fileNo < fileNames.length; fileNo++)
    {
      fileSystems.clear ();

      Path path = Path.of (fileNames[fileNo]);
      String name = path.toFile ().getName ();

//      heading (path);
      byte[] buffer = read (path);

      int offset = 0;
      int length = buffer.length;

      String prefix = new String (buffer, 0, 4);
      if ("2IMG".equals (prefix))
      {
        offset = getWord (buffer, 8);
        length -= offset;
      }

      try
      {
        if (length >= 143_360 && length <= 143_488)
        {
          // Dos3.3
          FsDos dos = getDos (name, buffer, offset, length);
          if (dos != null && dos.catalogBlocks > 0)
            fileSystems.add (dos);

          // Pascal
          FsPascal pascal = getPascal (name, buffer, offset, length);
          if (pascal != null)
            fileSystems.add (pascal);

          // CPM
          FsCpm cpm = getCpm (name, buffer, offset, length);
          if (cpm != null)
            fileSystems.add (cpm);

          // Dos4
          FsDos4 dos4 = new FsDos4 (name, buffer, offset, length);
          dos4.setBlockReader (dos33Reader0);
          dos4.readCatalog ();
          if (dos4.catalogBlocks > 0)
            fileSystems.add (dos4);
        }

        // Prodos
        FsProdos prodos = getProdos (name, buffer, offset, length);
        if (prodos != null && prodos.catalogBlocks > 0)
          fileSystems.add (prodos);

        // Dos3.1
        if (length == 116_480)
        {
          FsDos dos = new FsDos (name, buffer, offset, length);
          dos.setBlockReader (dos31Reader);
          dos.readCatalog ();
          if (dos.catalogBlocks > 0)
            fileSystems.add (dos);
        }

        // Unidos
        if (length == UNIDOS_SIZE * 2)
        {
          FsDos fs = getDos (name, buffer, 0, UNIDOS_SIZE);
          fs.setBlockReader (unidosReader);
          fs.readCatalog ();
          if (fs.catalogBlocks > 0)
            fileSystems.add (fs);

          fs = getDos (name, buffer, UNIDOS_SIZE, UNIDOS_SIZE);
          fs.setBlockReader (unidosReader);
          fs.readCatalog ();
          if (fs.catalogBlocks > 0)
            fileSystems.add (fs);
        }
      }
      catch (FileFormatException e)
      {
        System.out.println (e);
      }

      for (AppleFileSystem fs : fileSystems)
      {
        System.out.println (fs.catalog ());
        System.out.println ();
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  private void heading (Path path)
  // ---------------------------------------------------------------------------------//
  {
    File file = path.toFile ();
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
  public static int getWord (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    int a = (buffer[ptr + 1] & 0xFF) << 8;
    int b = buffer[ptr] & 0xFF;
    return a + b;
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args) throws IOException
  // ---------------------------------------------------------------------------------//
  {
    new Tester2 ();
  }
}
