package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileDos extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private List<AppleBlock> indexBlocks = new ArrayList<> ();

  private int sectorCount;
  private int length;
  private int address;
  private int textFileGaps;
  private boolean validName;

  // ---------------------------------------------------------------------------------//
  FileDos (FsDos fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    int nextTrack = buffer[ptr] & 0xFF;
    int nextSector = buffer[ptr + 1] & 0xFF;

    fileType = buffer[ptr + 2] & 0x7F;
    isLocked = (buffer[ptr + 2] & 0x80) != 0;
    fileName = Utility.string (buffer, ptr + 3, 30);
    validName = checkName (buffer, ptr + 3, 30);          // check for invalid characters
    sectorCount = Utility.unsignedShort (buffer, ptr + 33);

    fileTypeText = switch (fileType)
    {
      case 0x00 -> "T";
      case 0x01 -> "I";
      case 0x02 -> "A";
      case 0x04 -> "B";
      case 0x08 -> "S";
      case 0x10 -> "R";
      case 0x20 -> "B";
      case 0x40 -> "B";
      default -> "B";                   // should never happen
    };

    int sectorsLeft = sectorCount;
    loop: while (nextTrack != 0)
    {
      AppleBlock tsSector = fs.getSector (nextTrack, nextSector, BlockType.FS_DATA);
      if (tsSector == null)
        throw new FileFormatException ("Invalid TS sector");

      tsSector.setBlockSubType ("TSLIST");
      tsSector.setFileOwner (this);

      indexBlocks.add (tsSector);

      if (--sectorsLeft <= 0)
        break;

      byte[] sectorBuffer = tsSector.read ();

      for (int i = 12; i < 256; i += 2)
      {
        int track = sectorBuffer[i] & 0xFF;
        int sector = sectorBuffer[i + 1] & 0xFF;

        if (track > 0 && sector > 0)
        {
          AppleBlock dataSector = fs.getSector (track, sector, BlockType.FILE_DATA);
          if (dataSector == null)
            throw new FileFormatException ("Invalid data sector - " + dataSector);

          dataSector.setFileOwner (this);

          dataSector.setBlockSubType (switch (fileType)
          {
            case 0x00 -> "TEXT";
            case 0x01 -> "INT BASIC";
            case 0x02 -> "APPLESOFT";
            case 0x04 -> "BINARY";
            case 0x08 -> "S";
            case 0x10 -> "R";
            case 0x20 -> "B";
            case 0x40 -> "B";
            default -> "B";                       // should never happen
          });

          dataBlocks.add (dataSector);
          if (--sectorsLeft <= 0)
            break loop;
        }
        else if (fileType == 0x00)                // text file
        {
          dataBlocks.add (null);                  // must be a sparse file
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
    else if (fileType == 1 || fileType == 2)      // integer basic or applesoft
    {
      if (dataBlocks.size () > 0)
      {
        byte[] fileBuffer = fs.readBlock (dataBlocks.get (0));
        length = Utility.unsignedShort (fileBuffer, 0);
        // could calculate the address from the line numbers
      }
    }
    else
      length = dataBlocks.size () * getParentFileSystem ().getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public byte[] read ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return parentFileSystem.readBlocks (dataBlocks);
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    List<AppleBlock> blocks = new ArrayList<AppleBlock> (dataBlocks);
    blocks.addAll (indexBlocks);

    return blocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                       // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    return length;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                      // in blocks
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
      message += "Actual size (" + actualSize + ") ";

    if (getSectorCount () > 999)
      message += "Reported " + getSectorCount ();

    String text =
        String.format ("%1s  %1s  %03d  %-30.30s  %-5s  %-13s %3d %3d   %s", lockedFlag,
            getFileTypeText (), getSectorCount () % 1000, getFileName (), addressText,
            lengthText, getTotalIndexSectors (), getTotalDataSectors (), message.trim ());

    return text;
  }

  // This is mainly used to keep the stupid Beagle Bros files out of the file tree
  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isActualFile ()
  // ---------------------------------------------------------------------------------//
  {
    return validName && dataBlocks.size () > 0;
  }

  // ---------------------------------------------------------------------------------//
  private boolean checkName (byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    while (length-- > 0)
      if (buffer[offset++] == (byte) 0x88)      // backspace
        return false;

    return true;
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
