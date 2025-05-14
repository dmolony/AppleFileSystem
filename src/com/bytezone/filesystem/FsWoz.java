package com.bytezone.filesystem;

import com.bytezone.woz.DiskNibbleException;
import com.bytezone.woz.WozFile;

// -----------------------------------------------------------------------------------//
class FsWoz extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] WOZ_1 = { 0x57, 0x4F, 0x5A, 0x31, (byte) 0xFF, 0x0A, 0x0D, 0x0A };
  static final byte[] WOZ_2 = { 0x57, 0x4F, 0x5A, 0x32, (byte) 0xFF, 0x0A, 0x0D, 0x0A };

  WozFile wozFile;

  boolean debug = false;

  // ---------------------------------------------------------------------------------//
  FsWoz (BlockReader blockReader, FileSystemType fileSystemType)
      throws DiskNibbleException
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, fileSystemType);

    wozFile = new WozFile (getDiskBuffer ().data ());
    byte[] buffer = wozFile.getDiskBuffer ();

    if (buffer != null)
      addFileSystem (fileSystemType.toString (), buffer);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (wozFile);

    return text.toString ();
  }
}
