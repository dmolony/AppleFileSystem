package com.bytezone.filesystem;

import com.bytezone.utility.LZW1;
import com.bytezone.utility.LZW2;
import com.bytezone.utility.Squeeze;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class NuFXThread
// -----------------------------------------------------------------------------------//
{
  private static String[] threadClassText = { "Message", "Control", "Data", "Filename" };
  private static String[] formatText = { "Uncompressed", "Huffman squeeze", "LZW/1",
      "LZW/2", "Unix 12-bit Compress", "Unix 16-bit Compress" };

  private static String[][] threadKindText = {            //
      { "ASCII text", "predefined EOF", "IIgs icon" },    // message thread
      { "Create directory", "undefined", "undefined" },   // control thread
      { "Data fork", "Disk image", "Resource fork" },     // data thread
      { "Filename", "undefined", "undefined" } };         // filename thread

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

  // ---------------------------------------------------------------------------------//
  NuFXThread (byte[] buffer, int offset, int dataOffset)
  // ---------------------------------------------------------------------------------//
  {
    threadClass = Utility.unsignedShort (buffer, offset);
    threadFormat = Utility.unsignedShort (buffer, offset + 2);
    threadKind = Utility.unsignedShort (buffer, offset + 4);

    threadCrc = Utility.unsignedShort (buffer, offset + 6);
    uncompressedEOF = Utility.unsignedInt (buffer, offset + 8);
    compressedEOF = Utility.unsignedInt (buffer, offset + 12);

    compressedData = new byte[compressedEOF];

    System.arraycopy (buffer, dataOffset, compressedData, 0, compressedData.length);
  }

  // ---------------------------------------------------------------------------------//
  int getCompressedEOF ()
  // ---------------------------------------------------------------------------------//
  {
    return compressedEOF;
  }

  // ---------------------------------------------------------------------------------//
  int getUncompressedEOF ()
  // ---------------------------------------------------------------------------------//
  {
    return uncompressedEOF;
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
    return switch (threadFormat)
    {
      case 0 -> compressedData;
      case 1 -> new Squeeze ().unSqueeze (compressedData);
      case 2 -> new LZW1 (compressedData).getData ();
      case 3 -> new LZW2 (compressedData, threadCrc,
          threadKind == 1 ? 0 : uncompressedEOF).getData ();
      default -> null;
    };
  }

  // ---------------------------------------------------------------------------------//
  //  byte[] getDataOld ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    switch (threadFormat)
  //    {
  //      case 0:             // uncompressed
  //        return compressedData;
  //
  //      case 1:             // Huffman squeeze
  //        Squeeze squeeze = new Squeeze ();
  //        return squeeze.unSqueeze (compressedData);
  //
  //      case 2:             // Dynamic LZW/1 
  //        LZW1 lzw1 = new LZW1 (compressedData);
  //        return lzw1.getData ();
  //
  //      case 3:             // Dynamic LZW/2
  //        int crcLength = threadKind == 1 ? 0 : uncompressedEOF;
  //        LZW2 lzw2 = new LZW2 (compressedData, threadCrc, crcLength);
  //        return lzw2.getData ();
  //
  //      case 4:             // Unix 12-bit compress
  //        break;
  //
  //      case 5:             // Unix 16-bit compress
  //        break;
  //    }
  //
  //    return null;
  //  }

  // ---------------------------------------------------------------------------------//
  String getKindText ()
  // ---------------------------------------------------------------------------------//
  {
    return threadKindText[threadClass][threadKind];
  }

  // ---------------------------------------------------------------------------------//
  boolean hasDisk ()
  // ---------------------------------------------------------------------------------//
  {
    return threadClass == CLASS_DATA && threadKind == KIND_DISK_IMAGE;
  }

  // ---------------------------------------------------------------------------------//
  boolean hasData ()
  // ---------------------------------------------------------------------------------//
  {
    return threadClass == CLASS_DATA && threadKind == KIND_DATA_FORK;
  }

  // ---------------------------------------------------------------------------------//
  boolean hasResource ()
  // ---------------------------------------------------------------------------------//
  {
    return threadClass == CLASS_DATA && threadKind == KIND_RESOURCE_FORK;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    Utility.formatMeta (text, "  threadClass", 2, threadClass,
        threadClassText[threadClass]);
    Utility.formatMeta (text, "  format", 2, threadFormat, formatText[threadFormat]);
    Utility.formatMeta (text, "  kind", 2, threadKind, getKindText ());
    Utility.formatMeta (text, "  crc", 4, threadCrc);
    Utility.formatMeta (text, "  uncompressedEOF", 6, uncompressedEOF);
    Utility.formatMeta (text, "  compressedEOF", 6, compressedEOF);

    if (threadClass != CLASS_DATA)
    {
      //      text.append ("\n");
      Utility.formatMeta (text, "  data", getDataString ());
    }

    return Utility.rtrim (text);
  }
}
