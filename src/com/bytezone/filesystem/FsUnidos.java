package com.bytezone.filesystem;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public class FsUnidos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final int UNIDOS_SIZE = 409_600;

  private boolean debug = true;

  // ---------------------------------------------------------------------------------//
  public FsUnidos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.UNIDOS);

    byte[] buffer = getDiskBuffer ();
    int offset = getDiskOffset ();

    //    addFile (new FileUnidos (this, "DISK 1", buffer, offset, UNIDOS_SIZE));
    //    addFile (new FileUnidos (this, "DISK 2", buffer, offset + UNIDOS_SIZE, UNIDOS_SIZE));
    //
    //    checkFileSystem ((AbstractAppleFile) files.get (0), 0);
    //    checkFileSystem ((AbstractAppleFile) files.get (1), 0);

    try
    {
      BlockReader blockReader1 = new BlockReader ("DISK 1", buffer, offset, UNIDOS_SIZE);
      blockReader1.setParameters (256, AddressType.SECTOR, 0, 32);
      FsDos fs1 = new FsDos (blockReader1);

      if (fs1 != null && fs1.getTotalCatalogBlocks () > 0)
      {
        BlockReader blockReader2 =
            new BlockReader ("DISK 2", buffer, offset + UNIDOS_SIZE, UNIDOS_SIZE);
        blockReader2.setParameters (256, AddressType.SECTOR, 0, 32);
        FsDos fs2 = new FsDos (blockReader2);

        if (fs2 != null && fs2.getTotalCatalogBlocks () > 0)
        {
          addFileSystem (fs1);
          addFileSystem (fs2);
        }
      }
    }
    catch (FileFormatException e)
    {
      if (debug)
        System.out.println (e);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    for (AppleFile file : getFiles ())
      text.append (String.format ("%s%n", file.getFileName ()));

    return text.toString ();
  }
}
