package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatMeta;

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

    rawFileBuffer = new Buffer (buffer, 0, buffer.length);
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
    return rawFileBuffer.length ();
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

    formatMeta (text, "Compressed size", 8, (int) zipEntry.getCompressedSize ());
    formatMeta (text, "Size", 8, (int) zipEntry.getSize ());
    formatMeta (text, "Name", zipEntry.getName ());
    formatMeta (text, "Comment", comment);
    formatMeta (text, "CRC", 8, (int) zipEntry.getCrc ());
    formatMeta (text, "Creation time", creationTime);
    formatMeta (text, "Extra", extra);
    formatMeta (text, "Method", 2, zipEntry.getMethod ());
    formatMeta (text, "Is directory", zipEntry.isDirectory ());

    return Utility.rtrim (text);
  }
}
