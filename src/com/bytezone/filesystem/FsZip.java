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
        if (entry.getName ().startsWith ("__") || entry.getName ().startsWith (".")
            || Utility.getSuffixNo (entry.getName ()) < 0)
        {
          if (debug)
            System.out.printf ("Ignoring : %s%n", entry.getName ());
          continue;
        }

        if (debug)
        {
          System.out.println ();
          System.out.printf ("Compressed size ... %,d%n", entry.getCompressedSize ());
          System.out.printf ("Size .............. %,d%n", entry.getSize ());
          System.out.printf ("Name .............. %s%n", entry.getName ());
          System.out.printf ("Comment ........... %s%n", entry.getComment ());
          System.out.printf ("CRC ............... %,d%n", entry.getCrc ());
          System.out.printf ("Creation time ..... %s%n", entry.getCreationTime ());
          System.out.printf ("Extra ............. %s%n", entry.getExtra ());
          System.out.printf ("Method ............ %,d%n", entry.getMethod ());
          System.out.printf ("Is directory ...... %s%n", entry.isDirectory ());
        }

        if (entry.isDirectory ())
        {

        }
        else
        {
          int ptr = 0;
          int rem = (int) entry.getSize ();

          if (rem > 0)
          {
            //            System.out.println ("type 1");
            byte[] buffer = new byte[rem];
            while (true)
            {
              int len = zip.read (buffer, ptr, rem);
              if (len == 0)
                break;

              ptr += len;
              rem -= len;
            }

            addEntry (entry.getName (), buffer);
          }
          else
          {
            //            System.out.println ("type 2");
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

            addEntry (entry.getName (), buffer);
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
  private void addEntry (String path, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    String fileName;

    int pos = path.lastIndexOf ('/');
    if (pos < 0)
    {
      fileName = path;
      path = "";
    }
    else
    {
      fileName = path.substring (pos + 1);
      path = path.substring (0, pos);
    }

    AppleFile parent = this;
    if (!path.isEmpty ())
      for (String name : path.split ("/"))
        parent = getFolder (parent, name);

    AppleFileSystem fs = addFileSystem (parent, fileName, buffer);
  }

  // ---------------------------------------------------------------------------------//
  private FolderZip getFolder (AppleFile parent, String name)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleFile file : parent.getFiles ())
      if (file.getName ().equals (name) && file.isDirectory ())
        return (FolderZip) file;

    FolderZip folder = new FolderZip (this, name);
    parent.addFile (folder);

    return folder;
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

// Compression methods
// 0 - The file is stored (no compression)
// 1 - The file is Shrunk
// 2 - The file is Reduced with compression factor 1
// 3 - The file is Reduced with compression factor 2
// 4 - The file is Reduced with compression factor 3
// 5 - The file is Reduced with compression factor 4
// 6 - The file is Imploded
// 7 - Reserved for Tokenizing compression algorithm
// 8 - The file is Deflated
// 9 - Enhanced Deflating using Deflate64(tm)
// 10 - PKWARE Data Compression Library Imploding (old IBM TERSE)
// 11 - Reserved by PKWARE
// 12 - File is compressed using BZIP2 algorithm
// 13 - Reserved by PKWARE
// 14 - LZMA
// 15 - Reserved by PKWARE
// 16 - IBM z/OS CMPSC Compression
// 17 - Reserved by PKWARE
// 18 - File is compressed using IBM TERSE (new)
// 19 - IBM LZ77 z Architecture 
// 20 - deprecated (use method 93 for zstd)
// 93 - Zstandard (zstd) Compression 
// 94 - MP3 Compression 
// 95 - XZ Compression 
// 96 - JPEG variant
// 97 - WavPack compressed data
// 98 - PPMd version I, Rev 1
// 99 - AE-x encryption marker (see APPENDIX E)
// 
