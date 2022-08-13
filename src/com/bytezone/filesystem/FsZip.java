package com.bytezone.filesystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsZip extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] ZIP = { 0x50, 0x4B, 0x03, 0x04 };

  boolean debug = false;

  // ---------------------------------------------------------------------------------//
  public FsZip (String name, byte[] buffer, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public FsZip (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);

    setFileSystemName ("Zip");

    assert Utility.isMagic (buffer, offset, ZIP);

  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    //    System.out.println (Utility.format (diskBuffer, fileOffset, fileLength));

    try (ZipInputStream zip = new ZipInputStream (new ByteArrayInputStream (getBuffer ()));)
    {
      ZipEntry entry;
      while ((entry = zip.getNextEntry ()) != null)
      {
        if (entry.getName ().startsWith ("__"))
          continue;

        //        System.out.print (entry);
        if (entry.isDirectory ())
          System.out.println (" : dir");
        else
        {
          if (debug)
          {
            System.out.println (" : file");
            System.out.println (entry.getCompressedSize ());
            System.out.println (entry.getSize ());
            System.out.println (entry.getName ());
            System.out.println (entry.getComment ());
            System.out.println (entry.getCrc ());
            System.out.println (entry.getCreationTime ());
            System.out.println (entry.getExtra ());
            System.out.println (entry.getMethod ());
          }

          int ptr = 0;
          int rem = (int) entry.getSize ();

          if (rem > 0)
          {
            byte[] buffer = new byte[rem];
            while (true)
            {
              int len = zip.read (buffer, ptr, rem);
              if (len == 0)
                break;

              ptr += len;
              rem -= len;
            }

            addFileSystem (this, entry.getName (), buffer);
          }
          else
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

            for (int i = 0; i < buffers.size (); i++)
            {
              System.arraycopy (buffers.get (i), 0, buffer, ptr, sizes.get (i));
              ptr += sizes.get (i);
            }

            addFileSystem (this, entry.getName (), buffer);
          }
        }
      }
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

  // ---------------------------------------------------------------------------------//
  @Override
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toText ());

    //    text.append (String.format ("Entry length .......... %d%n", entryLength));

    return text.toString ();
  }
}
