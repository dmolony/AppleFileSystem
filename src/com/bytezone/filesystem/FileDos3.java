package com.bytezone.filesystem;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileDos3 extends FileDos
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  FileDos3 (FsDos3 fs, byte[] catalogBuffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    int nextTrack = catalogBuffer[ptr] & 0xFF;
    int nextSector = catalogBuffer[ptr + 1] & 0xFF;

    fileType = catalogBuffer[ptr + 2] & 0x7F;
    isLocked = (catalogBuffer[ptr + 2] & 0x80) != 0;
    fileName = Utility.string (catalogBuffer, ptr + 3, 30).trim ();
    isNameValid = checkName (fileName);                   // check for invalid characters
    sectorCount = Utility.unsignedShort (catalogBuffer, ptr + 33);
    fileTypeText = fs.getFileTypeText (fileType);
    String blockSubType = fs.getBlockSubTypeText (fileType);

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

      for (int i = 12; i < 256; i += 2)
      {
        int track = sectorBuffer[i] & 0xFF;
        int sector = sectorBuffer[i + 1] & 0xFF;

        if (track == 0 && sector == 0)            // invalid address
        {
          if (fileType != 0x00)                   // not a text file
            break loop;

          dataBlocks.add (null);                  // must be a sparse file
          ++textFileGaps;
        }
        else
        {
          AppleBlock dataSector = fs.getSector (track, sector, BlockType.FILE_DATA);
          if (dataSector == null)
            throw new FileFormatException (String
                .format ("%s - Invalid data sector - %d, %d", fileName, track, sector));

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
      message += "Reported " + getSectorCount ();

    if (textFileGaps > 0)
      message += String.format ("gaps %,d", textFileGaps);

    return String.format ("%1s  %1s  %03d  %-30.30s  %-5s  %-13s %3d %3d   %s",
        lockedFlag, getFileTypeText (), getSectorCount () % 1000, getFileName (),
        addressText, lengthText, getTotalIndexSectors (), getTotalDataSectors (),
        message.trim ());
  }
}
