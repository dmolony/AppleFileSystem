package com.bytezone.filesystem;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileDos4 extends FileDos
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  FileDos4 (FsDos4 fs, AppleBlock catalogBlock, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    byte[] buffer = catalogBlock.getBuffer ();

    int slot = (ptr - HEADER_SIZE) / ENTRY_SIZE;
    catalogEntry = new CatalogEntryDos4 (catalogBlock, slot);
    String blockSubType = fs.getBlockSubTypeText (getFileType ());

    int nextTrack = buffer[ptr] & 0xFF;
    int nextSector = buffer[ptr + 1] & 0xFF;

    // build lists of index and data sectors - NB cannot rely on catalogEntry.sectorCount
    outer_loop: while (nextTrack != 0)                // 0x40 = track zero
    {
      nextTrack &= 0x3F;            // 0x80 = deleted(?), 0x40 = track zero
      nextSector &= 0x1F;           // 0 : 31

      AppleBlock tsSector = fs.getSector (nextTrack, nextSector, BlockType.FS_DATA);
      if (tsSector == null)
        throw new FileFormatException ("Invalid TS sector");

      tsSector.setBlockSubType ("TSLIST");
      tsSector.setFileOwner (this);

      indexBlocks.add (tsSector);

      byte[] sectorBuffer = tsSector.getBuffer ();
      int sectorOffset = Utility.unsignedShort (sectorBuffer, 5);   // 0/122/244/366 etc

      for (int i = 12; i < sectorBuffer.length; i += 2)
      {
        int track = sectorBuffer[i] & 0xFF;
        int sector = sectorBuffer[i + 1] & 0xFF;

        boolean zero = (track & 0x40) != 0;
        track &= 0x3F;                                      // remove flags

        if (track == 0 && !zero && sector == 0)             // invalid address
        {
          if (getFileType () == FsDos.FILE_TYPE_TEXT)
          {
            dataBlocks.add (null);                          // must be a sparse file
            ++textFileGaps;
          }
          else
            break outer_loop;
        }
        else
        {
          AppleBlock dataSector = fs.getSector (track, sector, BlockType.FILE_DATA);
          if (dataSector == null)
            throw new FileFormatException (
                String.format ("Invalid data sector : %02X %02X%n", track, sector));

          dataSector.setBlockSubType (blockSubType);
          dataSector.setFileOwner (this);

          dataBlocks.add (dataSector);
        }
      }

      nextTrack = sectorBuffer[1] & 0xFF;
      nextSector = sectorBuffer[2] & 0xFF;
    }

    setFileLength ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    int actualSize = getTotalIndexSectors () + getTotalDataSectors ();
    int loadAddress = getLoadAddress ();

    String addressText = loadAddress == 0 || loadAddress == 0x801 ? ""
        : String.format ("$%4X", loadAddress);

    String lengthText =
        getFileLength () == 0 ? "" : String.format ("$%5X %<,7d", getFileLength ());

    String message = "";
    String lockedFlag = (isLocked ()) ? "*" : " ";

    if (getSectorCount () != actualSize && getTotalDataSectors () > 0)
      message = "** Bad size **";

    if (getSectorCount () > 999)
      message += "Reported " + getSectorCount ();

    return String.format ("%1s  %1s  %03d  %-24.24s  %-15.15s  %-5s  %-14s %3d %3d   %s",
        lockedFlag, getFileTypeText (), getSectorCount () % 1000, getFileName (),
        ((CatalogEntryDos4) catalogEntry).getModified1 (), addressText, lengthText,
        getTotalIndexSectors (), getTotalDataSectors (), message.trim ());
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
