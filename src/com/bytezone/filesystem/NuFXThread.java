package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

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
  boolean hasDataFork ()
  // ---------------------------------------------------------------------------------//
  {
    return threadClass == CLASS_DATA && threadKind == KIND_DATA_FORK;
  }

  // ---------------------------------------------------------------------------------//
  boolean hasResourceFork ()
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

    formatText (text, "  threadClass", 2, threadClass, threadClassText[threadClass]);
    formatText (text, "  format", 2, threadFormat, formatText[threadFormat]);
    formatText (text, "  kind", 2, threadKind, getKindText ());
    formatText (text, "  crc", 4, threadCrc);
    formatText (text, "  uncompressedEOF", 6, uncompressedEOF);
    formatText (text, "  compressedEOF", 6, compressedEOF);

    if (threadClass != CLASS_DATA)
    {
      //      text.append ("\n");
      formatText (text, "  data", getDataString ());
    }

    return Utility.rtrim (text);
  }
}
