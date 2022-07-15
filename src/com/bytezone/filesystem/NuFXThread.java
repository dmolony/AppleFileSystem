package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class NuFXThread
// -----------------------------------------------------------------------------------//
{
  private static String[] threadClassText = { "Message", "Control", "Data", "Filename" };
  private static String[] formatText = { "Uncompressed", "Huffman squeeze", "LZW/1", "LZW/2",
      "Unix 12-bit Compress", "Unix 16-bit Compress" };
  private static String[][] threadKindText = { { "ASCII text", "predefined EOF", "IIgs icon" },
      { "create directory", "undefined", "undefined" },
      { "data fork", "disk image", "resource fork" }, { "filename", "undefined", "undefined" } };

  private static final int DATA_FORK = 0;
  private static final int DISK_IMAGE = 1;
  private static final int RESOURCE_FORK = 2;

  final int threadClass;
  final int threadFormat;
  final int threadKind;

  final int threadCrc;
  final int uncompressedEOF;
  final int compressedEOF;

  private final byte[] data;
  private String fileName;
  private String message;
  //  private LZW lzw;

  private boolean hasDisk;
  private boolean hasFile;
  private boolean hasResource;
  private boolean hasFileName;

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

    data = new byte[compressedEOF];
    System.arraycopy (buffer, dataOffset, data, 0, data.length);
  }

  // ---------------------------------------------------------------------------------//
  int getCompressedEOF ()
  // ---------------------------------------------------------------------------------//
  {
    return compressedEOF;
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
    text.append (String.format ("  kind .............. %d          %s%n", threadKind,
        threadKindText[threadClass][threadKind]));
    text.append (String.format ("  crc ............... %04X%n", threadCrc));
    text.append (String.format ("  uncompressedEOF ... %08X %<,7d%n", uncompressedEOF));
    text.append (String.format ("  compressedEOF ..... %08X %<,7d", compressedEOF));

    if (fileName != null)
      text.append ("\n  filename .......... " + fileName);
    else if (message != null)
      text.append ("\n  message ........... " + message);
    //    else if (lzw != null)
    //    {
    //      text.append ("\n");
    //      text.append (lzw);
    //    }

    return text.toString ();
  }
}
