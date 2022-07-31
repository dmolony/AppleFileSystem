package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FsLbr extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{

  // ---------------------------------------------------------------------------------//
  public FsLbr (String name, byte[] buffer, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public FsLbr (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);
    setFileSystemName ("LBR");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    int max = 1;
    int count = 0;

    for (int i = 0; i < max; i++)
    {
      AppleBlock vtoc = getBlock (i);
      byte[] buffer = vtoc.read ();

      for (int j = 0; j < 4; j++)
      {
        FileLbr file = new FileLbr (this, buffer, j * 32);

        if (count++ == 0)                           // directory entry
        {
          if (file.status != 0 || !file.name.isBlank () || !file.extension.isBlank ())
            return;

          max = file.totalBlocks;                   // change outer loop
          setCatalogBlocks (file.totalBlocks);
          continue;
        }

        if (file.status == 0 && file.totalBlocks > 0)
        {
          addFile (file);
          ++totalFiles;
        }
      }
    }
  }
}
