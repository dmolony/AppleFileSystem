package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.util.BitSet;

import com.bytezone.utility.Utility;

// see https://prodos8.com/docs/techref/file-organization/
// see https://prodos8.com/docs/technote/25/
// -----------------------------------------------------------------------------------//
public class FsProdos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private final BitSet volumeBitMap;
  private DirectoryEntryProdos directoryEntry;

  // ---------------------------------------------------------------------------------//
  public FsProdos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.PRODOS);

    int nextBlockNo = 2;                    // first catalog block
    int prevBlockNo = -1;

    assert catalogBlocks == 0;
    int catalogBlocks = 0;

    while (nextBlockNo != 0)
    {
      AppleBlock vtoc = getBlock (nextBlockNo);
      byte[] buffer = vtoc.read ();

      if (catalogBlocks == 0)
      {
        directoryEntry = new DirectoryEntryProdos (buffer, 4);
        //        int type = (buffer[0x04] & 0xF0) >>> 4;
        if (directoryEntry.storageType != ProdosConstants.VOLUME_HEADER)
          throw new FileFormatException ("FsProdos: No Volume Header");

        //        rootFolder = new FolderProdos (this, buffer, 4);
        //        addFile (rootFolder);

        if (directoryEntry.entryLength != ProdosConstants.ENTRY_SIZE
            || directoryEntry.entriesPerBlock != ProdosConstants.ENTRIES_PER_BLOCK)
          throw new FileFormatException ("FsProdos: Invalid entry data");

        if (directoryEntry.keyPtr < 3 || directoryEntry.keyPtr > 10)
          throw new FileFormatException (
              "FsProdos: Invalid bitmap block value: " + directoryEntry.keyPtr);
      }

      prevBlockNo = Utility.unsignedShort (buffer, 0);
      nextBlockNo = Utility.unsignedShort (buffer, 2);

      if (!isValidBlockNo (prevBlockNo))
        throw new FileFormatException (
            "FsProdos: Invalid catalog previous block - " + prevBlockNo);

      if (!isValidBlockNo (nextBlockNo))
        throw new FileFormatException (
            "FsProdos: Invalid catalog next block - " + nextBlockNo);

      ++catalogBlocks;
    }

    processFolder (this, 2);
    //    System.out.printf ("%-15s has %d files%n", directoryEntry.fileName,
    //        getFiles ().size ());
    assert directoryEntry.fileCount == getFiles ().size ();
    setCatalogBlocks (catalogBlocks);

    volumeBitMap = createVolumeBitMap ();
  }

  // ---------------------------------------------------------------------------------//
  private void processFolder (AppleContainer parent, int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    AppleBlock catalogBlock = getBlock (blockNo);
    FileProdos file = null;

    while (catalogBlock.getBlockNo () != 0)
    {
      byte[] buffer = catalogBlock.read ();

      int ptr = 4;
      for (int i = 0; i < ProdosConstants.ENTRIES_PER_BLOCK; i++)
      {
        int blockType = (buffer[ptr] & 0xF0) >>> 4;

        switch (blockType)
        {
          case ProdosConstants.SEEDLING:
          case ProdosConstants.SAPLING:
          case ProdosConstants.TREE:
            file = new FileProdos (this, buffer, ptr);
            parent.addFile (file);

            if (file.getFileType () == ProdosConstants.FILE_TYPE_LBR)
              checkEmbeddedFileSystem (file, 0);

            break;

          case ProdosConstants.PASCAL_ON_PROFILE:
            file = new FileProdos (this, buffer, ptr);
            parent.addFile (file);
            checkEmbeddedFileSystem (file, 1024);
            break;

          case ProdosConstants.GSOS_EXTENDED_FILE:
            parent.addFile (new FileProdos (this, buffer, ptr));
            break;

          case ProdosConstants.SUBDIRECTORY:
            FolderProdos folder = new FolderProdos (this, buffer, ptr);
            parent.addFile (folder);
            processFolder (folder, folder.fileEntry.keyPtr);        // recursive
            break;

          case ProdosConstants.SUBDIRECTORY_HEADER:
            ((FolderProdos) parent).addDirectoryEntry (buffer, ptr);
            break;

          case ProdosConstants.VOLUME_HEADER:
            break;

          case ProdosConstants.FREE:
            break;

          default:
            System.out.printf ("Unknown Blocktype: %02X%n", blockType);
        }
        ptr += ProdosConstants.ENTRY_SIZE;
      }

      catalogBlock = getBlock (Utility.unsignedShort (buffer, 2));

      if (!catalogBlock.isValid ())
        throw new FileFormatException ("FsProdos: Invalid catalog");
    }
  }

  // ---------------------------------------------------------------------------------//
  private BitSet createVolumeBitMap ()
  // ---------------------------------------------------------------------------------//
  {
    int bitPtr = 0;
    int bfrPtr = 0;
    int blockNo = directoryEntry.keyPtr;
    byte[] buffer = null;

    BitSet bitMap = new BitSet (directoryEntry.totalBlocks);

    while (bitPtr < directoryEntry.totalBlocks)
    {
      if (bitPtr % 0x1000 == 0)
      {
        buffer = getBlock (blockNo++).read ();
        bfrPtr = 0;
      }

      byte flags = buffer[bfrPtr++];

      for (int i = 0; i < 8; i++)
      {
        if ((flags & 0x80) != 0)
          bitMap.set (bitPtr);

        flags <<= 1;
        ++bitPtr;
      }
    }

    freeBlocks = volumeBitMap.cardinality ();

    return bitMap;
  }

  // ---------------------------------------------------------------------------------//
  public String getVolumeName ()
  // ---------------------------------------------------------------------------------//
  {
    return directoryEntry.fileName;
  }

  // ---------------------------------------------------------------------------------//
  public static String getFileTypeText (int fileType)
  // ---------------------------------------------------------------------------------//
  {
    return ProdosConstants.fileTypes[fileType];
  }

  // ---------------------------------------------------------------------------------//
  private String getSubType (FileProdos file)
  // ---------------------------------------------------------------------------------//
  {
    switch (file.getFileType ())
    {
      case ProdosConstants.FILE_TYPE_TEXT:
        return String.format ("R=%5d", file.getAuxType ());

      case ProdosConstants.FILE_TYPE_BINARY:
      case ProdosConstants.FILE_TYPE_PNT:
      case ProdosConstants.FILE_TYPE_PIC:
      case ProdosConstants.FILE_TYPE_FOT:
        return String.format ("A=$%4X", file.getAuxType ());
    }

    return "";
  }

  // ---------------------------------------------------------------------------------//
  public String getProdosCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("/" + directoryEntry.fileName + "\n\n");

    text.append (" NAME           TYPE  BLOCKS  "
        + "MODIFIED         CREATED          ENDFILE SUBTYPE" + "\n\n");

    for (AppleFile file : getFiles ())
    {
      //      if (file.isEmbeddedFileSystem ())                             // LBR or PAR
      //        file = (AppleFileSystem) getAppleFile ();

      //      else if (file.isFile ())
      {
        FileProdos prodos = (FileProdos) file;

        LocalDateTime created = prodos.getCreated ();
        LocalDateTime modified = prodos.getModified ();

        int fileLength = file.isForkedFile () ? 0 : file.getFileLength ();

        String dateCreated = created == null ? NO_DATE : created.format (sdf);
        String timeCreated = created == null ? "" : created.format (stf);
        String dateModified = modified == null ? NO_DATE : modified.format (sdf);
        String timeModified = modified == null ? "" : modified.format (stf);

        String forkFlag = file.isForkedFile () ? "+" : " ";

        text.append (String.format (
            "%s%-15s %3s%s  %5d  %9s %5s  %9s %5s %8d %7s    %04X%n",
            file.isLocked () ? "*" : " ", file.getFileName (), file.getFileTypeText (),
            forkFlag, file.getTotalBlocks (), dateModified, timeModified, dateCreated,
            timeCreated, fileLength, getSubType (prodos), prodos.getAuxType ()));
      }
    }

    int totalBlocks = getTotalBlocks ();
    int freeBlocks = getFreeBlocks ();

    text.append (
        String.format ("%nBLOCKS FREE:%5d     BLOCKS USED:%5d     TOTAL BLOCKS:%5d%n",
            freeBlocks, totalBlocks - freeBlocks, totalBlocks));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString () + "\n\n");

    text.append (directoryEntry);
    //    text.append (String.format ("Volume name ........... %s%n", volumeName));
    //    text.append (
    //        String.format ("Created ............... %s%n", created == null ? "" : created));
    //    text.append (String.format ("Entry length .......... %d%n", entryLength));
    //    text.append (String.format ("Entries per block ..... %d%n", entriesPerBlock));
    //    text.append (String.format ("File count ............ %d%n", fileCount));
    //    text.append (String.format ("Bitmap ptr ............ %d", bitmapPointer));

    return text.toString ();
  }
}
