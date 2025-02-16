package com.bytezone.filesystem;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileDos3 extends FileDos
// -----------------------------------------------------------------------------------//
{
  CatalogEntryDos3 catalogEntry;

  // ---------------------------------------------------------------------------------//
  FileDos3 (FsDos3 fs, AppleBlock catalogBlock, int ptr, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    byte[] buffer = catalogBlock.getBuffer ();

    int nextTrack = buffer[ptr] & 0xFF;
    int nextSector = buffer[ptr + 1] & 0xFF;

    catalogEntry = new CatalogEntryDos3 (catalogBlock, slot);

    isNameValid = catalogEntry.isNameValid;
    sectorCount = catalogEntry.sectorCount;

    String blockSubType = fs.getBlockSubTypeText (getFileType ());

    // build lists of index and data sectors
    int sectorsLeft = sectorCount;
    loop: while (nextTrack != 0)
    {
      AppleBlock tsSector = fs.getSector (nextTrack, nextSector, BlockType.FS_DATA);
      if (tsSector == null)
        break;

      tsSector.setBlockSubType ("TSLIST");
      tsSector.setFileOwner (this);

      indexBlocks.add (tsSector);

      if (--sectorsLeft <= 0)
        break;

      byte[] sectorBuffer = tsSector.getBuffer ();

      int sectorOffset = Utility.unsignedShort (sectorBuffer, 5);   // 0/122/244/366 etc

      for (int i = 12; i < 256; i += 2)
      {
        int track = sectorBuffer[i] & 0xFF;
        int sector = sectorBuffer[i + 1] & 0xFF;

        if (track == 0 && sector == 0)            // invalid address
        {
          if (getFileType () != 0x00)             // not a text file
            break loop;

          dataBlocks.add (null);                  // must be a sparse file
          ++textFileGaps;
        }
        else
        {
          AppleBlock dataSector = fs.getSector (track, sector, BlockType.FILE_DATA);
          if (dataSector == null)
            throw new FileFormatException (String.format (
                "%s - Invalid data sector - %d, %d", getFileName (), track, sector));

          dataSector.setFileOwner (this);
          dataSector.setBlockSubType (blockSubType);

          dataBlocks.add (dataSector);
          if (--sectorsLeft <= 0)
            break loop;
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
        getFileLength () == 0 ? "" : String.format ("$%4X  %<,6d", getFileLength ());

    String message = "";
    String lockedFlag = (isLocked ()) ? "*" : " ";

    if (getSectorCount () != actualSize)
      message = "** Bad size **";

    if (getSectorCount () > 999)
      message += " - Reported " + getSectorCount ();

    if (textFileGaps > 0)
      message += String.format ("gaps %,d", textFileGaps);

    return String.format ("%1s  %1s  %03d  %-30.30s  %-5s  %-13s %3d %3d   %s",
        lockedFlag, getFileTypeText (), getSectorCount () % 1000, getFileName (),
        addressText, lengthText, getTotalIndexSectors (), getTotalDataSectors (),
        message.trim ());
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
}
