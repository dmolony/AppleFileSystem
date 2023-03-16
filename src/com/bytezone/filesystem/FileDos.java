package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileDos extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private List<AppleBlock> indexBlocks = new ArrayList<> ();
  private List<AppleBlock> dataBlocks = new ArrayList<> ();

  private int sectorCount;
  private int length;
  private int address;
  private int textFileGaps;

  // ---------------------------------------------------------------------------------//
  FileDos (FsDos fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    isFile = true;

    int nextTrack = buffer[ptr] & 0xFF;
    int nextSector = buffer[ptr + 1] & 0xFF;

    fileType = buffer[ptr + 2] & 0x7F;
    isLocked = (buffer[ptr + 2] & 0x80) != 0;
    fileName = Utility.string (buffer, ptr + 3, 30).trim ();
    sectorCount = Utility.unsignedShort (buffer, ptr + 33);

    fileTypeText = switch (fileType)
    {
      case 0x00 -> "T";
      case 0x01 -> "I";
      case 0x02 -> "A";
      case 0x04 -> "B";
      case 0x08 -> "S";
      case 0x10 -> "R";
      case 0x20 -> "X";
      case 0x40 -> "Y";
      default -> "B";                   // should never happen
    };

    int sectorsLeft = sectorCount;
    loop: while (nextTrack != 0)
    {
      AppleBlock tsSector = fs.getSector (nextTrack, nextSector);
      if (!tsSector.isValid ())
        throw new FileFormatException ("Invalid TS sector");

      indexBlocks.add (tsSector);

      if (--sectorsLeft <= 0)
        break;

      byte[] sectorBuffer = tsSector.read ();

      for (int i = 12; i < 256; i += 2)
      {
        AppleBlock dataSector = fs.getSector (sectorBuffer, i);

        if (!dataSector.isValid ())
          throw new FileFormatException ("Invalid data sector - " + dataSector);

        if (dataSector.getBlockNo () > 0)
        {
          dataBlocks.add (dataSector);
          if (--sectorsLeft <= 0)
            break loop;
        }
        else if (fileType == 0x00)            // text file
        {
          dataBlocks.add (null);              // must be a sparse file
          ++textFileGaps;
        }
        else
          throw new FileFormatException ("Unexpected zero index value in TsSector");
      }

      nextTrack = sectorBuffer[1] & 0xFF;
      nextSector = sectorBuffer[2] & 0xFF;
    }

    if (fileType == 4)                            // binary
    {
      if (dataBlocks.size () > 0)
      {
        byte[] fileBuffer = fs.readBlock (dataBlocks.get (0));
        address = Utility.unsignedShort (fileBuffer, 0);
        length = Utility.unsignedShort (fileBuffer, 2);
      }
    }
    else if (fileType == 1 || fileType == 2)       // integer basic or applesoft
    {
      if (dataBlocks.size () > 0)
      {
        byte[] fileBuffer = fs.readBlock (dataBlocks.get (0));
        length = Utility.unsignedShort (fileBuffer, 0);
        // could calculate the address from the line numbers
      }
    }
    else
      length = dataBlocks.size () * getFileSystem ().getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFileSystem.readBlocks (dataBlocks);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                         // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return length;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return indexBlocks.size () + dataBlocks.size () - textFileGaps;
  }

  // ---------------------------------------------------------------------------------//
  public int getSectorCount ()
  // ---------------------------------------------------------------------------------//
  {
    return sectorCount;
  }

  // ---------------------------------------------------------------------------------//
  public int getAddress ()
  // ---------------------------------------------------------------------------------//
  {
    return address;
  }

  // ---------------------------------------------------------------------------------//
  //  public int getTextFileGaps ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return textFileGaps;
  //  }

  // ---------------------------------------------------------------------------------//
  public int getTotalDataSectors ()
  // ---------------------------------------------------------------------------------//
  {
    return dataBlocks.size () - textFileGaps;
  }

  // ---------------------------------------------------------------------------------//
  public int getTotalIndexSectors ()
  // ---------------------------------------------------------------------------------//
  {
    return indexBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("Locked ................ %s%n", isLocked));
    text.append (String.format ("Sectors ............... %04X  %<,5d%n", sectorCount));
    text.append (String.format ("Length ................ %04X  %<,5d%n", length));
    text.append (String.format ("Address ............... %04X  %<,5d%n", address));
    text.append (String.format ("Text file gaps ........ %04X  %<,5d", textFileGaps));

    return Utility.rtrim (text);
  }
}
