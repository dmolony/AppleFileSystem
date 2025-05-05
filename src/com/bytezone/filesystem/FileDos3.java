package com.bytezone.filesystem;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileDos3 extends FileDos
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  FileDos3 (FsDos3 fs, AppleBlock catalogBlock, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    byte[] buffer = catalogBlock.getBuffer ();

    int slot = (ptr - HEADER_SIZE) / ENTRY_SIZE;
    catalogEntry = new CatalogEntryDos3 (catalogBlock, slot);
    String blockSubType = fs.getBlockSubTypeText (getFileType ());

    int nextTrack = buffer[ptr] & 0xFF;
    int nextSector = buffer[ptr + 1] & 0xFF;

    // build lists of index and data sectors
    int sectorsLeft = catalogEntry.sectorCount;
    outer_loop: while (nextTrack != 0)
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

      for (int i = 12; i < sectorBuffer.length; i += 2)
      {
        int track = sectorBuffer[i] & 0xFF;
        int sector = sectorBuffer[i + 1] & 0xFF;

        if (track == 0 && sector == 0)                  // invalid address
        {
          if (getFileType () == FsDos.FILE_TYPE_TEXT)
          {
            dataBlocks.add (null);                      // must be a sparse file
            ++textFileGaps;
          }
          else
            break outer_loop;
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
            break outer_loop;
        }
      }

      nextTrack = sectorBuffer[1] & 0xFF;
      nextSector = sectorBuffer[2] & 0xFF;
    }

    //    if (dataBlocks.size () - textFileGaps > 0)
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

    if (recordLength > 0)
      message = String.format ("Reclen = %,d ?", recordLength);

    if (getSectorCount () != actualSize)
      message = "** Bad size **";

    if (getSectorCount () > 999)
      message += " - Reported " + getSectorCount ();

    return String.format ("%1s  %1s  %03d  %-30.30s  %-5s  %-14s %3d %3d  %s", lockedFlag,
        getFileTypeText (), getSectorCount () % 1000, getFileName (), addressText,
        lengthText, getTotalIndexSectors (), getTotalDataSectors (), message.trim ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return ((FsDos) parentFileSystem).getFileTypeText (catalogEntry.fileType);
  }
}
