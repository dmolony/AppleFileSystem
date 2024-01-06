package com.bytezone.filesystem;

import com.bytezone.filesystem.AppleBlock.BlockType;

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
      AppleBlock block = getBlock (i);
      block.setBlockType (BlockType.FILE_DATA);
      byte[] buffer = block.read ();

      for (int j = 0; j < 4; j++)
      {
        FileLbr file = new FileLbr (this, buffer, j * 32);

        if (count++ == 0)                           // directory entry
        {
          if (file.status != 0 || !file.fileName.isBlank () || !file.extension.isBlank ())
            throw new FileFormatException ("LBR: Invalid header");

          max = file.totalBlocks;                   // change outer loop
          setTotalCatalogBlocks (file.totalBlocks);
          continue;
        }

        if (file.status == 0 && file.totalBlocks > 0)
          addFile (file);
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    for (AppleFile file : getFiles ())
      text.append (
          String.format ("%-15s %s%n", file.getFileName (), file.getFileSystemType ()));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return super.toString ();
  }
}
