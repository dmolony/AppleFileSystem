package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

// -----------------------------------------------------------------------------------//
public class FileDos extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  int track;
  int sector;
  int type;
  int sectorCount;

  List<AppleBlock> indexBlocks = new ArrayList<> ();
  List<AppleBlock> dataBlocks = new ArrayList<> ();

  int eof;

  // ---------------------------------------------------------------------------------//
  FileDos (FsDos fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    track = buffer[ptr] & 0xFF;
    sector = buffer[ptr + 1] & 0xFF;

    type = buffer[ptr + 2] & 0xFF;
    name = AbstractFileSystem.string (buffer, ptr + 3, 30).trim ();
    sectorCount = AbstractFileSystem.unsignedShort (buffer, ptr + 33);
    int sectorsLeft = sectorCount;

    AppleBlock tsSector = fs.getSector (track, sector);
    if (!tsSector.isValid ())
      throw new FileFormatException ("Invalid TS sector");

    while (tsSector.getBlockNo () != 0)
    {
      indexBlocks.add (tsSector);
      --sectorsLeft;
      //      System.out.println (tsSector);
      byte[] sectorBuffer = tsSector.read ();

      for (int i = 12; i < 256; i += 2)
      {
        int fileTrack = sectorBuffer[i] & 0xFF;
        int fileSector = sectorBuffer[i + 1] & 0xFF;

        AppleBlock dataSector = fs.getSector (fileTrack, fileSector);
        if (!dataSector.isValid ())
          throw new FileFormatException ("Invalid data sector");

        if (dataSector.getBlockNo () != 0)
        {
          dataBlocks.add (dataSector);
          --sectorsLeft;

          if (sectorsLeft == 0)
            break;
        }
        else
          dataBlocks.add (null);          // must be a sparse file
      }

      int nextTrack = sectorBuffer[1] & 0xFF;
      int nextSector = sectorBuffer[2] & 0xFF;

      tsSector = fs.getSector (nextTrack, nextSector);

      if (!tsSector.isValid ())
        throw new FileFormatException ("Invalid TS sector: " + tsSector);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getLength ()                 // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size () * fileSystem.getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSize ()                   // in data blocks
  // ---------------------------------------------------------------------------------//
  {
    return indexBlocks.size () + dataBlocks.size ();
  }
}
