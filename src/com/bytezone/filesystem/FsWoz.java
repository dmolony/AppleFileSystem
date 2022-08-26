package com.bytezone.filesystem;

import com.bytezone.utility.Utility;
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
  public FsWoz (String name, byte[] buffer, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public FsWoz (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);

    if (Utility.isMagic (buffer, 0, WOZ_1))
      setFileSystemName ("Woz1");
    else if (Utility.isMagic (buffer, 0, WOZ_2))
      setFileSystemName ("Woz2");

    readCatalog ();
  }

  // ---------------------------------------------------------------------------------//
  private void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      byte[] buffer = new WozFile (getBuffer ()).getDiskBuffer ();
      addFileSystem (this, fileName, buffer);
    }
    catch (DiskNibbleException e)
    {
      e.printStackTrace ();
    }
  }
}
