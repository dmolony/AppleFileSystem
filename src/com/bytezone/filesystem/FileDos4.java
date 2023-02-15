package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileDos4 extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  int type;
  int sectorCount;

  List<AppleBlock> indexBlocks = new ArrayList<> ();
  List<AppleBlock> dataBlocks = new ArrayList<> ();

  int eof;

  // ---------------------------------------------------------------------------------//
  FileDos4 (FsDos4 fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    int nextTrack = buffer[ptr] & 0xFF;
    int nextSector = buffer[ptr + 1] & 0xFF;

    type = buffer[ptr + 2] & 0xFF;
    fileName = Utility.string (buffer, ptr + 3, 24).trim ();
    sectorCount = Utility.unsignedShort (buffer, ptr + 33);
    int sectorsLeft = sectorCount;

    while (nextTrack != 0)
    {
      nextTrack &= 0x3F;
      nextSector &= 0x1F;

      AppleBlock tsSector = fs.getSector (nextTrack, nextSector);
      if (!tsSector.isValid ())
        throw new FileFormatException ("Invalid TS sector");

      indexBlocks.add (tsSector);
      --sectorsLeft;

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

      nextTrack = sectorBuffer[1] & 0xFF;
      nextSector = sectorBuffer[2] & 0xFF;
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystem.readBlocks (dataBlocks);
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
  public int getTotalBlocks ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return indexBlocks.size () + dataBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-30s  %3d  %3d", fileName, type, sectorCount);
  }
}
