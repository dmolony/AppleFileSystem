package com.bytezone.filesystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

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
  public FsGzip (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.GZIP);

    try (GZIPInputStream zip = new GZIPInputStream (//
        new ByteArrayInputStream (getDiskBuffer (), getDiskOffset (), getFileLength ()));)
    {
      addFileSystem (this, getFileName (), Utility.getFullBuffer (zip));
    }
    //    catch (ZipException e)
    //    {
    //      throw new FileFormatException (e.getMessage ());
    //    }
    catch (IOException e)
    {
      throw new FileFormatException (e.getMessage ());
    }

    assert blockReader.isMagic (0, GZIP);
  }
}
