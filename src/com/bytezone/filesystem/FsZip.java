package com.bytezone.filesystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import com.bytezone.utility.Utility;

// https://docs.fileformat.com/compression/zip/
// -----------------------------------------------------------------------------------//
public class FsZip extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] ZIP = { 0x50, 0x4B, 0x03, 0x04 };

  private boolean debug = false;

  private List<ZipEntry> zipEntries = new ArrayList<> ();

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
    try (ZipInputStream zip = new ZipInputStream (//
        new ByteArrayInputStream (getBuffer (), getOffset (), getLength ()));)
    {
      ZipEntry entry;
      while ((entry = zip.getNextEntry ()) != null)
      {
        zipEntries.add (entry);

        if (entry.getName ().startsWith ("__") || entry.getName ().startsWith (".")
            || entry.isDirectory () || Utility.getSuffixNo (entry.getName ()) < 0)
        {
          if (debug)
            System.out.printf ("Ignoring : %s%n", entry.getName ());
          continue;
        }

        int rem = (int) entry.getSize ();

        if (rem > 0)
        {
          byte[] buffer = new byte[rem];
          int ptr = 0;

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
          addEntry (entry.getName (), Utility.getFullBuffer (zip));
      }
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

    for (ZipEntry entry : zipEntries)
    {
      System.out.println ();
      text.append (String.format ("Compressed size ... %,d%n", entry.getCompressedSize ()));
      text.append (String.format ("Size .............. %,d%n", entry.getSize ()));
      text.append (String.format ("Name .............. %s%n", entry.getName ()));
      text.append (String.format ("Comment ........... %s%n", entry.getComment ()));
      text.append (String.format ("CRC ............... %,d%n", entry.getCrc ()));
      text.append (String.format ("Creation time ..... %s%n", entry.getCreationTime ()));
      text.append (String.format ("Extra ............. %s%n", entry.getExtra ()));
      text.append (String.format ("Method ............ %,d%n", entry.getMethod ()));
      text.append (String.format ("Is directory ...... %s%n", entry.isDirectory ()));
    }

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
