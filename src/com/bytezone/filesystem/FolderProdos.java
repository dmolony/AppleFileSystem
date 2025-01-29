package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class FolderProdos extends AbstractAppleFile implements AppleContainer
// -----------------------------------------------------------------------------------//
{
  private static Locale US = Locale.US;          // to force 3 character months
  protected static final DateTimeFormatter sdf =
      DateTimeFormatter.ofPattern ("d-LLL-yy", US);
  protected static final DateTimeFormatter stf = DateTimeFormatter.ofPattern ("H:mm");
  protected static final String NO_DATE = "<NO DATE>";

  FileEntryProdos fileEntry;                  // SDH only
  DirectoryEntryProdos directoryEntry;        // both VDH and SDH
  AppleContainer parentContainer;

  List<AppleFile> files = new ArrayList<> ();
  List<AppleFileSystem> fileSystems = new ArrayList<> ();

  AppleBlock parentCatalogBlock;                // block containing this file entry
  int parentCatalogPtr;

  // ---------------------------------------------------------------------------------//
  FolderProdos (FsProdos fs, AppleContainer parentContainer,
      AppleBlock parentCatalogBlock, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    this.parentContainer = parentContainer;              // file system or folder
    this.parentCatalogBlock = parentCatalogBlock;
    this.parentCatalogPtr = ptr;

    fileEntry = new FileEntryProdos (parentCatalogBlock, ptr);

    fileName = fileEntry.fileName;
    fileType = fileEntry.fileType;
    fileTypeText = ProdosConstants.fileTypes[fileEntry.fileType];

    directoryEntry =
        new DirectoryEntryProdos ((FsProdos) parentFileSystem, fileEntry.keyPtr);

    processFolder (this);
    dataBlocks.addAll (directoryEntry.catalogBlocks);

    isFolder = true;
  }

  // ---------------------------------------------------------------------------------//
  private void processFolder (AppleContainer parent)
  // ---------------------------------------------------------------------------------//
  {
    FsProdos fs = (FsProdos) parentFileSystem;

    FileProdos file = null;

    for (AppleBlock catalogBlock : directoryEntry.catalogBlocks)
    {
      byte[] buffer = catalogBlock.getBuffer ();

      int ptr = 4;
      for (int i = 0; i < ProdosConstants.ENTRIES_PER_BLOCK; i++)
      {
        int blockType = (buffer[ptr] & 0xF0) >>> 4;

        switch (blockType)
        {
          case ProdosConstants.SEEDLING:
          case ProdosConstants.SAPLING:
          case ProdosConstants.TREE:
            file = new FileProdos (fs, parent, catalogBlock, ptr);
            parent.addFile (file);

            if (file.getFileType () == ProdosConstants.FILE_TYPE_LBR)
              fs.addEmbeddedFileSystem (file, 0);

            break;

          case ProdosConstants.PASCAL_ON_PROFILE:
            file = new FileProdos (fs, parent, catalogBlock, ptr);
            parent.addFile (file);
            fs.addEmbeddedFileSystem (file, 1024);
            break;

          case ProdosConstants.GSOS_EXTENDED_FILE:
            parent.addFile (new FileProdos (fs, parent, catalogBlock, ptr));
            break;

          case ProdosConstants.SUBDIRECTORY:
            FolderProdos folder = new FolderProdos (fs, parent, catalogBlock, ptr);
            parent.addFile (folder);
            break;

          case ProdosConstants.SUBDIRECTORY_HEADER:
          case ProdosConstants.VOLUME_HEADER:
          case ProdosConstants.FREE:
            break;

          default:
            System.out.printf ("Unknown Blocktype: %02X%n", blockType);
        }

        ptr += ProdosConstants.ENTRY_SIZE;
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntry.blocksUsed;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return directoryEntry.fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFileSystem (AppleFileSystem fileSystem)
  // ---------------------------------------------------------------------------------//
  {
    fileSystems.add (fileSystem);           // Not used AFAIK
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFileSystem> getFileSystems ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystems;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    files.add (file);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()
  // ---------------------------------------------------------------------------------//
  {
    return files;
  }

  // ---------------------------------------------------------------------------------//
  public LocalDateTime getCreated ()
  // ---------------------------------------------------------------------------------//
  {
    return directoryEntry.created;
  }

  // ---------------------------------------------------------------------------------//
  public LocalDateTime getModified ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntry.modified;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntry.eof;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return parentContainer.getPath () + "/" + getFileName ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Optional<AppleFile> getFile (String fileName)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleFile file : files)
      if (file.getFileName ().equals (fileName))
        return Optional.of (file);

    return Optional.empty ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    LocalDateTime created = getCreated ();
    LocalDateTime modified = getModified ();

    int fileLength = isForkedFile () ? 0 : getFileLength ();

    String dateCreated = created == null ? NO_DATE : created.format (sdf);
    String timeCreated = created == null ? "" : created.format (stf);
    String dateModified = modified == null ? NO_DATE : modified.format (sdf);
    String timeModified = modified == null ? "" : modified.format (stf);

    String forkFlag = isForkedFile () ? "+" : " ";

    text.append (String.format ("%s%-15s %3s%s  %5d  %9s %5s  %9s %5s %8d%n",
        isLocked () ? "*" : " ", getFileName (), getFileTypeText (), forkFlag,
        getTotalBlocks (), dateModified, timeModified, dateCreated, timeCreated,
        fileLength));

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("%s%n%n", getPath ()));

    text.append (" NAME           TYPE  BLOCKS  "
        + "MODIFIED         CREATED          ENDFILE SUBTYPE" + "\n\n");

    for (AppleFile file : getFiles ())
    {
      text.append (file.getCatalogLine ());
      text.append ("\n");
    }

    int totalBlocks = getParentFileSystem ().getTotalBlocks ();
    int freeBlocks = getParentFileSystem ().getFreeBlocks ();

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
    StringBuilder text = new StringBuilder (super.toString ());

    if (fileEntry != null)
    {
      text.append (fileEntry);
      text.append ("\n\n");
    }

    text.append (directoryEntry);

    return Utility.rtrim (text);
  }
}
