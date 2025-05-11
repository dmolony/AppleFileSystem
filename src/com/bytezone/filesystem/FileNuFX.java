package com.bytezone.filesystem;

import static com.bytezone.filesystem.ProdosConstants.fileTypes;
import static com.bytezone.utility.Utility.formatMeta;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.DateTime;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileNuFX extends AbstractAppleFile implements AppleFilePath, AppleForkedFile
// -----------------------------------------------------------------------------------//
{
  private static final byte[] NuFX = { 0x4E, (byte) 0xF5, 0x46, (byte) 0xD8 };
  private static String[] fileSystems = { "", "ProDOS/SOS", "DOS 3.3", "DOS 3.2",
      "Apple II Pascal", "Macintosh HFS", "Macintosh MFS", "Lisa File System",
      "Apple CP/M", "", "MS-DOS", "High Sierra", "ISO 9660", "AppleShare" };
  private static String[] storage = { "", "Seedling", "Sapling", "Tree",
      "Pascal on Profile", "GS/OS Extended", "", "", "", "", "", "", "", "Subdirectory" };
  private static String[] accessChars = { "D", "R", "B", "", "", "I", "W", "R" };
  protected static final String[] threadFormats = { "unc", "sq ", "lz1", "lz2", "", "" };

  private final int crc;
  private final int attributeSectionLength;
  private final int version;
  private final int totThreads;
  private final int fileSystemID;
  private final char separator;
  private final int access;

  private final int auxType;
  private final int storType;
  private final DateTime created;
  private final DateTime modified;
  private final DateTime archived;
  private final int optionSize;
  private final int fileNameLength;
  private String fileName1 = "";

  private boolean crcPassed;
  final List<NuFXThread> threads = new ArrayList<> ();
  int rawLength;

  private int messageThreads;
  private int controlThreads;
  private int dataThreads;
  private int filenameThreads;
  private String threadKindText = "";

  private boolean isDiskImage;
  private NuFXThread diskImageThread;

  private ForkNuFX dataFork;            // for non-forked files only
  protected final List<AppleFile> forks = new ArrayList<> ();

  String fileName;
  int fileType;

  // A Record  
  // ---------------------------------------------------------------------------------//
  FileNuFX (FsNuFX fs, byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    if (!Utility.isMagic (buffer, offset, NuFX))
      throw new FileFormatException ("NuFX not found");

    crc = Utility.unsignedShort (buffer, offset + 4);
    attributeSectionLength = Utility.unsignedShort (buffer, offset + 6);
    version = Utility.unsignedShort (buffer, offset + 8);
    totThreads = Utility.unsignedInt (buffer, offset + 10);
    fileSystemID = Utility.unsignedShort (buffer, offset + 14);
    separator = (char) (buffer[offset + 16] & 0x00FF);
    access = Utility.unsignedInt (buffer, offset + 18);

    fileType = Utility.unsignedInt (buffer, offset + 22);
    auxType = Utility.unsignedInt (buffer, offset + 26);
    storType = Utility.unsignedShort (buffer, offset + 30);

    created = new DateTime (buffer, offset + 32);
    modified = new DateTime (buffer, offset + 40);
    archived = new DateTime (buffer, offset + 48);

    optionSize = Utility.unsignedShort (buffer, offset + 56);
    fileNameLength = Utility.unsignedShort (buffer, offset + attributeSectionLength - 2);

    int len = attributeSectionLength + fileNameLength - 6;
    byte[] crcBuffer = new byte[len + totThreads * 16];
    System.arraycopy (buffer, offset + 6, crcBuffer, 0, crcBuffer.length);

    crcPassed = crc == Utility.crc16 (crcBuffer, crcBuffer.length, 0);
    if (!crcPassed)
    {
      System.out.println ("***** Record CRC mismatch *****");
      throw new FileFormatException ("Record CRC failed");
    }

    int threadsPtr = offset + attributeSectionLength + fileNameLength;
    int ptr = threadsPtr + totThreads * 16;           // beginning of data

    for (int i = 0; i < totThreads; i++)
    {
      NuFXThread thread = new NuFXThread (buffer, threadsPtr + i * 16, ptr);
      threads.add (thread);
      ptr += thread.getCompressedEOF ();
    }

    fileName = getFileName (buffer, offset);
    countThreadTypes ();

    assert totThreads == messageThreads + controlThreads + dataThreads + filenameThreads;

    for (NuFXThread thread : threads)
      switch (thread.threadClass)
      {
        case NuFXThread.CLASS_FILENAME:
          break;

        case NuFXThread.CLASS_CONTROL:
          break;

        case NuFXThread.CLASS_MESSAGE:
          break;

        case NuFXThread.CLASS_DATA:
          switch (thread.threadKind)
          {
            case NuFXThread.KIND_DATA_FORK:
              ForkNuFX fork = new ForkNuFX (this, FileProdos.ForkType.DATA, thread);
              if (dataThreads == 2)
              {
                isForkedFile = true;
                forks.add (fork);
              }
              else
                dataFork = fork;
              break;

            case NuFXThread.KIND_DISK_IMAGE:
              isDiskImage = true;
              diskImageThread = thread;
              break;

            case NuFXThread.KIND_RESOURCE_FORK:
              isForkedFile = true;
              forks.add (new ForkNuFX (this, FileProdos.ForkType.RESOURCE, thread));
              break;

            default:
              break;
          }
      }

    rawLength = ptr - offset;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    if (hasDisk ())
      return getAuxType ();                           // total blocks on disk

    switch (storType)
    {
      case 1:                                         // seedling
        return 1;
      case 2:                                         // sapling
        return (getFileLength () - 1) / 512 + 2;
      case 3:                                         // tree
        return (getFileLength () - 1) / 512 + 3;                        // wrong
      case 5:                                         // forked file
        int size = 1;
        for (AppleFile fork : forks)
          size += fork.getTotalBlocks ();
        return size;                                                    // also wrong
      default:
        return 0;
    }
  }

  // ---------------------------------------------------------------------------------//
  public DateTime getArchived ()
  // ---------------------------------------------------------------------------------//
  {
    return archived;
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
    return fileType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return fileTypes[fileType];
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
  public boolean isValidFile ()
  // ---------------------------------------------------------------------------------//
  {
    return true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public char getSeparator ()
  // ---------------------------------------------------------------------------------//
  {
    return separator;
  }

  // ---------------------------------------------------------------------------------//
  public int getAuxType ()
  // ---------------------------------------------------------------------------------//
  {
    return auxType;
  }

  // ---------------------------------------------------------------------------------//
  public int getAccess ()
  // ---------------------------------------------------------------------------------//
  {
    return access;
  }

  // ---------------------------------------------------------------------------------//
  boolean isLibrary ()
  // ---------------------------------------------------------------------------------//
  {
    return fileType == 0xE0 && auxType == 0x8002;
  }

  // ---------------------------------------------------------------------------------//
  public boolean hasDisk ()
  // ---------------------------------------------------------------------------------//
  {
    for (NuFXThread thread : threads)
      if (thread.hasDisk ())
        return true;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  public boolean hasFile ()
  // ---------------------------------------------------------------------------------//
  {
    for (NuFXThread thread : threads)
      if (thread.hasDataFork ())
        return true;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  public boolean hasResource ()
  // ---------------------------------------------------------------------------------//
  {
    for (NuFXThread thread : threads)
      if (thread.hasResourceFork ())
        return true;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Buffer getRawFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    if (isForkedFile)
      throw new FileFormatException ("Cannot read() a forked file");

    if (rawFileBuffer != null)
      return rawFileBuffer;

    try           // some nufx files are corrupt
    {
      if (isDiskImage)
      {
        byte[] buffer = diskImageThread.getData ();
        rawFileBuffer = new Buffer (buffer, 0, buffer.length);
        return rawFileBuffer;
      }

      rawFileBuffer = dataFork.getRawFileBuffer ();
      return rawFileBuffer;
    }
    catch (Exception e)
    {
      errorMessage = String.format ("Reading NuFX file %s failed : %s%n",
          getFullFileName (), e.getMessage ());
      return null;
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getDataBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return null;
  }

  // ---------------------------------------------------------------------------------//
  private String getFileName (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    if (fileNameLength > 0)
    {
      int start = offset + attributeSectionLength;
      int end = start + fileNameLength;

      for (int i = start; i < end; i++)
        buffer[i] &= 0x7F;

      fileName1 = new String (buffer, start, fileNameLength);

      return fileName1;
    }

    for (NuFXThread thread : threads)
      if (thread.threadClass == NuFXThread.CLASS_FILENAME
          && thread.threadKind == NuFXThread.KIND_FILENAME)
        return thread.getDataString ();

    return "** Filename not found **";
  }

  // ---------------------------------------------------------------------------------//
  private void countThreadTypes ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    for (NuFXThread thread : threads)
      switch (thread.threadClass)
      {
        case NuFXThread.CLASS_MESSAGE:
          ++messageThreads;
          break;

        case NuFXThread.CLASS_CONTROL:
          ++controlThreads;
          break;

        case NuFXThread.CLASS_DATA:
          ++dataThreads;
          text.append (thread.getKindText () + "/");
          break;

        case NuFXThread.CLASS_FILENAME:
          ++filenameThreads;
          break;

        default:
          System.out.println ("Unknown thread class: " + thread.threadClass);
      }

    text.deleteCharAt (text.length () - 1);
    threadKindText = "(" + text.toString () + ")";
  }

  // ---------------------------------------------------------------------------------//
  public int getFileSystemId ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystemID;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    return getUncompressedSize ();
  }

  // ---------------------------------------------------------------------------------//
  public int getUncompressedSize ()
  // ---------------------------------------------------------------------------------//
  {
    if (hasDisk ())
      return auxType * storType;

    int size = 0;

    for (NuFXThread thread : threads)
      if (thread.hasDataFork () || thread.hasResourceFork () || thread.hasDisk ())
        size += thread.getUncompressedEOF ();

    return size;
  }

  // ---------------------------------------------------------------------------------//
  public int getCompressedSize ()
  // ---------------------------------------------------------------------------------//
  {
    int size = 0;

    for (NuFXThread thread : threads)
      if (thread.hasDataFork () || thread.hasResourceFork () || thread.hasDisk ())
        size += thread.compressedEOF;

    return size;
  }

  // ---------------------------------------------------------------------------------//
  public float getCompressedPct ()
  // ---------------------------------------------------------------------------------//
  {
    float pct = 100;

    if (getUncompressedSize () > 0)
      pct = getCompressedSize () * 100 / getUncompressedSize ();

    return pct;
  }

  // ---------------------------------------------------------------------------------//
  public int getThreadFormat ()
  // ---------------------------------------------------------------------------------//
  {
    for (NuFXThread thread : threads)
      if (thread.hasDataFork () || thread.hasDisk ())
        return thread.threadFormat;

    return 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getForks ()
  // ---------------------------------------------------------------------------------//
  {
    return forks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    if (!isForkedFile)
      throw new FileFormatException ("Cannot getCatalog() on a non-forked file");

    return "Forked NuFX catalog";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    String lockedFlag = (getAccess () | 0xC3) == 1 ? "+" : " ";
    String forkedFlag = hasResource () ? "+" : " ";

    if (hasEmbeddedFileSystem ())
      return String.format ("%s%-27.27s %-4s %-6s %-15s  %s  %3.0f%%   %7d", lockedFlag,
          getFileName (), "Disk", (getUncompressedSize () / 1024) + "k",
          getArchived ().format (), threadFormats[getThreadFormat ()],
          getCompressedPct (), getUncompressedSize ());

    return String.format ("%s%-27.27s %s%s $%04X  %-15s  %s  %3.0f%%   %7d", lockedFlag,
        getFullFileName (), getFileTypeText (), forkedFlag, getAuxType (),
        getArchived ().format (), threadFormats[getThreadFormat ()], getCompressedPct (),
        getUncompressedSize ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    String bits = "00000000" + Integer.toBinaryString (access & 0xFF);
    bits = bits.substring (bits.length () - 8);
    String decode = Utility.matchFlags (access, accessChars);

    if (!getFileName ().equals (getFullFileName ()))
      formatMeta (text, "Full file name", getFullFileName ());
    formatMeta (text, "Header CRC", 4, crc, crcPassed ? "Passed" : "** Failed **");
    formatMeta (text, "Attributes", 2, attributeSectionLength);
    formatMeta (text, "Version", 2, version);
    formatMeta (text, "Threads", 2, totThreads);
    formatMeta (text, "File sys id", 2, fileSystemID, fileSystems[fileSystemID]);
    formatMeta (text, "Separator", separator);
    formatMeta (text, "Access", bits, decode);

    if (storType < 16)
    {
      formatMeta (text, "File type", 2, fileType, fileTypes[fileType]);
      formatMeta (text, "Aux type", 4, auxType);
      formatMeta (text, "Storage type", 2, storType, storage[storType]);
      text.append ("\n");
    }
    else
    {
      formatMeta (text, "Zero", 2, fileType);
      formatMeta (text, "Total blocks", 2, auxType);
      formatMeta (text, "Block size", 2, storType);
      text.append ("\n");
    }

    formatMeta (text, "Created", created.format ());
    formatMeta (text, "Modified", modified.format ());
    formatMeta (text, "Archived", archived.format ());
    text.append ("\n");

    formatMeta (text, "Option size", 2, optionSize);
    formatMeta (text, "Filename len", 2, fileNameLength);
    formatMeta (text, "Filename", fileName1);
    text.append ("\n");

    formatMeta (text, "Message threads", 2, messageThreads);
    formatMeta (text, "Control threads", 2, controlThreads);
    formatMeta (text, "Data threads", 2, dataThreads, threadKindText);
    formatMeta (text, "Filename threads", 2, filenameThreads);
    text.append ("\n");

    for (NuFXThread thread : threads)
    {
      text.append (thread);
      text.append ("\n\n");
    }

    return Utility.rtrim (text);
  }
}
