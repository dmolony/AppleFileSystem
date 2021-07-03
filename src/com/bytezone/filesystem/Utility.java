package com.bytezone.filesystem;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Utility
{
  // ---------------------------------------------------------------------------------//
  public static int unsignedShort (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    if (ptr >= buffer.length - 1)
    {
      System.out.printf ("Index out of range (unsigned short): %d > %d%n", ptr,
          buffer.length);
      return 0;
    }
    return (buffer[ptr] & 0xFF) | ((buffer[ptr + 1] & 0xFF) << 8);
  }

  // ---------------------------------------------------------------------------------//
  public static int unsignedTriple (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    return (buffer[ptr] & 0xFF) | (buffer[ptr + 1] & 0xFF) << 8
        | (buffer[ptr + 2] & 0xFF) << 16;
  }

  // ---------------------------------------------------------------------------------//
  static String format (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    return format (buffer, 0, buffer.length, true, 0);
  }

  private static String[] hex =
      { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

  // ---------------------------------------------------------------------------------//
  static String format (byte[] buffer, int offset, int length, boolean header,
      int startingAddress)
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder line = new StringBuilder ();
    int[] freq = new int[256];
    boolean startedOnBoundary = offset % 0x100 == 0;

    if (header)
    {
      line.append ("      ");
      for (int i = 0; i < 16; i++)
        line.append ("  " + hex[i]);
      if (offset == 0)
        line.append ("\n");
    }

    for (int i = offset; i < offset + length; i += 16)
    {
      if (line.length () > 0 && i > 0)
        line.append ("\n");
      if (i > offset && startedOnBoundary && (i % 0x200) == 0)
        line.append ("\n");

      // print offset
      line.append (String.format ("%05X : ", (startingAddress + i - offset)));

      // print hex values
      StringBuffer trans = new StringBuffer ();
      StringBuffer hexLine = new StringBuffer ();

      int max = Math.min (i + 16, offset + length);
      max = Math.min (max, buffer.length);
      for (int j = i; j < max; j++)
      {
        int c = buffer[j] & 0xFF;
        freq[c]++;
        hexLine.append (String.format ("%02X ", c));

        if (c > 127)
        {
          if (c < 160)
            c -= 64;
          else
            c -= 128;
        }
        if (c < 32 || c == 127)         // non-printable
          trans.append (".");
        else                            // standard ascii
          trans.append ((char) c);
      }
      while (hexLine.length () < 48)
        hexLine.append (" ");

      line.append (hexLine.toString () + ": " + trans.toString ());
    }

    if (false)
    {
      line.append ("\n\n");
      int totalBits = 0;
      for (int i = 0; i < freq.length; i++)
        if (freq[i] > 0)
        {
          totalBits += (Integer.bitCount (i) * freq[i]);
          line.append (
              String.format ("%02X  %3d   %d%n", i, freq[i], Integer.bitCount (i)));
        }
      line.append (String.format ("%nTotal bits : %d%n", totalBits));
    }
    return line.toString ();
  }

  // ---------------------------------------------------------------------------------//
  public static String string (byte[] buffer, int ptr, int nameLength)
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();
    for (int i = 0; i < nameLength; i++)
    {
      int c = buffer[ptr + i] & 0x7F;
      if (c < 32)
        c += 64;
      //      if (c >= 32)
      text.append ((char) c);
      //      else
      //        text.append (".");
    }
    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  public static LocalDateTime getAppleDate (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    int yymmdd = unsignedShort (buffer, offset);
    if (yymmdd != 0)
    {
      int year = (yymmdd & 0xFE00) >> 9;
      int month = (yymmdd & 0x01E0) >> 5;
      int day = yymmdd & 0x001F;

      int minute = buffer[offset + 2] & 0x3F;
      int hour = buffer[offset + 3] & 0x1F;

      if (year < 70)
        year += 2000;
      else
        year += 1900;

      try
      {
        return LocalDateTime.of (year, month, day, hour, minute);
      }
      catch (DateTimeException e)
      {
        System.out.printf ("Bad date/time: %d %d %d %d %d %n", year, month, day, hour,
            minute);
      }
    }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  public static LocalDate getPascalDate (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    int date = unsignedShort (buffer, offset);
    int month = date & 0x0F;
    int day = (date & 0x1F0) >>> 4;
    int year = (date & 0xFE00) >>> 9;

    if (year < 70)
      year += 2000;
    else
      year += 1900;

    return LocalDate.of (year, month, day);
  }
}
