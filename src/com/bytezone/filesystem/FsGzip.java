package com.bytezone.filesystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import com.bytezone.utility.Utility;

// https://docs.fileformat.com/compression/gzip/
// https://docs.fileformat.com/compression/gz/
// -----------------------------------------------------------------------------------//
public class FsGzip extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] GZIP = { 0x1F, (byte) 0x8B };

  boolean debug = false;

  // ---------------------------------------------------------------------------------//
  public FsGzip (String name, byte[] buffer, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public FsGzip (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);

    setFileSystemName ("GZip");
    readCatalog ();

    assert Utility.isMagic (buffer, offset, GZIP);
  }

  // ---------------------------------------------------------------------------------//
  private void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    try (GZIPInputStream zip = new GZIPInputStream (//
        new ByteArrayInputStream (getBuffer (), getOffset (), getLength ()));)
    {
      addFileSystem (this, getName (), Utility.getFullBuffer (zip));
    }
    catch (ZipException e)
    {
      throw new FileFormatException (e.getMessage ());
      //      e.printStackTrace ();
    }
    catch (IOException e)
    {
      throw new FileFormatException (e.getMessage ());
      //      e.printStackTrace ();
    }
  }
}
