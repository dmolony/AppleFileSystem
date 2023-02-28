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
  public FsWoz (BlockReader blockReader, FileSystemType fileSystemType)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, fileSystemType);

    readCatalog ();
  }

  // ---------------------------------------------------------------------------------//
  private void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      WozFile wozFile = new WozFile (getDiskBuffer ());
      byte[] buffer = wozFile.getDiskBuffer ();

      addFileSystem (this,
          new BlockReader (getFileSystemType ().toString (), buffer, 0, buffer.length));
    }
    catch (DiskNibbleException e)
    {
      e.printStackTrace ();
    }
  }
}
