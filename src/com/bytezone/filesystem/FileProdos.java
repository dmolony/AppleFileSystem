package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileProdos extends AbstractAppleFile implements AppleForkedFile
// -----------------------------------------------------------------------------------//
{
  private static Locale US = Locale.US;                 // to force 3 character months
  protected static final DateTimeFormatter sdf =
      DateTimeFormatter.ofPattern ("d-LLL-yy", US);
  protected static final DateTimeFormatter stf = DateTimeFormatter.ofPattern ("H:mm");
  protected static final String NO_DATE = "<NO DATE>";

  final CatalogEntryProdos catalogEntry;
  private AppleContainer parentContainer;

  private ForkProdos dataFork;                        // for non-forked files
  List<AppleFile> forks = new ArrayList<> ();         // for forked files

  // ---------------------------------------------------------------------------------//
  FileProdos (FsProdos parentFs, AppleContainer parentContainer,
      AppleBlock parentCatalogBlock, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (parentFs);

    this.parentContainer = parentContainer;

    catalogEntry = new CatalogEntryProdos (parentCatalogBlock, slot);
    isForkedFile = catalogEntry.storageType == ProdosConstants.GSOS_EXTENDED_FILE;

    if (isForkedFile)
      createForks ();
    else
      dataFork = new ForkProdos (this, null, catalogEntry.keyPtr,
          catalogEntry.storageType, catalogEntry.blocksUsed, catalogEntry.eof);
  }

  // ---------------------------------------------------------------------------------//
  private void createForks ()
  // ---------------------------------------------------------------------------------//
  {
    AppleBlock block =
        getParentFileSystem ().getBlock (catalogEntry.keyPtr, BlockType.FS_DATA);

    block.setBlockSubType ("FORK");
    block.setFileOwner (this);

    dataBlocks.add (block);
    byte[] buffer = block.getBuffer ();

    for (int ptr = 0; ptr < 512; ptr += 256)
    {
      int storageType = buffer[ptr] & 0x0F;                       // use right nybble!
      int keyPtr = Utility.unsignedShort (buffer, ptr + 1);
      int size = Utility.unsignedShort (buffer, ptr + 3);
      int eof = Utility.unsignedTriple (buffer, ptr + 5);

      if (keyPtr > 0)
        forks.add (new ForkProdos (this, ptr == 0 ? ForkType.DATA : ForkType.RESOURCE,
            keyPtr, storageType, size, eof));
    }
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
  public List<AppleBlock> getDataBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return isForkedFile ? dataBlocks : dataFork.getDataBlocks ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleBlock> getAllBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    List<AppleBlock> blocks = new ArrayList<> ();
    blocks.add (catalogEntry.catalogBlock);

    if (isForkedFile)
    {
      blocks.addAll (dataBlocks);                   // the fork index block
      for (AppleFile fork : forks)
        blocks.addAll (fork.getAllBlocks ());
    }
    else
      blocks.addAll (dataFork.getAllBlocks ());

    return blocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isValidFile ()
  // ---------------------------------------------------------------------------------//
  {
    return getTotalBlocks () > 0;
  }

  // ---------------------------------------------------------------------------------//
  public int getAuxType ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.auxType;
  }

  // ---------------------------------------------------------------------------------//
  public int getStorageType ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.storageType;
  }

  // ---------------------------------------------------------------------------------//
  public LocalDateTime getCreated ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.created;
  }

  // ---------------------------------------------------------------------------------//
  public LocalDateTime getModified ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.modified;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Buffer getFileBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    if (isForkedFile ())
      return super.getFileBuffer ();

    return dataFork.getFileBuffer ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()                                       // in bytes (eof)
  // ---------------------------------------------------------------------------------//
  {
    if (isForkedFile ())
      return catalogEntry.eof;

    return dataFork.getFileLength ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()                                      // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.blocksUsed;                // size of both forks if GSOS extended
  }

  // ---------------------------------------------------------------------------------//
  String getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return parentContainer.getPath () + "/" + getFileName ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getForks ()
  // ---------------------------------------------------------------------------------//
  {
    return forks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    if (!isForkedFile)
      throw new FileFormatException ("Cannot getCatalog() on a non-forked file");

    StringBuilder text = new StringBuilder ();

    text.append (" NAME           TYPE  BLOCKS  "
        + "MODIFIED         CREATED          ENDFILE SUBTYPE" + "\n\n");

    for (AppleFile file : getForks ())
    {
      text.append (file.getCatalogLine ());
      text.append ("\n");
    }

    return text.toString ();
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

    text.append (String.format ("%s%-15s %3s%s  %5d  %9s %5s  %9s %5s %8d %7s%n",
        isLocked () ? "*" : " ", getFileName (), getFileTypeText (), forkFlag,
        getTotalBlocks (), dateModified, timeModified, dateCreated, timeCreated,
        fileLength, getSubType ()));

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  private String getSubType ()
  // ---------------------------------------------------------------------------------//
  {
    switch (getFileType ())
    {
      case ProdosConstants.FILE_TYPE_TEXT:
        return String.format ("R=%5d", getAuxType ());

      case ProdosConstants.FILE_TYPE_BINARY:
      case ProdosConstants.FILE_TYPE_PNT:
      case ProdosConstants.FILE_TYPE_PIC:
      case ProdosConstants.FILE_TYPE_FOT:
        return String.format ("A=$%04X", getAuxType ());
    }

    return "";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("\n");
    text.append (catalogEntry);
    text.append ("\n\n");

    if (dataFork != null)
      text.append (dataFork);

    return Utility.rtrim (text);
  }
}
