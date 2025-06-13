package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//-----------------------------------------------------------------------------------//
class FileProdosGS extends AbstractAppleFile implements AppleContainer
//-----------------------------------------------------------------------------------//
{
  AppleContainer parentContainer;
  List<AppleFile> files = new ArrayList<> ();             // data and resource forks
  List<AppleFileSystem> fileSystems = new ArrayList<> ();
  final CatalogEntryProdos catalogEntry;

  // ---------------------------------------------------------------------------------//
  FileProdosGS (FsProdos parentFs, AppleContainer parentContainer,
      AppleBlock parentCatalogBlock, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (parentFs);

    this.parentContainer = parentContainer;
    catalogEntry = new CatalogEntryProdos (parentCatalogBlock, slot);
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
  public void addFileSystem (AppleFileSystem fileSystem)
  // ---------------------------------------------------------------------------------//
  {
    fileSystems.add (fileSystem);           // Not used AFAIK
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
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return catalogEntry.isLocked;
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
  public void sort ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("sort () not implemented in FileProdosGS");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    return text.toString ();
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
  @Override
  public Optional<AppleFile> getFile (String fileName)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "getFile () not implemented in FileProdosGS");
  }
}
