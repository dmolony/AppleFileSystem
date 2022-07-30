package com.bytezone.filesystem;

import com.bytezone.utility.LZW1;
import com.bytezone.utility.LZW2;
import com.bytezone.utility.Squeeze;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class NuFXThread
// -----------------------------------------------------------------------------------//
{
  private static String[] threadClassText = { "Message", "Control", "Data", "Filename" };
  private static String[] formatText = { "Uncompressed", "Huffman squeeze", "LZW/1", "LZW/2",
      "Unix 12-bit Compress", "Unix 16-bit Compress" };

  private static String[][] threadKindText = {            //
      { "ASCII text", "predefined EOF", "IIgs icon" },    // message thread
      { "create directory", "undefined", "undefined" },   // control thread
      { "data fork", "disk image", "resource fork" },     // data thread
      { "filename", "undefined", "undefined" } };         // filename thread

  public static final int CLASS_MESSAGE = 0;
  public static final int CLASS_CONTROL = 1;
  public static final int CLASS_DATA = 2;
  public static final int CLASS_FILENAME = 3;

  public static final int KIND_DATA_FORK = 0;
  public static final int KIND_DISK_IMAGE = 1;
  public static final int KIND_RESOURCE_FORK = 2;

  public static final int KIND_FILENAME = 0;

  final int threadClass;
  final int threadFormat;
  final int threadKind;

  final int threadCrc;
  final int uncompressedEOF;
  final int compressedEOF;

  private final byte[] compressedData;
  private final byte[] uncompressedData;

  // ---------------------------------------------------------------------------------//
  public NuFXThread (byte[] buffer, int offset, int dataOffset)
  // ---------------------------------------------------------------------------------//
  {
    threadClass = Utility.unsignedShort (buffer, offset);
    threadFormat = Utility.unsignedShort (buffer, offset + 2);
    threadKind = Utility.unsignedShort (buffer, offset + 4);

    threadCrc = Utility.unsignedShort (buffer, offset + 6);
    uncompressedEOF = Utility.unsignedLong (buffer, offset + 8);
    compressedEOF = Utility.unsignedLong (buffer, offset + 12);

    compressedData = new byte[compressedEOF];
    uncompressedData = new byte[uncompressedEOF];

    System.arraycopy (buffer, dataOffset, compressedData, 0, compressedData.length);
  }

  // ---------------------------------------------------------------------------------//
  int getCompressedEOF ()
  // ---------------------------------------------------------------------------------//
  {
    return compressedEOF;
  }

  // ---------------------------------------------------------------------------------//
  byte[] getDataBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return uncompressedData;
  }

  // ---------------------------------------------------------------------------------//
  String getDataString ()
  // ---------------------------------------------------------------------------------//
  {
    return new String (getData ()).trim ();     // trim removes the trailing nulls
  }

  // ---------------------------------------------------------------------------------//
  byte[] getData ()
  // ---------------------------------------------------------------------------------//
  {
    switch (threadFormat)
    {
      case 0:             // uncompressed
        return compressedData;

      case 1:             // Huffman squeeze
        Squeeze squeeze = new Squeeze ();
        return squeeze.unSqueeze (compressedData);

      case 2:             // Dynamic LZW/1 
        LZW1 lzw1 = new LZW1 (compressedData);
        return lzw1.getData ();

      case 3:             // Dynamic LZW/2
        int crcLength = threadKind == 1 ? 0 : uncompressedEOF;
        LZW2 lzw2 = new LZW2 (compressedData, threadCrc, crcLength);
        return lzw2.getData ();

      case 4:             // Unix 12-bit compress
        break;

      case 5:             // Unix 16-bit compress
        break;
    }

    return uncompressedData;
  }

  // ---------------------------------------------------------------------------------//
  String getKindText ()
  // ---------------------------------------------------------------------------------//
  {
    return threadKindText[threadClass][threadKind];
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("  threadClass ....... %d          %s%n", threadClass,
        threadClassText[threadClass]));
    text.append (String.format ("  format ............ %d          %s%n", threadFormat,
        formatText[threadFormat]));
    text.append (
        String.format ("  kind .............. %d          %s%n", threadKind, getKindText ()));
    text.append (String.format ("  crc ............... %04X%n", threadCrc));
    text.append (String.format ("  uncompressedEOF ... %08X %<,7d%n", uncompressedEOF));
    text.append (String.format ("  compressedEOF ..... %08X %<,7d", compressedEOF));

    if (threadFormat == 0)
      text.append ("\n  data .............. " + getDataString ());

    return text.toString ();
  }
}
