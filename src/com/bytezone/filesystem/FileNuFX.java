package com.bytezone.filesystem;

import static com.bytezone.filesystem.ProdosConstants.fileTypes;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.DateTime;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileNuFX extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  private static final byte[] NuFX = { 0x4E, (byte) 0xF5, 0x46, (byte) 0xD8 };
  private static String[] fileSystems =
      { "", "ProDOS/SOS", "DOS 3.3", "DOS 3.2", "Apple II Pascal", "Macintosh HFS", "Macintosh MFS",
          "Lisa File System", "Apple CP/M", "", "MS-DOS", "High Sierra", "ISO 9660", "AppleShare" };
  private static String[] storage = { "", "Seedling", "Sapling", "Tree", "Pascal on Profile",
      "GS/OS Extended", "", "", "", "", "", "", "", "Subdirectory" };
  private static String[] accessChars = { "D", "R", "B", "", "", "I", "W", "R" };

  private final int crc;
  private final int attributeSectionLength;
  private final int version;
  private final int totThreads;
  private final int fileSystemID;
  private final char separator;
  private final int access;
  private final int fileType;
  private final int auxType;
  private final int storType;
  private final DateTime created;
  private final DateTime modified;
  private final DateTime archived;
  private final int optionSize;
  private final int fileNameLength;
  private String fileName;

  private boolean crcPassed;
  final List<NuFXThread> threads = new ArrayList<> ();
  int rawLength;

  // ---------------------------------------------------------------------------------//
  FileNuFX (FsNuFX fs, byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFile = true;

    if (!Utility.isMagic (buffer, offset, NuFX))
      throw new FileFormatException ("NuFX not found");

    crc = Utility.unsignedShort (buffer, offset + 4);
    attributeSectionLength = Utility.unsignedShort (buffer, offset + 6);
    version = Utility.unsignedShort (buffer, offset + 8);
    totThreads = Utility.unsignedLong (buffer, offset + 10);
    fileSystemID = Utility.unsignedShort (buffer, offset + 14);
    separator = (char) (buffer[offset + 16] & 0x00FF);
    access = Utility.unsignedLong (buffer, offset + 18);
    fileType = Utility.unsignedLong (buffer, offset + 22);
    auxType = Utility.unsignedLong (buffer, offset + 26);
    storType = Utility.unsignedShort (buffer, offset + 30);
    created = new DateTime (buffer, offset + 32);
    modified = new DateTime (buffer, offset + 40);
    archived = new DateTime (buffer, offset + 48);
    optionSize = Utility.unsignedShort (buffer, offset + 56);
    fileNameLength = Utility.unsignedShort (buffer, offset + attributeSectionLength - 2);

    int len = attributeSectionLength + fileNameLength - 6;
    byte[] crcBuffer = new byte[len + totThreads * 16];
    System.arraycopy (buffer, offset + 6, crcBuffer, 0, crcBuffer.length);

    crcPassed = crc == Utility.getCRC (crcBuffer, crcBuffer.length, 0);
    if (!crcPassed)
    {
      System.out.println ("***** Record CRC mismatch *****");
      throw new FileFormatException ("Record CRC failed");
    }

    int ptr = offset + attributeSectionLength + fileNameLength;
    int threadsPtr = ptr;
    ptr += totThreads * 16;           // beginning of data

    for (int i = 0; i < totThreads; i++)
    {
      NuFXThread thread = new NuFXThread (buffer, threadsPtr + i * 16, ptr);
      threads.add (thread);
      ptr += thread.getCompressedEOF ();
    }

    if (fileNameLength > 0)
    {
      int start = offset + attributeSectionLength;
      int end = start + fileNameLength;
      for (int i = start; i < end; i++)
        buffer[i] &= 0x7F;
      fileName = new String (buffer, start, fileNameLength);
    }
    else
      for (int i = 0; i < totThreads; i++)
      {
        NuFXThread thread = threads.get (i);
        if (thread.threadClass == 3 && thread.threadKind == 0)
        {
          fileName = thread.getDataString ();
          break;
        }
      }

    System.out.println (this);
    System.out.println ();

    for (int i = 0; i < totThreads; i++)
    {
      System.out.println (threads.get (i));
      System.out.println ();
    }

    rawLength = ptr - offset;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    String bits = "00000000" + Integer.toBinaryString (access & 0xFF);
    bits = bits.substring (bits.length () - 8);
    String decode = Utility.matchFlags (access, accessChars);

    text.append (
        String.format ("Header CRC ..... %04X   %s%n", crc, crcPassed ? "Passed" : "** Failed **"));
    text.append (String.format ("Attributes ..... %d%n", attributeSectionLength));
    text.append (String.format ("Version ........ %d%n", version));
    text.append (String.format ("Threads ........ %d%n", totThreads));
    text.append (
        String.format ("File sys id .... %02X     %s%n", fileSystemID, fileSystems[fileSystemID]));
    text.append (String.format ("Separator ...... %s%n", separator));
    text.append (String.format ("Access ......... %s  %s%n", bits, decode));

    if (storType < 16)
    {
      text.append (String.format ("File type ...... %02X     %s%n", fileType, fileTypes[fileType]));
      text.append (String.format ("Aux type ....... %04X%n", auxType));
      text.append (String.format ("Stor type ...... %02X     %s%n", storType, storage[storType]));
    }
    else
    {
      text.append (String.format ("Zero ........... %,d%n", fileType));
      text.append (String.format ("Total blocks ... %,d%n", auxType));
      text.append (String.format ("Block size ..... %,d%n", storType));
    }

    text.append (String.format ("Created ........ %s%n", created.format ()));
    text.append (String.format ("Modified ....... %s%n", modified.format ()));
    text.append (String.format ("Archived ....... %s%n", archived.format ()));
    text.append (String.format ("Option size .... %,d%n", optionSize));
    text.append (String.format ("Filename len ... %,d%n", fileNameLength));
    text.append (String.format ("Filename ....... %s", fileName));

    return text.toString ();
  }
}
