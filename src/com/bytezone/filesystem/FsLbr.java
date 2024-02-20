package com.bytezone.filesystem;

import com.bytezone.filesystem.AppleBlock.BlockType;

// -----------------------------------------------------------------------------------//
public class FsLbr extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  FsLbr (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.LBR);

    int max = 1;
    int count = 0;

    for (int i = 0; i < max; i++)
    {
      AppleBlock block = getBlock (i, BlockType.FS_DATA);
      block.setBlockSubType ("CATALOG");
      byte[] buffer = block.read ();

      for (int j = 0; j < 4; j++)
      {
        FileLbr file = new FileLbr (this, buffer, j * 32);

        if (count++ == 0)                           // directory entry
        {
          if (file.status != 0 || !file.fileName.isBlank () || !file.extension.isBlank ())
            throw new FileFormatException ("LBR: Invalid header");

          max = file.length;                   // change outer loop
          setTotalCatalogBlocks (file.length);
          continue;
        }

        if (file.status == 0 && file.length > 0)
          addFile (file);
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    for (AppleFile file : getFiles ())
    {
      text.append (file.getCatalogLine ());
      text.append ("\n");
      //      text.append (
      //          String.format ("%-15s %s%n", file.getFileName (), file.getFileSystemType ()));
    }

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
