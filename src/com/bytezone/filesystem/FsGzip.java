package com.bytezone.filesystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import com.bytezone.utility.Utility;

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

    assert Utility.isMagic (buffer, offset, GZIP);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    try (GZIPInputStream zip = new GZIPInputStream (new ByteArrayInputStream (getBuffer ()));)
    {
      List<byte[]> buffers = new ArrayList<> ();
      List<Integer> sizes = new ArrayList<> ();
      int bytesRead;
      int size = 0;

      while (true)
      {
        byte[] buffer = new byte[1024];
        bytesRead = zip.read (buffer);
        if (bytesRead < 0)
          break;
        buffers.add (buffer);
        sizes.add (bytesRead);
        size += bytesRead;
      }

      byte[] buffer = new byte[size];
      int ptr = 0;

      for (int i = 0; i < buffers.size (); i++)
      {
        System.arraycopy (buffers.get (i), 0, buffer, ptr, sizes.get (i));
        ptr += sizes.get (i);
      }

      addFileSystem (this, getName (), buffer);
    }
    catch (ZipException e)
    {
      e.printStackTrace ();
    }
    catch (IOException e)
    {
      e.printStackTrace ();
    }
  }
}
