package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FsNuFX extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{

  // ---------------------------------------------------------------------------------//
  public FsNuFX (String name, byte[] buffer, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public FsNuFX (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);
    setFileSystemName ("NuFX - Shrinkit");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {

  }
}
