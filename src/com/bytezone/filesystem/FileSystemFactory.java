package com.bytezone.filesystem;

import static com.bytezone.filesystem.BlockReader.AddressType.BLOCK;
import static com.bytezone.filesystem.BlockReader.AddressType.SECTOR;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.diskbrowser.nufx.NuFX;
import com.bytezone.woz.DiskNibbleException;
import com.bytezone.woz.WozFile;

// -----------------------------------------------------------------------------------//
public class FileSystemFactory
// -----------------------------------------------------------------------------------//
{
  private static final byte[] NuFile = { 0x4E, (byte) 0xF5, 0x46, (byte) 0xE9, 0x6C, (byte) 0xE5 };
  private static final byte[] BIN2 = { 0x0A, 0x47, 0x4C };
  private static final byte[] TWO_IMG = { 0x32, 0x49, 0x4D, 0x47 };
  private static final byte[] WOZ_1 = { 0x57, 0x4F, 0x5A, 0x32, (byte) 0xFF, 0x0A, 0x0D, 0x0A };
  private static final byte[] WOZ_2 = { 0x57, 0x4F, 0x5A, 0x31, (byte) 0xFF, 0x0A, 0x0D, 0x0A };
  private static final int UNIDOS_SIZE = 409_600;

  static BlockReader blockReader0 = new BlockReader (512, BLOCK, 0, 0);     // Prodos
  static BlockReader blockReader1 = new BlockReader (512, BLOCK, 1, 8);     // Prodos
  static BlockReader cpmReader = new BlockReader (1024, BLOCK, 2, 4);       // CPM
  static BlockReader dos31Reader = new BlockReader (256, SECTOR, 0, 13);    // Dos 3.1
  static BlockReader dos33Reader0 = new BlockReader (256, SECTOR, 0, 16);   // Dos 3.3
  static BlockReader dos33Reader1 = new BlockReader (256, SECTOR, 1, 16);   // Dos 3.3
  static BlockReader unidosReader = new BlockReader (256, SECTOR, 0, 32);   // UniDos

  // ---------------------------------------------------------------------------------//
  private FileSystemFactory ()
  // ---------------------------------------------------------------------------------//
  {
  }

  // ---------------------------------------------------------------------------------//
  public static List<AppleFileSystem> getFileSystems (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    int offset = 0;
    int length = buffer.length;

    if (length == 143_488)
      length = 143_360;

    List<AppleFileSystem> fileSystems = new ArrayList<> ();

    if (Utility.isMagic (buffer, 0, TWO_IMG))
    {
      offset = Utility.unsignedShort (buffer, 8);
      length -= offset;
    }
    else if (Utility.isMagic (buffer, 0, NuFile))
    {
      NuFX nufx = new NuFX (buffer, name);
      buffer = nufx.getDiskBuffer ();
      length = buffer.length;
    }
    else if (Utility.isMagic (buffer, 0, WOZ_1) || Utility.isMagic (buffer, 0, WOZ_2))
    {
      try
      {
        buffer = new WozFile (buffer).getDiskBuffer ();
        length = buffer.length;
      }
      catch (DiskNibbleException e)
      {
        e.printStackTrace ();
      }
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

      if (length == 116_480)                  // Dos3.1
      {
        FsDos dos = new FsDos (name, buffer, offset, length, dos31Reader);
        if (dos.catalogBlocks > 0)
          fileSystems.add (dos);
      }
      else if (length == 143_360)
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
      else if (length == UNIDOS_SIZE * 2)       // Unidos
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

    return fileSystems;
  }

  // ---------------------------------------------------------------------------------//
  static FsDos getDos (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    return getDos (name, buffer, 0, buffer.length);
  }

  // ---------------------------------------------------------------------------------//
  static FsDos getDos (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    List<FsDos> disks = new ArrayList<> (2);

    for (int i = 0; i < 2; i++)
      try
      {
        FsDos fs = new FsDos (name, buffer, offset, length, (i == 0 ? dos33Reader0 : dos33Reader1));

        if (fs.catalogBlocks > 0)
          disks.add (fs);
      }
      catch (FileFormatException e)
      {
        System.out.println (e);       // loop around
      }

    if (disks.size () == 0)
      return null;

    if (disks.size () == 1)
      return disks.get (0);

    return disks.get (0).catalogBlocks > disks.get (1).catalogBlocks ? disks.get (0)
        : disks.get (1);
  }

  // ---------------------------------------------------------------------------------//
  static FsProdos getProdos (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {

    for (int i = 0; i < 2; i++)
      try
      {
        FsProdos prodos =
            new FsProdos (name, buffer, offset, length, (i == 0 ? blockReader0 : blockReader1));

        if (prodos.catalogBlocks > 0)
          return prodos;
      }
      catch (FileFormatException e)
      {
        //        System.out.println (e);
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  static FsPascal getPascal (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {

    for (int i = 0; i < 2; i++)
      try
      {
        FsPascal pascal =
            new FsPascal (name, buffer, offset, length, (i == 0 ? blockReader0 : blockReader1));

        if (pascal.catalogBlocks > 0)
          return pascal;
      }
      catch (FileFormatException e)
      {
        //          System.out.println (e);
      }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  static FsCpm getCpm (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      FsCpm cpm = new FsCpm (name, buffer, offset, length, cpmReader);

      if (cpm.catalogBlocks > 0)
        return cpm;
    }
    catch (FileFormatException e)
    {
      //          System.out.println (e);
    }

    return null;
  }
}
