package com.bytezone.filesystem;

import java.time.LocalDateTime;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileDos4 extends FileDos
// -----------------------------------------------------------------------------------//
{
  boolean zero;
  LocalDateTime modified;

  // ---------------------------------------------------------------------------------//
  FileDos4 (FsDos4 fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    int nextTrack = buffer[ptr] & 0xFF;
    int nextSector = buffer[ptr + 1] & 0xFF;

    boolean deleted = (buffer[ptr] & 0x80) != 0;
    zero = (buffer[ptr] & 0x40) != 0;

    isLocked = (buffer[ptr + 2] & 0x80) != 0;
    fileType = buffer[ptr + 2] & 0x7F;

    fileTypeText = fs.getFileTypeText (fileType);
    String blockSubType = fs.getBlockSubTypeText (fileType);

    fileName = Utility.string (buffer, ptr + 3, 24).trim ();
    modified = Utility.getDos4LocalDateTime (buffer, 27);
    sectorCount = Utility.unsignedShort (buffer, ptr + 33);
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

      byte[] sectorBuffer = tsSector.read ();
      int offset = Utility.unsignedShort (sectorBuffer, 5);

      for (int i = 12; i < 256; i += 2)
      {
        int fileTrack = sectorBuffer[i] & 0xFF;
        int fileSector = sectorBuffer[i + 1] & 0xFF;
        boolean zero = (fileTrack & 0x40) != 0;
        fileTrack &= 0x3F;

        if (fileTrack == 0 && !zero && fileSector == 0)
        {
          if (fileType != 0x00)                   // not a text file
            break loop;

          dataBlocks.add (null);                  // must be a sparse file
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

    String addressText = getAddress () == 0 ? "" : String.format ("$%4X", getAddress ());

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

    return String.format ("%1s  %1s  %03d  %-24.24s  %-5s  %-13s %3d %3d   %s",
        lockedFlag, getFileTypeText (), getSectorCount () % 1000, getFileName (),
        addressText, lengthText, getTotalIndexSectors (), getTotalDataSectors (),
        message.trim ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("%-30s  %3d  %3d", fileName, fileType, sectorCount));

    return Utility.rtrim (text);
  }
}
