package com.bytezone.filesystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import com.bytezone.utility.Utility;

// https://docs.fileformat.com/compression/gzip/
// https://docs.fileformat.com/compression/gz/
// -----------------------------------------------------------------------------------//
class FsGzip extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] GZIP = { 0x1F, (byte) 0x8B };

  boolean debug = false;

  // ---------------------------------------------------------------------------------//
  FsGzip (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.GZIP);

    Buffer dataRecord = getDiskBuffer ();

    try (GZIPInputStream zip = new GZIPInputStream (  //
        new ByteArrayInputStream (                    //
            dataRecord.data (), dataRecord.offset (), dataRecord.length ()));)
    {
      addFileSystem (blockReader.getName (), Utility.getFullBuffer (zip));
    }
    catch (IOException e)
    {
      throw new FileFormatException (e.getMessage ());
    }

    assert Utility.isMagic (dataRecord, 0, GZIP);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return super.toString ();
  }
}
