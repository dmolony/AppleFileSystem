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

    // build lists of index and data sectors - NB cannot rely on catalogEntry.sectorCount
    outer_loop: while (nextTrack != 0)
    {
      AppleBlock tsSector = fs.getSector (nextTrack, nextSector);

      if (tsSector == null)
        break;
      if (tsSector.getBlockType () == BlockType.EMPTY)
        throw new FileFormatException (blockSubType);

      tsSector.setBlockType (BlockType.FS_DATA);
      tsSector.setBlockSubType ("TSLIST");
      tsSector.setFileOwner (this);

      indexBlocks.add (tsSector);

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
            ++fileGaps;
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
    return String.format ("%1s  %1s  %03d  %-30.30s  %-5s  %-14s %3d %3d  %s",
        isLocked () ? "*" : " ",        //
        getFileTypeText (),             //
        getSectorCount () % 1000,       //
        getFileName (),                 //
        getAddressText (),              //
        getLengthText (),               //
        getTotalIndexSectors (),        //
        getTotalDataSectors (),         //
        getCatalogMessage ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return ((FsDos) parentFileSystem).getFileTypeText (catalogEntry.fileType);
  }
}
