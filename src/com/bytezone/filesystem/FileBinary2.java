package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bytezone.utility.Squeeze;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileBinary2 extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  private static String[] bin2Formats =
      { "Prodos or SOS", "Dos 3.3", "", "Dos 3.1 or 3.2", "Apple II Pascal" };
  private static String[] flags = { "compressed", "encrypted", "", "", "", "", "", "sparse" };

  private int headerBlockNo;

  private int accessCode;
  private int fileType;
  private int auxType;
  private int storageType;
  private int blocks;
  private int modDate;
  private int modTime;
  private int createDate;
  private int createTime;
  private int eof;
  private String nativeName;

  private int gAuxType;
  private int gAccess;
  private int gFileType;
  private int gStorage;
  private int gFileSize;
  private int gEof;

  private int diskSpace;
  private int osType;
  private int nativeFileType;
  private int phantomFile;
  private int dataFlags;
  private int version;
  private int filesFollowing;

  private Optional<LocalDateTime> created = Optional.empty ();
  private Optional<LocalDateTime> modified = Optional.empty ();

  private List<AppleBlock> dataBlocks = new ArrayList<> ();
  private String squeezeName;
  private boolean debug = false;
  private boolean validBlocks = true;

  // ---------------------------------------------------------------------------------//
  FileBinary2 (FsBinary2 fs, int headerBlockNo)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFile = true;
    this.headerBlockNo = headerBlockNo;

    byte[] buffer = fs.getBlock (headerBlockNo).read ();

    accessCode = buffer[3] & 0xFF;
    fileType = buffer[4] & 0xFF;
    auxType = Utility.unsignedShort (buffer, 5);
    storageType = buffer[7] & 0xFF;
    blocks = Utility.unsignedShort (buffer, 8);
    modDate = Utility.unsignedShort (buffer, 10);
    modTime = Utility.unsignedShort (buffer, 12);

    modified = Utility.appleDateTime (buffer, 10);
    created = Utility.appleDateTime (buffer, 14);

    createDate = Utility.unsignedShort (buffer, 14);
    createTime = Utility.unsignedShort (buffer, 16);

    eof = Utility.unsignedTriple (buffer, 20);
    name = Utility.getMaskedPascalString (buffer, 23);
    nativeName = Utility.getMaskedPascalString (buffer, 39);

    gAuxType = Utility.unsignedShort (buffer, 109);
    gAccess = buffer[111] & 0xFF;
    gFileType = buffer[112] & 0xFF;
    gStorage = buffer[113] & 0xFF;
    gFileSize = Utility.unsignedShort (buffer, 114);
    gEof = buffer[116] & 0xFF;
    diskSpace = Utility.unsignedLong (buffer, 117);

    osType = buffer[121] & 0xFF;
    nativeFileType = Utility.unsignedShort (buffer, 122);
    phantomFile = buffer[124] & 0xFF;
    dataFlags = buffer[125] & 0xFF;
    version = buffer[126] & 0xFF;
    filesFollowing = buffer[127] & 0xFF;

    int firstBlock = headerBlockNo + 1;
    int lastBlock = firstBlock + (eof - 1) / 128;

    for (int block = firstBlock; block <= lastBlock; block++)
    {
      if (!fs.isValidBlockNo (block))
      {
        if (debug)
          System.out.printf ("Invalid block %d in %s%n", block, getName ());
        validBlocks = false;
        break;
      }

      dataBlocks.add (fs.getBlock (block));
    }

    if (validBlocks && (isCompressed () || name.endsWith (".QQ")))
    {
      buffer = fs.getBlock (headerBlockNo + 1).read ();
      if (buffer[0] == 0x76 && buffer[1] == (byte) 0xFF)      // squeeze
        squeezeName = Utility.getCString (buffer, 4);
    }

    if (debug)
      System.out.println (toText ());
  }

  // ---------------------------------------------------------------------------------//
  boolean isValid ()
  // ---------------------------------------------------------------------------------//
  {
    return validBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getName ()
  // ---------------------------------------------------------------------------------//
  {
    return squeezeName == null ? super.getName () : squeezeName;
  }

  // ---------------------------------------------------------------------------------//
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return fileType;
  }

  // ---------------------------------------------------------------------------------//
  public int getAuxType ()
  // ---------------------------------------------------------------------------------//
  {
    return auxType;
  }

  // ---------------------------------------------------------------------------------//
  public int getEof ()
  // ---------------------------------------------------------------------------------//
  {
    return eof;
  }

  // ---------------------------------------------------------------------------------//
  public int getFilesFollowing ()
  // ---------------------------------------------------------------------------------//
  {
    return filesFollowing;
  }

  // ---------------------------------------------------------------------------------//
  public boolean isCompressed ()
  // ---------------------------------------------------------------------------------//
  {
    return (dataFlags & 0x80) != 0;
  }

  // ---------------------------------------------------------------------------------//
  private String getFlagsText (int dataFlags)
  // ---------------------------------------------------------------------------------//
  {
    int mask = 0x80;
    StringBuilder text = new StringBuilder ();
    for (int i = 0; i < flags.length; i++)
    {
      if ((dataFlags & mask) != 0)
      {
        text.append (flags[i] + ", ");
      }
      mask >>>= 1;
    }

    if (text.length () > 0)
      text.delete (text.length () - 2, text.length ());

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    String suffix = Utility.getSuffix (fileSystem.getName ());
    if (suffix.equals ("bqy") && squeezeName != null)
    {
      Squeeze squeeze = new Squeeze ();
      byte[] buffer = fileSystem.readBlocks (dataBlocks);
      //      System.out.println (Utility.format (buffer));
      return squeeze.unSqueeze (buffer);
    }

    return fileSystem.readBlocks (dataBlocks);
  }

  // ---------------------------------------------------------------------------------//
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Header block ...... %02X%n", headerBlockNo));
    text.append (String.format ("Access code ....... %02X%n", accessCode));
    text.append (String.format ("File type ......... %02X        %s%n", fileType,
        ProdosConstants.fileTypes[fileType]));
    text.append (String.format ("Aux type .......... %04X%n", auxType));
    text.append (String.format ("Storage type ...... %02X%n", storageType));
    text.append (String.format ("File size x 512 ... %02X      %<,7d%n", blocks));
    text.append (String.format ("Mod date .......... %04X    %s%n", modDate,
        modified.isPresent () ? modified.get () : ""));
    text.append (String.format ("Mod time .......... %04X%n", modTime));
    text.append (String.format ("Create date ....... %04X    %s%n", createDate,
        created.isPresent () ? created.get () : ""));
    text.append (String.format ("Create time ....... %04X%n", createTime));
    text.append (String.format ("EOF ............... %06X  %<,7d%n", eof));
    text.append (String.format ("File name ......... %s%n", getName ()));
    text.append (String.format ("Native name ....... %s%n", nativeName));

    text.append (String.format ("G Aux type ........ %04X  %<d%n", gAuxType));
    text.append (String.format ("G Access .......... %02X    %<d%n", gAccess));
    text.append (String.format ("G File type ....... %02X    %<d%n", gFileType));
    text.append (String.format ("G Storage ......... %02X    %<d%n", gStorage));
    text.append (String.format ("G File size ....... %04X  %<d%n", gFileSize));
    text.append (String.format ("G EOF ............. %02X    %<d%n", gEof));
    text.append (String.format ("Disk space ........ %,d   (all files)%n", diskSpace));

    text.append (String.format ("OS type ........... %02X  %s%n", osType,
        (osType >= 0 && osType < bin2Formats.length) ? bin2Formats[osType] : ""));
    text.append (String.format ("Native file type .. %04X%n", nativeFileType));
    text.append (String.format ("Phantom file ...... %02X%n", phantomFile));
    text.append (
        String.format ("Data flags ........ %02X %s%n", dataFlags, getFlagsText (dataFlags)));
    text.append (String.format ("Version ........... %02X%n", version));
    text.append (String.format ("Files following ... %02X%n", filesFollowing));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%,6d  %-20s  %02X  %04X %03X  %3d  %,8d", dataBlocks.size (), getName (),
        fileType, auxType, storageType, blocks, eof);
  }
}
