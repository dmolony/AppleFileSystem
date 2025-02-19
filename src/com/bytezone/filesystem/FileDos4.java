package com.bytezone.filesystem;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileDos4 extends FileDos
// -----------------------------------------------------------------------------------//
{
  static final int ENTRY_SIZE = 0x23;
  static final int HEADER_SIZE = 0x0B;

  CatalogEntryDos4 catalogEntry;

  // ---------------------------------------------------------------------------------//
  FileDos4 (FsDos4 fs, AppleBlock catalogSector, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    byte[] buffer = catalogSector.getBuffer ();
    int ptr = HEADER_SIZE + slot * ENTRY_SIZE;

    int nextTrack = buffer[ptr] & 0xFF;
    int nextSector = buffer[ptr + 1] & 0xFF;

    catalogEntry = new CatalogEntryDos4 (catalogSector, slot);

    isNameValid = catalogEntry.isNameValid;
    sectorCount = catalogEntry.sectorCount;

    String blockSubType = fs.getBlockSubTypeText (catalogEntry.fileType);

    // build lists of index and data sectors
    int sectorsLeft = sectorCount;
    loop: while (nextTrack != 0)
    {
      nextTrack &= 0x3F;
      nextSector &= 0x1F;

      AppleBlock tsSector = fs.getSector (nextTrack, nextSector, BlockType.FS_DATA);
      if (tsSector == null)
        throw new FileFormatException ("Invalid TS sector");

      tsSector.setBlockSubType ("TSLIST");
      tsSector.setFileOwner (this);

      indexBlocks.add (tsSector);
      --sectorsLeft;

      byte[] sectorBuffer = tsSector.getBuffer ();
      int sectorOffset = Utility.unsignedShort (sectorBuffer, 5);   // 0/122/244/366 etc

      for (int i = 12; i < 256; i += 2)
      {
        int fileTrack = sectorBuffer[i] & 0xFF;
        int fileSector = sectorBuffer[i + 1] & 0xFF;
        boolean zero = (fileTrack & 0x40) != 0;
        fileTrack &= 0x3F;

        if (fileTrack == 0 && !zero && fileSector == 0)     // invalid address
        {
          if (getFileType () != 0x00)                       // not a text file
            break loop;

          dataBlocks.add (null);                            // must be a sparse file
          ++textFileGaps;
        }
        else
        {
          AppleBlock dataSector =
              fs.getSector (fileTrack, fileSector, BlockType.FILE_DATA);
          if (dataSector == null)
            throw new FileFormatException (String
                .format ("Invalid data sector : %02X %02X%n", fileTrack, fileSector));

          dataSector.setBlockSubType (blockSubType);
          dataSector.setFileOwner (this);

          dataBlocks.add (dataSector);
          --sectorsLeft;

          if (sectorsLeft == 0)
            break;
        }
      }

      nextTrack = sectorBuffer[1] & 0xFF;
      nextSector = sectorBuffer[2] & 0xFF;
    }

    setLength ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    int actualSize = getTotalIndexSectors () + getTotalDataSectors ();

    String addressText =
        getLoadAddress () == 0 ? "" : String.format ("$%4X", getLoadAddress ());

    String lengthText =
        getFileLength () == 0 ? "" : String.format ("$%4X %<,7d", getFileLength ());

    String message = "";
    String lockedFlag = (isLocked ()) ? "*" : " ";

    if (getSectorCount () != actualSize)
      message = "** Bad size **";

    if (getSectorCount () > 999)
      message += "Reported " + getSectorCount ();

    if (textFileGaps > 0)
      message += String.format ("gaps %,d", textFileGaps);

    return String.format ("%1s  %1s  %03d  %-24.24s  %-15.15s  %-5s  %-13s %3d %3d   %s",
        lockedFlag, getFileTypeText (), getSectorCount () % 1000, getFileName (),
        catalogEntry.getModified1 (), addressText, lengthText, getTotalIndexSectors (),
        getTotalDataSectors (), message.trim ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.isLocked;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.fileType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return ((FsDos) parentFileSystem).getFileTypeText (catalogEntry.fileType);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (catalogEntry);

    return Utility.rtrim (text);
  }
}
