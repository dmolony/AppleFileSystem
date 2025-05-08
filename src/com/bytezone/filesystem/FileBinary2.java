package com.bytezone.filesystem;

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
  public List<AppleBlock> getDataBlocks ()
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
    if (adjustedFileBuffer != null)
      return adjustedFileBuffer;

    getRawFileBuffer ();
    byte[] data = rawFileBuffer.data ();

    String suffix = Utility.getSuffix (getParentFileSystem ().getFileName ());

    if (suffix.equals ("bqy") && !squeezeName.isEmpty ())
    {
      Squeeze squeeze = new Squeeze ();
      byte[] unsqueeze = squeeze.unSqueeze (data);

      adjustedFileBuffer = new Buffer (unsqueeze, 0, unsqueeze.length);
      return adjustedFileBuffer;
    }

    adjustedFileBuffer = rawFileBuffer;

    return adjustedFileBuffer;
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
    Utility.formatMeta (text, "Header block", 2, headerBlockNo);
    Utility.formatMeta (text, "Access code", 2, accessCode);
    Utility.formatMeta (text, "File type", 2, fileType, fileTypeText);

    Utility.formatMeta (text, "Aux type", 4, auxType);
    Utility.formatMeta (text, "Storage type", 2, storageType,
        ProdosConstants.storageTypes[storageType]);
    Utility.formatMeta (text, "File size x 512", 4, blocks);

    Utility.formatMeta (text, "Mod date", 4, modDate,
        modified.isPresent () ? modified.get ().toString () : "");
    Utility.formatMeta (text, "Mod time", 4, modTime);
    Utility.formatMeta (text, "Create date", 4, createDate,
        created.isPresent () ? created.get ().toString () : "");
    Utility.formatMeta (text, "Create time", 4, createTime);

    Utility.formatMeta (text, "EOF", 6, eof);
    Utility.formatMeta (text, "File name", fileName);
    Utility.formatMeta (text, "Squeeze name", squeezeName);
    Utility.formatMeta (text, "Native name", nativeName);

    Utility.formatMeta (text, "G Aux type", 4, gAuxType);
    Utility.formatMeta (text, "G Access", 2, gAccess);
    Utility.formatMeta (text, "G File type", 2, gFileType);
    Utility.formatMeta (text, "G Storage", 2, gStorage);
    Utility.formatMeta (text, "G File size", 4, gEof);
    Utility.formatMeta (text, "G EOF", 2, gFileSize);

    Utility.formatMeta (text, "Disk space", 4, diskSpace, "(all files)");

    Utility.formatMeta (text, "OS type", 2, osType,
        (osType >= 0 && osType < bin2Formats.length) ? bin2Formats[osType] : "");
    Utility.formatMeta (text, "Native file type", 4, nativeFileType);
    Utility.formatMeta (text, "Phantom file", 2, phantomFile);

    Utility.formatMeta (text, "Data flags", 2, dataFlags, getFlagsText (dataFlags));

    String message =
        !squeezeName.isEmpty () && !isCompressed () ? "  <-- should be true" : "";
    Utility.formatMeta (text, "  compressed?", isCompressed (), message);

    Utility.formatMeta (text, "  encrypted?", isEncrypted ());
    Utility.formatMeta (text, "  sparse?", isSparse ());
    Utility.formatMeta (text, "Bin2 version", 2, version);
    Utility.formatMeta (text, "Files following", 2, filesFollowing);

    return Utility.rtrim (text);
  }
}
