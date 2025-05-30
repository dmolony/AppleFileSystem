package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Squeeze;
import com.bytezone.utility.Utility;

// see file compressed/BQY/ACOS.TECH.BQY/BINARY.II.DOCS
// see FTN $E0 / $0001
// -----------------------------------------------------------------------------------//
public class FileBinary2 extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private static String[] bin2Formats =
      { "Prodos or SOS", "Dos 3.3", "Pascal", "CPM", "MS Dos" };
  private static String[] flags =
      { "compressed", "encrypted", "", "", "", "", "", "sparse (packed)" };

  private int headerBlockNo;
  private AppleBlock headerBlock;

  private int accessCode;
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

  private String squeezeName = "";

  private boolean debug = false;
  private boolean validBlocks = true;

  int fileType;
  String fileName;
  String fileTypeText;

  // ---------------------------------------------------------------------------------//
  FileBinary2 (FsBinary2 fs, int headerBlockNo)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    this.headerBlockNo = headerBlockNo;

    headerBlock = fs.getBlock (headerBlockNo, BlockType.FS_DATA);
    headerBlock.setBlockSubType ("BIN2 HDR");
    headerBlock.setFileOwner (this);

    byte[] buffer = headerBlock.getBuffer ();

    accessCode = buffer[3] & 0xFF;
    fileType = buffer[4] & 0xFF;                        // local variable
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
    fileName = Utility.getMaskedPascalString (buffer, 23);        // local variable
    nativeName = Utility.getMaskedPascalString (buffer, 39);

    gAuxType = Utility.unsignedShort (buffer, 109);
    gAccess = buffer[111] & 0xFF;
    gFileType = buffer[112] & 0xFF;
    gStorage = buffer[113] & 0xFF;
    gFileSize = Utility.unsignedShort (buffer, 114);
    gEof = buffer[116] & 0xFF;
    diskSpace = Utility.unsignedInt (buffer, 117);        // total for all files

    osType = buffer[121] & 0xFF;
    nativeFileType = Utility.unsignedShort (buffer, 122);
    phantomFile = buffer[124] & 0xFF;                     // ignore file if != 0
    dataFlags = buffer[125] & 0xFF;
    version = buffer[126] & 0xFF;
    filesFollowing = buffer[127] & 0xFF;

    fileTypeText = switch (osType)                      // local variable
    {
      case 0 -> FsProdos.getFileTypeText (fileType);
      case 1 -> "Dos type " + nativeFileType;
      case 2 -> "Pascal type " + nativeFileType;
      case 3 -> "CPM type " + nativeFileType;
      case 4 -> "MS-DOS type " + nativeFileType;
      default -> throw new IllegalArgumentException ("Unexpected value: " + osType);
    };

    int firstBlock = headerBlockNo + 1;
    int lastBlock = firstBlock + (eof - 1) / 128;

    for (int block = firstBlock; block <= lastBlock; block++)
    {
      if (!fs.isValidAddress (block))
      {
        if (debug)
          System.out.printf ("Invalid block %d in %s%n", block, getFileName ());
        validBlocks = false;
        break;
      }

      AppleBlock dataBlock = fs.getBlock (block, BlockType.FILE_DATA);
      dataBlocks.add (dataBlock);
      dataBlock.setFileOwner (this);
    }

    if (validBlocks && (isCompressed () || fileName.endsWith (".QQ")))
    {
      buffer = dataBlocks.get (0).getBuffer ();
      if (buffer[0] == 0x76 && buffer[1] == (byte) 0xFF)      // squeeze
      {
        String name = Utility.getCString (buffer, 4);
        if (!name.isBlank ())
          squeezeName = name;
      }
    }

    if (debug)
      System.out.println (toString ());
  }

  // ---------------------------------------------------------------------------------//
  boolean isValid ()
  // ---------------------------------------------------------------------------------//
  {
    return validBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return !squeezeName.isEmpty () ? squeezeName : fileName;
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
  public int getFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    return eof;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return fileTypeText;
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
  public int getAuxType ()
  // ---------------------------------------------------------------------------------//
  {
    return auxType;
  }

  // ---------------------------------------------------------------------------------//
  public int getOsType ()
  // ---------------------------------------------------------------------------------//
  {
    return osType;
  }

  // ---------------------------------------------------------------------------------//
  public int getEof ()
  // ---------------------------------------------------------------------------------//
  {
    return eof;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getAllBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    List<AppleBlock> blocks = new ArrayList<AppleBlock> (dataBlocks);
    blocks.add (headerBlock);

    return blocks;
  }

  // ---------------------------------------------------------------------------------//
  boolean isPhantomFile ()
  // ---------------------------------------------------------------------------------//
  {
    return phantomFile != 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return blocks;        // this will be different from dataBlocks.size()
  }

  // ---------------------------------------------------------------------------------//
  int getFilesFollowing ()
  // ---------------------------------------------------------------------------------//
  {
    return filesFollowing;
  }

  // ---------------------------------------------------------------------------------//
  boolean isCompressed ()
  // ---------------------------------------------------------------------------------//
  {
    return (dataFlags & 0x80) != 0;
  }

  // ---------------------------------------------------------------------------------//
  boolean isEncrypted ()
  // ---------------------------------------------------------------------------------//
  {
    return (dataFlags & 0x40) != 0;
  }

  // ---------------------------------------------------------------------------------//
  boolean isSparse ()
  // ---------------------------------------------------------------------------------//
  {
    return (dataFlags & 0x01) != 0;
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
        text.append (flags[i] + ", ");
      mask >>>= 1;
    }

    if (text.length () > 1)
    {
      text.deleteCharAt (text.length () - 1);     // space
      text.deleteCharAt (text.length () - 1);     // comma
    }

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Buffer getFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    if (exactFileBuffer != null)
      return exactFileBuffer;

    getRawFileBuffer ();
    byte[] data = rawFileBuffer.data ();

    String suffix = Utility.getSuffix (getParentFileSystem ().getFileName ());

    if (suffix.equals ("bqy") && !squeezeName.isEmpty ())
    {
      Squeeze squeeze = new Squeeze ();
      byte[] unsqueeze = squeeze.unSqueeze (data);

      exactFileBuffer = new Buffer (unsqueeze, 0, unsqueeze.length);
      return exactFileBuffer;
    }

    exactFileBuffer = rawFileBuffer;

    return exactFileBuffer;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-15s %-3s  %04X  %s  %,7d", fileName, fileTypeText, auxType,
        created.isPresent () ? created.get () : "", eof);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("\n--- Binary II Header --\n");
    formatText (text, "Header block", 2, headerBlockNo);
    formatText (text, "Access code", 2, accessCode);
    formatText (text, "File type", 2, fileType, fileTypeText);

    formatText (text, "Aux type", 4, auxType);
    formatText (text, "Storage type", 2, storageType,
        ProdosConstants.storageTypes[storageType]);
    formatText (text, "File size x 512", 4, blocks);

    formatText (text, "Mod date", 4, modDate,
        modified.isPresent () ? modified.get ().toString () : "");
    formatText (text, "Mod time", 4, modTime);
    formatText (text, "Create date", 4, createDate,
        created.isPresent () ? created.get ().toString () : "");
    formatText (text, "Create time", 4, createTime);

    formatText (text, "EOF", 6, eof);
    formatText (text, "File name", fileName);
    formatText (text, "Squeeze name", squeezeName);
    formatText (text, "Native name", nativeName);

    formatText (text, "G Aux type", 4, gAuxType);
    formatText (text, "G Access", 2, gAccess);
    formatText (text, "G File type", 2, gFileType);
    formatText (text, "G Storage", 2, gStorage);
    formatText (text, "G File size", 4, gEof);
    formatText (text, "G EOF", 2, gFileSize);

    formatText (text, "Disk space", 4, diskSpace, "(all files)");

    formatText (text, "OS type", 2, osType,
        (osType >= 0 && osType < bin2Formats.length) ? bin2Formats[osType] : "");
    formatText (text, "Native file type", 4, nativeFileType);
    formatText (text, "Phantom file", 2, phantomFile);

    formatText (text, "Data flags", 2, dataFlags, getFlagsText (dataFlags));

    String message =
        !squeezeName.isEmpty () && !isCompressed () ? "  <-- should be true" : "";
    formatText (text, "  compressed?", isCompressed (), message);

    formatText (text, "  encrypted?", isEncrypted ());
    formatText (text, "  sparse?", isSparse ());
    formatText (text, "Bin2 version", 2, version);
    formatText (text, "Files following", 2, filesFollowing);

    return Utility.rtrim (text);
  }
}
