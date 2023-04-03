package com.bytezone.filesystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
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
  public FsZip (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.ZIP);

    try (ZipInputStream zip = new ZipInputStream (
        new ByteArrayInputStream (getDiskBuffer (), getDiskOffset (), getDiskLength ()));)
    {
      ZipEntry entry;
      while ((entry = zip.getNextEntry ()) != null)
      {
        zipEntries.add (entry);

        String name = entry.getName ();

        if (name.startsWith ("__"))
          continue;

        if (name.startsWith (".") || entry.isDirectory ()
            || Utility.getSuffixNo (name) < 0)
        {
          if (debug)
            System.out.printf ("Ignoring : %s%n", name);
          continue;
        }

        //        System.out.printf ("%s %,9d %,9d  %-20s%n", entry.isDirectory () ? "D" : " ",
        //            entry.getCompressedSize (), entry.getSize (), name);

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

          checkFileSystem (name, buffer);
        }
        else
        {
          byte[] buffer = Utility.getFullBuffer (zip);
          if (buffer.length > 0)
            checkFileSystem (name, buffer);
        }
      }
    }
    catch (ZipException e)
    {
      throw new FileFormatException (e.getMessage ());
    }
    catch (IOException e)
    {
      throw new FileFormatException (e.getMessage ());
    }

    assert blockReader.isMagic (0, ZIP);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    for (AppleFile file : getFiles ())
      text.append (
          String.format ("%-15s %s%n", file.getFileName (), file.getFileSystemType ()));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    for (ZipEntry entry : zipEntries)
    {
      String comment = entry.getComment ();
      if (comment == null)
        comment = "";
      FileTime fileTime = entry.getCreationTime ();
      String creationTime = fileTime == null ? "" : fileTime.toString ();
      byte[] bytes = entry.getExtra ();
      String extra = bytes == null ? "" : bytes.toString ();

      text.append ("\n");
      text.append (
          String.format ("Compressed size ... %,d%n", entry.getCompressedSize ()));
      text.append (String.format ("Size .............. %,d%n", entry.getSize ()));
      text.append (String.format ("Name .............. %s%n", entry.getName ()));
      text.append (String.format ("Comment ........... %s%n", comment));
      text.append (String.format ("CRC ............... %,d%n", entry.getCrc ()));
      text.append (String.format ("Creation time ..... %s%n", creationTime));
      text.append (String.format ("Extra ............. %s%n", extra));
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
