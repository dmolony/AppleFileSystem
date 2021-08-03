package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.FsDos.FileType;

// -----------------------------------------------------------------------------------//
public class FileDos extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  int type;
  int sectorCount;
  boolean locked;
  FileType fileType;
  String fileTypeLetter;

  List<AppleBlock> indexBlocks = new ArrayList<> ();
  List<AppleBlock> dataBlocks = new ArrayList<> ();

  int eof;

  // ---------------------------------------------------------------------------------//
  FileDos (FsDos fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    int nextTrack = buffer[ptr] & 0xFF;
    int nextSector = buffer[ptr + 1] & 0xFF;

    type = buffer[ptr + 2] & 0x7F;
    locked = (buffer[ptr + 2] & 0x80) != 0;
    name = Utility.string (buffer, ptr + 3, 30).trim ();
    sectorCount = Utility.unsignedShort (buffer, ptr + 33);

    fileType = switch (type)
    {
      case 0x00 -> FileType.Text;
      case 0x01 -> FileType.IntegerBasic;
      case 0x02 -> FileType.ApplesoftBasic;
      case 0x04 -> FileType.Binary;
      case 0x08 -> FileType.SS;
      case 0x10 -> FileType.Relocatable;
      case 0x20 -> FileType.AA;
      case 0x40 -> FileType.BB;
      default -> FileType.Binary;        // should never happen
    };

    fileTypeLetter = switch (type)
    {
      case 0x00 -> "T";
      case 0x01 -> "I";
      case 0x02 -> "A";
      case 0x04 -> "B";
      case 0x08 -> "S";
      case 0x10 -> "R";
      case 0x20 -> "X";
      case 0x40 -> "Y";
      default -> "B";        // should never happen
    };

    int sectorsLeft = sectorCount;
    while (nextTrack != 0)
    {
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
  public int getSize ()                   // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return indexBlocks.size () + dataBlocks.size ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%s %s %03d %-30s", locked ? "*" : " ", fileTypeLetter,
        sectorCount, name);
  }
}
