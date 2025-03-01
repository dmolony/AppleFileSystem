package com.bytezone.filesystem;

import java.nio.file.attribute.FileTime;
import java.util.zip.ZipEntry;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileZip extends AbstractAppleFile implements AppleFilePath
// -----------------------------------------------------------------------------------//
{
  private final char separator = '/';
  private final ZipEntry zipEntry;

  String fileName;

  // ---------------------------------------------------------------------------------//
  FileZip (FsZip fs, String fileName, byte[] buffer, ZipEntry zipEntry)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    fileBuffer = new Buffer (buffer, 0, buffer.length);
    this.fileName = fileName;
    this.zipEntry = zipEntry;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public char getSeparator ()
  // ---------------------------------------------------------------------------------//
  {
    return separator;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isValidFile ()
  // ---------------------------------------------------------------------------------//
  {
    return true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    int pos = fileName.lastIndexOf (separator);
    return pos < 0 ? fileName : fileName.substring (pos + 1);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFullFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return "???";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return false;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    return fileBuffer.length ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    String comment = zipEntry.getComment ();
    if (comment == null)
      comment = "";

    FileTime fileTime = zipEntry.getCreationTime ();
    String creationTime = fileTime == null ? "" : fileTime.toString ();
    byte[] bytes = zipEntry.getExtra ();
    String extra = bytes == null ? "" : "\n" + Utility.format (bytes);

    text.append (
        String.format ("Compressed size ....... %,d%n", zipEntry.getCompressedSize ()));
    text.append (String.format ("Size .................. %,d%n", zipEntry.getSize ()));
    text.append (String.format ("Name .................. %s%n", zipEntry.getName ()));
    text.append (String.format ("Comment ............... %s%n", comment));
    text.append (String.format ("CRC ................... %,d%n", zipEntry.getCrc ()));
    text.append (String.format ("Creation time ......... %s%n", creationTime));
    text.append (String.format ("Extra ................. %s%n", extra));
    text.append (String.format ("Method ................ %,d%n", zipEntry.getMethod ()));
    text.append (String.format ("Is directory .......... %s%n", zipEntry.isDirectory ()));

    return Utility.rtrim (text);
  }
}
