package com.bytezone.filesystem;

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
      Path path = Path.of (fileNames[fileNo]);
      String name = path.toFile ().getName ();

      byte[] buffer = read (path);

      int offset = 0;
      int length = buffer.length;

      String prefix = new String (buffer, 0, 4);
      if ("2IMG".equals (prefix))
      {
        offset = Utility.unsignedShort (buffer, 8);
        length -= offset;
      }

      try
      {
        // Prodos
        FsProdos prodos = getProdos (name, buffer, offset, length);
        if (prodos != null)
          fileSystems.add (prodos);

        // Pascal
        FsPascal pascal = getPascal (name, buffer, offset, length);
        if (pascal != null)
          fileSystems.add (pascal);

        // Dos3.1
        if (length == 116_480)
        {
          FsDos dos = new FsDos (name, buffer, offset, length, dos31Reader);
          if (dos.catalogBlocks > 0)
            fileSystems.add (dos);
        }

        else if (length >= 143_360 && length <= 143_488)
        {
          // Dos3.3
          FsDos dos = getDos (name, buffer, offset, length);
          if (dos != null)
            fileSystems.add (dos);

          // CPM
          FsCpm cpm = getCpm (name, buffer, offset, length);
          if (cpm != null)
            fileSystems.add (cpm);

          // Dos4
          FsDos4 dos4 = new FsDos4 (name, buffer, offset, length, dos33Reader0);
          if (dos4.catalogBlocks > 0)
            fileSystems.add (dos4);
        }

        // Unidos
        else if (length == UNIDOS_SIZE * 2)
        {
          FsDos fs = new FsDos (name, buffer, 0, UNIDOS_SIZE, unidosReader);
          if (fs != null && fs.catalogBlocks > 0)
            fileSystems.add (fs);

          fs = new FsDos (name, buffer, UNIDOS_SIZE, UNIDOS_SIZE, unidosReader);
          if (fs != null && fs.catalogBlocks > 0)
            fileSystems.add (fs);
        }
      }
      catch (FileFormatException e)
      {
        System.out.println (e);
      }
    }

    for (AppleFileSystem fs : fileSystems)
    {
      System.out.println (fs.catalog ());
      System.out.println ();
    }
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
      System.exit (1);
      return null;            // stupid editor
    }
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args) throws IOException
  // ---------------------------------------------------------------------------------//
  {
    new Tester2 ();
  }
}
