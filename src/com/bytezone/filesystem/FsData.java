package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FsData extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  public FsData (String name, byte[] buffer, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length, blockReader);

    setFileSystemName ("Data");
  }

  // ---------------------------------------------------------------------------------//
  public FsData (String fileName, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (fileName, buffer, offset, length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    addFile (new FileData (this, getBuffer (), 0));
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    return super.toText ();
  }
}
