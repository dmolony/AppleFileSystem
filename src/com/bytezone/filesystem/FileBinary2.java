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
    fileName = Utility.getMaskedPascalString (buffer, 23);
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

    fileTypeText = switch (osType)
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
  //  @Override
  //  public int getFileType ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return fileType;
  //  }

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
  public List<AppleBlock> getBlocks ()
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
  //  @Override
  //  public byte[] read ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    String suffix = Utility.getSuffix (getParentFileSystem ().getFileName ());
  //    if (suffix.equals ("bqy") && squeezeName != null)
  //    {
  //      Squeeze squeeze = new Squeeze ();
  //      byte[] buffer = getParentFileSystem ().readBlocks (dataBlocks);
  //
  //      return squeeze.unSqueeze (buffer);
  //    }
  //
  //    return parentFileSystem.readBlocks (dataBlocks);
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Buffer getFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    if (dataRecord != null)
      return dataRecord;

    String suffix = Utility.getSuffix (getParentFileSystem ().getFileName ());
    if (suffix.equals ("bqy") && squeezeName != null)
    {
      Squeeze squeeze = new Squeeze ();
      byte[] buffer = getParentFileSystem ().readBlocks (dataBlocks);

      byte[] unsqueeze = squeeze.unSqueeze (buffer);
      dataRecord = new Buffer (unsqueeze, 0, unsqueeze.length);
      return dataRecord;
    }

    byte[] buffer = parentFileSystem.readBlocks (dataBlocks);
    dataRecord = new Buffer (buffer, 0, buffer.length);
    return dataRecord;
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

    text.append ("--- Binary II Header --\n");
    text.append (String.format ("Header block .......... %02X%n", headerBlockNo));
    text.append (String.format ("Access code ........... %02X%n", accessCode));
    text.append (String.format ("File type ............. %02X        %s%n", fileType,
        fileTypeText));
    text.append (String.format ("Aux type .............. %04X%n", auxType));
    text.append (String.format ("Storage type .......... %02X  %s%n", storageType,
        ProdosConstants.storageTypes[storageType]));
    text.append (String.format ("File size x 512 ....... %02X      %<,7d%n", blocks));
    text.append (String.format ("Mod date .............. %04X    %s%n", modDate,
        modified.isPresent () ? modified.get () : ""));
    text.append (String.format ("Mod time .............. %04X%n", modTime));
    text.append (String.format ("Create date ........... %04X    %s%n", createDate,
        created.isPresent () ? created.get () : ""));
    text.append (String.format ("Create time ........... %04X%n", createTime));
    text.append (String.format ("EOF ................... %06X  %<,7d%n", eof));
    text.append (String.format ("File name ............. %s%n", fileName));
    text.append (String.format ("Squeeze name .......... %s%n", squeezeName));
    text.append (String.format ("Native name ........... %s%n", nativeName));

    text.append (String.format ("G Aux type ............ %04X  %<d%n", gAuxType));
    text.append (String.format ("G Access .............. %02X    %<d%n", gAccess));
    text.append (String.format ("G File type ........... %02X    %<d%n", gFileType));
    text.append (String.format ("G Storage ............. %02X    %<d%n", gStorage));
    text.append (String.format ("G File size ........... %04X  %<d%n", gFileSize));
    text.append (String.format ("G EOF ................. %02X    %<d%n", gEof));
    text.append (
        String.format ("Disk space ............ %,d   (all files)%n", diskSpace));

    text.append (String.format ("OS type ............... %02X  %s%n", osType,
        (osType >= 0 && osType < bin2Formats.length) ? bin2Formats[osType] : ""));
    text.append (String.format ("Native file type ...... %04X%n", nativeFileType));
    text.append (String.format ("Phantom file .......... %02X%n", phantomFile));

    String message =
        !squeezeName.isEmpty () && !isCompressed () ? "  <-- should be true" : "";
    text.append (String.format ("Data flags ............ %02X %s%n", dataFlags,
        getFlagsText (dataFlags)));
    text.append (
        String.format ("  compressed? ......... %s%s%n", isCompressed (), message));
    text.append (String.format ("  encrypted? .......... %s%n", isEncrypted ()));
    text.append (String.format ("  sparse? ............. %s%n", isSparse ()));
    text.append (String.format ("Bin2 version .......... %02X%n", version));
    text.append (String.format ("Files following ....... %02X%n", filesFollowing));

    return Utility.rtrim (text);
  }
}
