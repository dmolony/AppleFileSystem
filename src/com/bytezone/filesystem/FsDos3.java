package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsDos3 extends FsDos
// -----------------------------------------------------------------------------------//
{
  List<AppleBlock> catalogSectors = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  FsDos3 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.DOS3);

    AppleBlock vtoc = getSector (17, 0, BlockType.FS_DATA);
    if (vtoc == null)
      throw new FileFormatException ("Dos: Invalid VTOC");

    vtoc.setBlockSubType ("VTOC");

    byte[] buffer = vtoc.getBuffer ();
    createVolumeBitMap (buffer);

    if (buffer[3] < 0x01 || buffer[3] > 0x03)
      throw new FileFormatException (
          String.format ("Dos: version byte invalid: %02X", buffer[3]));

    dosVersion = buffer[0x03] & 0xFF;
    volumeNumber = buffer[0x06] & 0xFF;
    maxTSpairs = buffer[0x27] & 0xFF;
    lastTrackAllocated = buffer[0x30] & 0xFF;

    direction = buffer[0x31];
    tracksPerDisk = buffer[0x34] & 0xFF;
    sectorsPerTrack = buffer[0x35] & 0xFF;
    bytesPerSector = Utility.signedShort (buffer, 0x36);

    //    List<AppleBlock> catalogSectors = new ArrayList<> ();   // to check for looping

    while (true)
    {
      int track = buffer[1] & 0xFF;
      int sector = buffer[2] & 0xFF;

      if (track == 0)
        break;

      AppleBlock catalogSector = getSector (track, sector, BlockType.FS_DATA);
      if (catalogSector == null)
        throw new FileFormatException ("Dos: Invalid catalog sector");

      catalogSector.setBlockSubType ("CATALOG");

      if (checkDuplicate (catalogSectors, catalogSector))
        throw new FileFormatException ("Dos: Duplicate catalog sector (looping)");

      catalogSectors.add (catalogSector);

      buffer = catalogSector.getBuffer ();
      int ptr = 11;
      int index = 0;

      while (ptr < buffer.length && buffer[ptr] != 0)
      {
        if (buffer[ptr] == (byte) 0xFF)        // deleted file
        {
          String fileName = Utility.string (buffer, ptr + 3, 29).trim ();
          int sectorCount = Utility.unsignedShort (buffer, ptr + 33);
          int fileType = buffer[ptr + 2] & 0x7F;
          boolean isLocked = (buffer[ptr + 2] & 0x80) != 0;
          deletedFiles.add (String.format ("%s  %s  %03d  %s", isLocked ? "*" : " ",
              getFileTypeText (fileType), sectorCount, fileName));
        }
        else
          try
          {
            //            FileDos3 file = new FileDos3 (this, buffer, ptr, index);
            FileDos3 file = new FileDos3 (this, catalogSector, ptr, index);
            file.catalogEntryBlock = catalogSector;
            file.catalogEntryIndex = index;
            addFile (file);
          }
          catch (FileFormatException e)
          {
            String fileName = Utility.string (buffer, ptr + 3, 30).trim ();
            int sectorCount = Utility.unsignedShort (buffer, ptr + 33);
            int fileType = buffer[ptr + 2] & 0x7F;
            boolean isLocked = (buffer[ptr + 2] & 0x80) != 0;
            failedFiles.add (String.format ("%s  %s  %03d  %s", isLocked ? "*" : " ",
                getFileTypeText (fileType), sectorCount, fileName));
          }

        ptr += ENTRY_SIZE;
        index++;
      }
    }

    setTotalCatalogBlocks (catalogSectors.size ());
    flagDosSectors ();
  }

  // ---------------------------------------------------------------------------------//
  private boolean checkDuplicate (List<AppleBlock> catalogSectors, AppleBlock testSector)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleBlock catalogSector : catalogSectors)
      if (catalogSector.getBlockNo () == testSector.getBlockNo ())
        return true;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Volume : %03d%n%n", volumeNumber));

    String underline = "- --- ---  ------------------------------  -----  -------------"
        + "  -- ---  -------------------\n";

    text.append ("L Typ Len  Name                            Addr"
        + "   Length         TS DAT  Comment\n");
    text.append (underline);

    for (AppleFile file : getFiles ())
    {
      text.append (file.getCatalogLine ());
      text.append ("\n");
    }

    int totalSectors = getTotalBlocks ();
    int freeSectors = getTotalFreeBlocks ();

    text.append (underline);
    text.append (String.format (
        "           Free sectors: %3d    " + "Used sectors: %3d    Total sectors: %3d",
        freeSectors, totalSectors - freeSectors, totalSectors));

    if (deletedFiles.size () > 0)
    {
      text.append ("\n\nDeleted files\n");
      text.append ("-------------\n");
      for (String name : deletedFiles)
        text.append (String.format ("%s%n", name));
    }

    if (failedFiles.size () > 0)
    {
      text.append ("\n\nFailed files\n");
      text.append ("------------\n");
      for (String name : failedFiles)
        text.append (String.format ("%s%n", name));
    }

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void deleteFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    if (file.getParentFileSystem () != this)
      throw new InvalidParentFileSystemException ("file not part of this File System");

    // mark file as deleted in the catalog
    AppleBlock catalogSector = ((FileDos3) file).catalogEntryBlock;
    byte[] buffer = catalogSector.getBuffer ();
    int ptr = 11 + ((FileDos3) file).catalogEntryIndex * ENTRY_SIZE;
    buffer[ptr + 0x20] = buffer[ptr];
    buffer[ptr] = (byte) 0xFF;            // deleted file
    catalogSector.markDirty ();

    // mark file's sectors as free in the vtoc
    for (AppleBlock block : file.getBlocks ())      // index and data blocks
      volumeBitMap.set (block.getBlockNo ());

    AppleBlock vtoc = getSector (17, 0);
    buffer = vtoc.getBuffer ();
    writeVolumeBitMap (buffer);
    vtoc.markDirty ();
  }
}
