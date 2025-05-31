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
class FsZip extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] ZIP = { 0x50, 0x4B, 0x03, 0x04 };

  private boolean debug = false;

  private List<ZipEntry> zipEntries = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  FsZip (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.ZIP);

    Buffer dataRecord = getDiskBuffer ();

    try (ZipInputStream zip =
        new ZipInputStream (new ByteArrayInputStream (dataRecord.data (),
            dataRecord.offset (), dataRecord.length ()));)
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

        if (false)
          System.out.printf ("%s %,9d %,9d  %-20s%n", entry.isDirectory () ? "D" : " ",
              entry.getCompressedSize (), entry.getSize (), name);

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

          addZipFile (new FileZip (this, name, buffer, entry));
        }
        else
        {
          byte[] buffer = Utility.getFullBuffer (zip);
          if (buffer.length > 0)
            addZipFile (new FileZip (this, name, buffer, entry));
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

    assert Utility.isMagic (dataRecord, 0, ZIP);
  }

  // ---------------------------------------------------------------------------------//
  private void addZipFile (FileZip file)
  // ---------------------------------------------------------------------------------//
  {
    addEmbeddedFileSystem (file, 0);            // create FS and embed it
    addFile (file);
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public String getCatalog ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    StringBuilder text = new StringBuilder ();
  //
  //    for (AppleFile file : getFiles ())
  //      text.append (file.getCatalogLine ());
  //
  //    for (AppleFileSystem fileSystem : getFileSystems ())
  //      text.append (String.format ("%-5s %s%n", fileSystem.getFileSystemType (),
  //          fileSystem.getFileName ()));
  //
  //    return text.toString ();
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    //    for (AppleFile file : files)
    //    {
    //      text.append (file);
    //      text.append ("\n\n");
    //    }

    return Utility.rtrim (text);
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
