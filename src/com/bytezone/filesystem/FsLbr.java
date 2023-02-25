package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FsLbr extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  public FsLbr (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.LBR);

    int max = 1;
    int count = 0;

    for (int i = 0; i < max; i++)
    {
      byte[] buffer = getBlock (i).read ();

      for (int j = 0; j < 4; j++)
      {
        FileLbr file = new FileLbr (this, buffer, j * 32);

        if (count++ == 0)                           // directory entry
        {
          if (file.status != 0 || !file.name.isBlank () || !file.extension.isBlank ())
            throw new FileFormatException ("LBR: Invalid header");

          max = file.totalBlocks;                   // change outer loop
          setCatalogBlocks (file.totalBlocks);
          continue;
        }

        if (file.status == 0 && file.totalBlocks > 0)
          addFile (file);
      }
    }
  }
}
