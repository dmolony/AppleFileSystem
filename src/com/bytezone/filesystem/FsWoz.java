package com.bytezone.filesystem;

import java.nio.file.Path;

import com.bytezone.woz.DiskNibbleException;
import com.bytezone.woz.WozFile;

// -----------------------------------------------------------------------------------//
public class FsWoz extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] WOZ_1 = { 0x57, 0x4F, 0x5A, 0x32, (byte) 0xFF, 0x0A, 0x0D, 0x0A };
  static final byte[] WOZ_2 = { 0x57, 0x4F, 0x5A, 0x31, (byte) 0xFF, 0x0A, 0x0D, 0x0A };

  boolean debug = false;

  // ---------------------------------------------------------------------------------//
  public FsWoz (Path path, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (path, blockReader);

    readCatalog ();
  }

  // ---------------------------------------------------------------------------------//
  public FsWoz (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader);

    readCatalog ();
  }

  // ---------------------------------------------------------------------------------//
  private void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {

    if (blockReader.isMagic (0, WOZ_1))
      setFileSystemName ("Woz1");
    else if (blockReader.isMagic (0, WOZ_2))
      setFileSystemName ("Woz2");
    else
      System.out.println ("Not woz");

    try
    {
      byte[] buffer = new WozFile (getBuffer ()).getDiskBuffer ();

      BlockReader blockReader = new BlockReader (buffer, 0, buffer.length);
      addFileSystem (this, blockReader);
    }
    catch (DiskNibbleException e)
    {
      e.printStackTrace ();
    }
  }
}
