package com.bytezone.utility;

import java.io.IOException;
import java.io.InputStream;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// -----------------------------------------------------------------------------------//
public class Utility
// -----------------------------------------------------------------------------------//
{
  private static String[] hex =
      { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
  private static final int MAX_SHORT = 0xFFFF;
  private static final List<String> suffixes = List.of ("po", "dsk", "do", "hdv", "2mg", "d13",
      "sdk", "shk", "bxy", "bny", "bqy", "woz", "img", "dimg", "zip", "gz");

  // ---------------------------------------------------------------------------------//
  public static List<String> getSuffixes ()
  // ---------------------------------------------------------------------------------//
  {
    return suffixes;
  }

  // ---------------------------------------------------------------------------------//
  public static int getSuffixNo (String filename)
  // ---------------------------------------------------------------------------------//
  {
    return suffixes.indexOf (getSuffix (filename));
  }

  // ---------------------------------------------------------------------------------//
  public static String getSuffix (String filename)
  // ---------------------------------------------------------------------------------//
  {
    String lcFilename = filename.toLowerCase ();

    int dotPos = lcFilename.lastIndexOf ('.');
    if (dotPos < 0)
      return "";

    return lcFilename.substring (dotPos + 1);
  }

  // ---------------------------------------------------------------------------------//
  public static int unsignedShort (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      return (buffer[ptr] & 0xFF)             //
          | ((buffer[ptr + 1] & 0xFF) << 8);
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      System.out.printf ("Index out of range (unsignedShort): %d > %d%n", ptr, buffer.length);
      return 0;
    }
  }

  // ---------------------------------------------------------------------------------//
  public static int signedShort (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    int val = unsignedShort (buffer, ptr);

    return ((val & 0x8000) == 0) ? val : val - MAX_SHORT - 1;
  }

  // ---------------------------------------------------------------------------------//
  public static int unsignedTriple (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      return (buffer[ptr] & 0xFF)             //
          | (buffer[ptr + 1] & 0xFF) << 8     //
          | (buffer[ptr + 2] & 0xFF) << 16;
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      System.out.printf ("Index out of range (unsignedTriple): %d > %d%n", ptr, buffer.length);
      return 0;
    }
  }

  // ---------------------------------------------------------------------------------//
  public static int unsignedLong (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      return (buffer[ptr] & 0xFF)             //
          | (buffer[ptr + 1] & 0xFF) << 8     //
          | (buffer[ptr + 2] & 0xFF) << 16    //
          | (buffer[ptr + 3] & 0xFF) << 24;
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      System.out.printf ("Index out of range (unsignedLong): %d > %d%n", ptr, buffer.length);
      return 0;
    }
  }

  // ---------------------------------------------------------------------------------//
  public static void writeShort (byte[] buffer, int ptr, int value)
  // ---------------------------------------------------------------------------------//
  {
    buffer[ptr] = (byte) (value & 0xFF);
    buffer[ptr + 1] = (byte) ((value & 0xFF00) >>> 8);
  }

  // ---------------------------------------------------------------------------------//
  public static void writeTriple (byte[] buffer, int ptr, int value)
  // ---------------------------------------------------------------------------------//
  {
    buffer[ptr] = (byte) (value & 0xFF);
    buffer[ptr + 1] = (byte) ((value & 0xFF00) >>> 8);
    buffer[ptr + 2] = (byte) ((value & 0xFF0000) >>> 16);
  }

  // ---------------------------------------------------------------------------------//
  public static String format (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    return format (buffer, 0, buffer.length, true, 0);
  }

  // ---------------------------------------------------------------------------------//
  public static String format (byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    return format (buffer, offset, length, true, 0);
  }

  // ---------------------------------------------------------------------------------//
  public static String format (byte[] buffer, int offset, int length, boolean header,
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
          line.append (String.format ("%02X  %3d   %d%n", i, freq[i], Integer.bitCount (i)));
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
  public static String getPascalString (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    int length = buffer[offset] & 0xFF;
    return new String (buffer, offset + 1, length);
  }

  // ---------------------------------------------------------------------------------//
  public static String getMaskedPascalString (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    int length = buffer[offset] & 0xFF;
    byte[] text = new byte[length];

    for (int i = 0; i < length; i++)
      text[i] = (byte) (buffer[offset + i + 1] & 0x7F);

    return new String (text);
  }

  // ---------------------------------------------------------------------------------//
  public static String getCString (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    int length = 0;
    int ptr = offset;

    while (buffer[ptr++] != 0)
      ++length;

    return new String (buffer, offset, length);
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

      if (hour > 23)
        hour = 0;
      if (minute > 59)
        minute = 0;

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
        System.out.printf ("Bad date/time: %d %d %d %d %d %n", year, month, day, hour, minute);
      }
    }

    return null;
  }

  // ---------------------------------------------------------------------------------//
  public static Optional<LocalDateTime> appleDateTime (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    if (buffer[offset] == 0 && buffer[offset + 1] == 00)
      return Optional.empty ();

    int date = unsignedLong (buffer, offset);     // reverses bytes
    int year = (date & 0xFE00) >>> 9;             // 7 bits (no sign extension)
    int month = (date & 0x01E0) >> 5;             // 4 bits
    int day = date & 0x001F;                      // 5 bits

    int hour = (date & 0xFF000000) >>> 24;        // 5 bits (no sign extension)
    int minute = (date & 0x00FF0000) >> 16;       // 6 bits

    year += year < 70 ? 2000 : 1900;
    if (minute >= 128)
      minute -= 128;

    try
    {
      while (month > 12)    // see PRODOS111_1.HDV
      {
        month -= 12;
        year++;
      }

      if (hour == 0xFF && minute == 0)
        return Optional.of (LocalDateTime.of (year, month, day, 0, 0));

      return Optional.of (LocalDateTime.of (year, month, day, hour, minute));
    }
    catch (DateTimeException e)
    {
      System.out.println ("DateTimeException:");
      System.out.printf ("Date: %02X%02X%n", buffer[offset] & 0xFF, buffer[offset + 1] & 0xFF);
      System.out.printf ("Time: %02X%02X%n", buffer[offset + 2] & 0xFF, buffer[offset + 3] & 0xFF);
      System.out.printf ("Year ..... %d%n", year);
      System.out.printf ("Month .... %d%n", month);
      System.out.printf ("Day ...... %d%n", day);
      System.out.printf ("Hour ..... %02X%n", hour);
      System.out.printf ("Minute ... %02X%n", minute);
    }
    return Optional.empty ();
  }

  // ---------------------------------------------------------------------------------//
  public static void putAppleDate (byte[] buffer, int offset, LocalDateTime date)
  // ---------------------------------------------------------------------------------//
  {
    if (date != null)
    {
      int year = date.getYear ();
      int month = date.getMonthValue ();
      int day = date.getDayOfMonth ();
      int hour = date.getHour ();
      int minute = date.getMinute ();

      if (year < 2000)
        year -= 1900;
      else
        year -= 2000;

      int val1 = year << 9 | month << 5 | day;
      writeShort (buffer, offset, val1);
      buffer[offset + 2] = (byte) minute;
      buffer[offset + 3] = (byte) hour;
    }
  }

  // ---------------------------------------------------------------------------------//
  public static LocalDate getPascalLocalDate (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    int date = Utility.unsignedShort (buffer, offset);

    if (date == 0)
      return null;

    int month = date & 0x0F;
    int day = (date & 0x1F0) >>> 4;
    int year = (date & 0xFE00) >>> 9;

    if (year < 70)
      year += 2000;
    else
      year += 1900;

    try
    {
      return LocalDate.of (year, month, day);
    }
    catch (DateTimeException e)
    {
      return null;
    }
  }

  // ---------------------------------------------------------------------------------//
  public static boolean isMagic (byte[] buffer, int ptr, byte[] magic)
  // ---------------------------------------------------------------------------------//
  {
    if (ptr + magic.length >= buffer.length)
      return false;

    for (int i = 0; i < magic.length; i++)
      if (buffer[ptr + i] != magic[i])
        return false;

    return true;
  }

  // ---------------------------------------------------------------------------------//
  public static String matchFlags (int flag, String[] chars)
  // ---------------------------------------------------------------------------------//
  {
    int weight = (int) Math.pow (2, chars.length - 1);
    StringBuilder text = new StringBuilder ();

    for (int i = 0; i < chars.length; i++)
    {
      if ((flag & weight) != 0)
        text.append (chars[i]);
      else
        text.append (".");
      weight >>>= 1;
    }

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  public static byte[] getFullBuffer (InputStream zip) throws IOException
  // ---------------------------------------------------------------------------------//
  {
    List<byte[]> buffers = new ArrayList<> ();
    List<Integer> sizes = new ArrayList<> ();

    int bytesRead;
    int size = 0;
    int ptr = 0;

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

    return buffer;
  }

  // Used by FsNuFX and FileNuFX (multiple bytes)
  // ---------------------------------------------------------------------------------//
  public static int crc16 (final byte[] buffer, int length, int crc)
  // ---------------------------------------------------------------------------------//
  {
    for (int i = 0; i < length; i++)
    {
      crc = ((crc >>> 8) | (crc << 8)) & 0xFFFF;
      crc ^= (buffer[i] & 0xFF);
      crc ^= ((crc & 0xFF) >>> 4);
      crc ^= (crc << 12) & 0xFFFF;
      crc ^= ((crc & 0xFF) << 5) & 0xFFFF;
    }

    return crc &= 0xFFFF;
  }

  // (same as crc16 but for a single byte)
  // ---------------------------------------------------------------------------------//
  public static int crc16 (int crc, int val)
  // ---------------------------------------------------------------------------------//
  {
    crc ^= val << 8;

    for (int i = 0; i < 8; i++)
      if ((crc & 0x8000) == 0)
        crc <<= 1;
      else
        crc = crc << 1 ^ 0x1021;

    return crc & 0xFFFF;
  }

  // (same as crc16 but using a table)
  // ---------------------------------------------------------------------------------//
  public static int calcCrc16 (int crc, int val)
  // ---------------------------------------------------------------------------------//
  {
    return (Utility.crc16_tab[((crc >>> 8) & 0xFF) ^ val] ^ (crc << 8)) & 0xFFFF;
  }

  // Used by WozFile
  // ---------------------------------------------------------------------------------//
  public static int crc32 (byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    int crc = 0xFFFFFFFF;        // one's complement of zero
    int eof = offset + length;

    for (int i = offset; i < eof; i++)
      crc = crc32_tab[(crc ^ buffer[i]) & 0xFF] ^ (crc >>> 8);

    return ~crc;                 // one's complement
  }

  // ---------------------------------------------------------------------------------//
  static int[] crc16_tab = { //
      0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5, 0x60c6, 0x70e7, 0x8108, 0x9129, 0xa14a,
      0xb16b, 0xc18c, 0xd1ad, 0xe1ce, 0xf1ef, 0x1231, 0x0210, 0x3273, 0x2252, 0x52b5, 0x4294,
      0x72f7, 0x62d6, 0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c, 0xf3ff, 0xe3de, 0x2462,
      0x3443, 0x0420, 0x1401, 0x64e6, 0x74c7, 0x44a4, 0x5485, 0xa56a, 0xb54b, 0x8528, 0x9509,
      0xe5ee, 0xf5cf, 0xc5ac, 0xd58d, 0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6, 0x5695,
      0x46b4, 0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df, 0xe7fe, 0xd79d, 0xc7bc, 0x48c4, 0x58e5,
      0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823, 0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948,
      0x9969, 0xa90a, 0xb92b, 0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71, 0x0a50, 0x3a33, 0x2a12,
      0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a, 0x6ca6, 0x7c87, 0x4ce4,
      0x5cc5, 0x2c22, 0x3c03, 0x0c60, 0x1c41, 0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a, 0xbd0b,
      0x8d68, 0x9d49, 0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70, 0xff9f,
      0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a, 0x9f59, 0x8f78, 0x9188, 0x81a9, 0xb1ca, 0xa1eb,
      0xd10c, 0xc12d, 0xf14e, 0xe16f, 0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046,
      0x6067, 0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c, 0xe37f, 0xf35e, 0x02b1, 0x1290,
      0x22f3, 0x32d2, 0x4235, 0x5214, 0x6277, 0x7256, 0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e,
      0xe54f, 0xd52c, 0xc50d, 0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
      0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f, 0xf77e, 0xc71d, 0xd73c, 0x26d3, 0x36f2, 0x0691,
      0x16b0, 0x6657, 0x7676, 0x4615, 0x5634, 0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9,
      0xb98a, 0xa9ab, 0x5844, 0x4865, 0x7806, 0x6827, 0x18c0, 0x08e1, 0x3882, 0x28a3, 0xcb7d,
      0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a, 0x4a75, 0x5a54, 0x6a37, 0x7a16,
      0x0af1, 0x1ad0, 0x2ab3, 0x3a92, 0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa, 0xad8b, 0x9de8,
      0x8dc9, 0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1, 0xef1f, 0xff3e,
      0xcf5d, 0xdf7c, 0xaf9b, 0xbfba, 0x8fd9, 0x9ff8, 0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93,
      0x3eb2, 0x0ed1, 0x1ef0 };

  // ---------------------------------------------------------------------------------//
  static int[] crc32_tab = { //
      0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419, 0x706af48f, 0xe963a535,
      0x9e6495a3, 0x0edb8832, 0x79dcb8a4, 0xe0d5e91e, 0x97d2d988, 0x09b64c2b, 0x7eb17cbd,
      0xe7b82d07, 0x90bf1d91, 0x1db71064, 0x6ab020f2, 0xf3b97148, 0x84be41de, 0x1adad47d,
      0x6ddde4eb, 0xf4d4b551, 0x83d385c7, 0x136c9856, 0x646ba8c0, 0xfd62f97a, 0x8a65c9ec,
      0x14015c4f, 0x63066cd9, 0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e, 0xd56041e4,
      0xa2677172, 0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b, 0x35b5a8fa, 0x42b2986c,
      0xdbbbc9d6, 0xacbcf940, 0x32d86ce3, 0x45df5c75, 0xdcd60dcf, 0xabd13d59, 0x26d930ac,
      0x51de003a, 0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423, 0xcfba9599, 0xb8bda50f,
      0x2802b89e, 0x5f058808, 0xc60cd9b2, 0xb10be924, 0x2f6f7c87, 0x58684c11, 0xc1611dab,
      0xb6662d3d, 0x76dc4190, 0x01db7106, 0x98d220bc, 0xefd5102a, 0x71b18589, 0x06b6b51f,
      0x9fbfe4a5, 0xe8b8d433, 0x7807c9a2, 0x0f00f934, 0x9609a88e, 0xe10e9818, 0x7f6a0dbb,
      0x086d3d2d, 0x91646c97, 0xe6635c01, 0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e,
      0x6c0695ed, 0x1b01a57b, 0x8208f4c1, 0xf50fc457, 0x65b0d9c6, 0x12b7e950, 0x8bbeb8ea,
      0xfcb9887c, 0x62dd1ddf, 0x15da2d49, 0x8cd37cf3, 0xfbd44c65, 0x4db26158, 0x3ab551ce,
      0xa3bc0074, 0xd4bb30e2, 0x4adfa541, 0x3dd895d7, 0xa4d1c46d, 0xd3d6f4fb, 0x4369e96a,
      0x346ed9fc, 0xad678846, 0xda60b8d0, 0x44042d73, 0x33031de5, 0xaa0a4c5f, 0xdd0d7cc9,
      0x5005713c, 0x270241aa, 0xbe0b1010, 0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409,
      0xce61e49f, 0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17, 0x2eb40d81,
      0xb7bd5c3b, 0xc0ba6cad, 0xedb88320, 0x9abfb3b6, 0x03b6e20c, 0x74b1d29a, 0xead54739,
      0x9dd277af, 0x04db2615, 0x73dc1683, 0xe3630b12, 0x94643b84, 0x0d6d6a3e, 0x7a6a5aa8,
      0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1, 0xf00f9344, 0x8708a3d2, 0x1e01f268,
      0x6906c2fe, 0xf762575d, 0x806567cb, 0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0,
      0x10da7a5a, 0x67dd4acc, 0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5, 0xd6d6a3e8,
      0xa1d1937e, 0x38d8c2c4, 0x4fdff252, 0xd1bb67f1, 0xa6bc5767, 0x3fb506dd, 0x48b2364b,
      0xd80d2bda, 0xaf0a1b4c, 0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55, 0x316e8eef,
      0x4669be79, 0xcb61b38c, 0xbc66831a, 0x256fd2a0, 0x5268e236, 0xcc0c7795, 0xbb0b4703,
      0x220216b9, 0x5505262f, 0xc5ba3bbe, 0xb2bd0b28, 0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7,
      0xb5d0cf31, 0x2cd99e8b, 0x5bdeae1d, 0x9b64c2b0, 0xec63f226, 0x756aa39c, 0x026d930a,
      0x9c0906a9, 0xeb0e363f, 0x72076785, 0x05005713, 0x95bf4a82, 0xe2b87a14, 0x7bb12bae,
      0x0cb61b38, 0x92d28e9b, 0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21, 0x86d3d2d4, 0xf1d4e242,
      0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b, 0x6fb077e1, 0x18b74777, 0x88085ae6,
      0xff0f6a70, 0x66063bca, 0x11010b5c, 0x8f659eff, 0xf862ae69, 0x616bffd3, 0x166ccf45,
      0xa00ae278, 0xd70dd2ee, 0x4e048354, 0x3903b3c2, 0xa7672661, 0xd06016f7, 0x4969474d,
      0x3e6e77db, 0xaed16a4a, 0xd9d65adc, 0x40df0b66, 0x37d83bf0, 0xa9bcae53, 0xdebb9ec5,
      0x47b2cf7f, 0x30b5ffe9, 0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605,
      0xcdd70693, 0x54de5729, 0x23d967bf, 0xb3667a2e, 0xc4614ab8, 0x5d681b02, 0x2a6f2b94,
      0xb40bbe37, 0xc30c8ea1, 0x5a05df1b, 0x2d02ef8d };
}
