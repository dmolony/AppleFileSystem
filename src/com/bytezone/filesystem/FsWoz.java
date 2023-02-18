package com.bytezone.filesystem;

import com.bytezone.woz.DiskNibbleException;
import com.bytezone.woz.WozFile;

// -----------------------------------------------------------------------------------//
public class FsWoz extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] WOZ_1 = { 0x57, 0x4F, 0x5A, 0x31, (byte) 0xFF, 0x0A, 0x0D, 0x0A };
  static final byte[] WOZ_2 = { 0x57, 0x4F, 0x5A, 0x32, (byte) 0xFF, 0x0A, 0x0D, 0x0A };

  boolean debug = false;

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
    {
      setFileSystemName ("WOZ-1");
      setFileSystemType (FileSystemType.WOZ1);
    }
    else if (blockReader.isMagic (0, WOZ_2))
    {
      setFileSystemName ("WOZ-2");
      setFileSystemType (FileSystemType.WOZ2);
    }
    else
      System.out.println ("Not woz");

    try
    {
      WozFile wozFile = new WozFile (getBuffer ());
      byte[] buffer = wozFile.getDiskBuffer ();

      addFileSystem (this, new BlockReader (getFileSystemName (), buffer, 0, buffer.length));
    }
    catch (DiskNibbleException e)
    {
      e.printStackTrace ();
    }
  }
}
