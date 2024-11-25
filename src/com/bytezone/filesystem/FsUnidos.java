package com.bytezone.filesystem;

import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
class FsUnidos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final int UNIDOS_SIZE = 409_600;

  private boolean debug = true;

  // ---------------------------------------------------------------------------------//
  FsUnidos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.UNIDOS);

    byte[] buffer = getDataRecord ().data ();
    int offset = getDataRecord ().offset ();

    //    addFile (new FileUnidos (this, "DISK 1", buffer, offset, UNIDOS_SIZE));
    //    addFile (new FileUnidos (this, "DISK 2", buffer, offset + UNIDOS_SIZE, UNIDOS_SIZE));
    //
    //    checkFileSystem ((AbstractAppleFile) files.get (0), 0);
    //    checkFileSystem ((AbstractAppleFile) files.get (1), 0);

    try
    {
      BlockReader blockReader1 = new BlockReader ("DISK 1", buffer, offset, UNIDOS_SIZE);
      blockReader1.setParameters (256, AddressType.SECTOR, 0, 32);
      FsDos3 fs1 = new FsDos3 (blockReader1);

      if (fs1 != null && fs1.getTotalCatalogBlocks () > 0)
      {
        BlockReader blockReader2 =
            new BlockReader ("DISK 2", buffer, offset + UNIDOS_SIZE, UNIDOS_SIZE);
        blockReader2.setParameters (256, AddressType.SECTOR, 0, 32);
        FsDos3 fs2 = new FsDos3 (blockReader2);

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
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    for (AppleFileSystem fileSystem : getFileSystems ())
    {
      text.append (fileSystem.getCatalogText ());
      text.append ("\n\n");
    }

    return text.toString ();
  }
}
