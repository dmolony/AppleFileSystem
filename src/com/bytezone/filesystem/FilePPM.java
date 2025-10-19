package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

//https://ciderpress2.com/formatdoc/Pascal-notes.html
// -----------------------------------------------------------------------------------//
public class FilePPM extends FileProdos
// -----------------------------------------------------------------------------------//
{
  byte[] header;
  int ppmSize;
  int totalVolumes;
  String signature;

  String[] descriptions = new String[32];
  String[] volumeNames = new String[32];
  VolumeInfo[] volumeInfo = new VolumeInfo[32];
  Buffer[] buffers = new Buffer[32];

  // ---------------------------------------------------------------------------------//
  FilePPM (FsProdos parentFs, AppleContainer parentContainer,
      AppleBlock parentCatalogBlock, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (parentFs, parentContainer, parentCatalogBlock, slot);

    List<AppleBlock> headerBlocks = new ArrayList<> (2);

    int keyPtr = dataFork.keyPtr;
    for (int i = 0; i < 2; i++)
      headerBlocks.add (parentFs.getBlock (keyPtr++));

    header = parentFs.readBlocks (headerBlocks);

    ppmSize = Utility.unsignedShort (header, 0);
    totalVolumes = Utility.unsignedShort (header, 2);
    signature = Utility.getPascalString (header, 4);

    int volInfoPtr = 0x08;
    int descriptionPtr = 0x0110;
    int volNamePtr = 0x0308;

    for (int i = 1; i < 31; i++)
    {
      int firstBlock = Utility.unsignedShort (header, volInfoPtr);
      int volumeLength = Utility.unsignedShort (header, volInfoPtr + 2);
      int defaultUnit = header[volInfoPtr + 4] & 0xFF;
      boolean writeProtected = (header[volInfoPtr + 5] & 0x80) != 0;
      int oldDriverAddress = Utility.unsignedShort (header, volInfoPtr + 6);

      if (firstBlock > 0)
      {
        volumeInfo[i] = new VolumeInfo (firstBlock, volumeLength, defaultUnit,
            writeProtected, oldDriverAddress);
        buffers[i] = new Buffer (getRawFileBuffer ().data (),
            (firstBlock - dataFork.keyPtr) * 512, volumeLength);
      }

      descriptions[i] = Utility.getPascalString (header, descriptionPtr);
      volumeNames[i] = Utility.getPascalString (header, volNamePtr);

      volInfoPtr += 8;
      descriptionPtr += 16;
      volNamePtr += 8;
    }
  }

  // ---------------------------------------------------------------------------------//
  int getTotalVolumes ()
  // ---------------------------------------------------------------------------------//
  {
    return totalVolumes;
  }

  // ---------------------------------------------------------------------------------//
  Buffer getVolumeBuffer (int volumeNumber)
  // ---------------------------------------------------------------------------------//
  {
    return buffers[volumeNumber];
  }

  private record VolumeInfo (int firstBlock, int volumeLength, int defaultUnit,
      boolean writeProtected, int oldDriverAddress)
  {
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("\n\n---- PPM Partition ----\n");

    formatText (text, "PPM size", 4, ppmSize);
    formatText (text, "Volumes", 4, totalVolumes);
    formatText (text, "Signature", signature);

    for (int i = 1; i < 31; i++)
      if (volumeNames[i].length () > 0)
      {
        text.append ("\n");
        formatText (text, "Volume name", volumeNames[i]);
        formatText (text, "Description", descriptions[i]);
        formatText (text, "First block", 4, volumeInfo[i].firstBlock);
        formatText (text, "Length", 4, volumeInfo[i].volumeLength);
        formatText (text, "Default unit", 2, volumeInfo[i].defaultUnit);
        formatText (text, "Write protected", volumeInfo[i].writeProtected);
        formatText (text, "Old driver address", 4, volumeInfo[i].oldDriverAddress);
      }

    return Utility.rtrim (text);
  }
}