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
  private static Locale US = Locale.US;        // to force 3 character months
  protected static final DateTimeFormatter sdf =
      DateTimeFormatter.ofPattern ("d-LLL-yy", US);
  protected static final DateTimeFormatter stf = DateTimeFormatter.ofPattern ("H:mm");
  protected static final String NO_DATE = "<NO DATE>";

  CatalogEntryProdos catalogEntry;             // standard prodos catalog entry
  DirectoryHeaderProdos directoryEntry;        // SDH
  AppleContainer parentContainer;

  List<AppleFile> files = new ArrayList<> ();
  List<AppleFileSystem> fileSystems = new ArrayList<> ();

  // Every ProdosFolder is represented by a standard Prodos File Entry, and its
  // key ptr indicates the first (1:n) block of its own catalog where the SDH is
  // located.
  // Like all file entries it has a pointer back to the directory block which
  // contains the VDH/SDH for the set of catalog blocks which contain this entry.
  // That makes it easier to update the total # of files in this catalog.
  // ---------------------------------------------------------------------------------//
  FolderProdos (FsProdos fs, AppleContainer parentContainer,
      AppleBlock parentCatalogBlock, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    this.parentContainer = parentContainer;              // file system or folder

    catalogEntry = new CatalogEntryProdos (parentCatalogBlock, slot);
    //    checkChain (parentCatalogBlock.getBlockNo (), catalogEntry.headerPtr);

    // create the Sub Directory Header (which collects all the catalog blocks)
    // this is the first entry in the first catalog block pointed to by the key ptr
    directoryEntry =
        new DirectoryHeaderProdos ((FsProdos) parentFileSystem, catalogEntry.keyPtr);

    // the data blocks for a folder are the 1:n blocks containing its catalog file list
    dataBlocks.addAll (directoryEntry.catalogBlocks);

    fs.readCatalog (this, directoryEntry.catalogBlocks);
    //    readCatalog ();

    isFolder = true;
  }

  // ---------------------------------------------------------------------------------//
  private void checkChain (int thisBlockNo, int headerBlockNo)
  // ---------------------------------------------------------------------------------//
  {
    System.out.printf ("This block: %04X Header: %04X  SDH: %04X  %s%n",
        catalogEntry.catalogBlock.getBlockNo (), catalogEntry.headerPtr,
        catalogEntry.keyPtr, catalogEntry.fileName);
    System.out.printf ("%04X  %04X%n", thisBlockNo, headerBlockNo);

    while (thisBlockNo != headerBlockNo)
    {
      byte[] buffer = parentFileSystem.getBlock (thisBlockNo).getBuffer ();
      thisBlockNo = Utility.unsignedShort (buffer, 0);
      System.out.printf ("%04X  %04X%n", thisBlockNo, headerBlockNo);
    }
    ;
  }

  // ---------------------------------------------------------------------------------//
  private void readCatalog ()
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
            file = new FileProdos (fs, this, catalogBlock, i);
            addFile (file);

            if (file.getFileType () == ProdosConstants.FILE_TYPE_LBR)
              fs.addEmbeddedFileSystem (file, 0);

            break;

          case ProdosConstants.PASCAL_ON_PROFILE:
            file = new FileProdos (fs, this, catalogBlock, i);
            addFile (file);
            fs.addEmbeddedFileSystem (file, 1024);
            break;

          case ProdosConstants.GSOS_EXTENDED_FILE:
            addFile (new FileProdos (fs, this, catalogBlock, i));
            break;

          case ProdosConstants.SUBDIRECTORY:
            FolderProdos folder = new FolderProdos (fs, this, catalogBlock, i);
            addFile (folder);
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
    return catalogEntry.blocksUsed;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getAllBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    List<AppleBlock> blocks = new ArrayList<AppleBlock> (dataBlocks);
    blocks.add (catalogEntry.catalogBlock);

    return blocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.fileType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return ProdosConstants.fileTypes[catalogEntry.fileType];
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.isLocked;
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
    return catalogEntry.modified;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.eof;
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
    int freeBlocks = getParentFileSystem ().getTotalFreeBlocks ();

    text.append (
        String.format ("%nBLOCKS FREE:%5d     BLOCKS USED:%5d     TOTAL BLOCKS:%5d%n",
            freeBlocks, totalBlocks - freeBlocks, totalBlocks));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void sort ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("sort () not implemented in FolderProdos");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("\n");
    text.append (catalogEntry);         // the folder's own file entry
    text.append ("\n\n");
    text.append (directoryEntry);       // the directory header (SDH)

    return Utility.rtrim (text);
  }
}
