package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class FsUnidos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final int UNIDOS_SIZE = 409_600;

  private boolean debug = false;

  // ---------------------------------------------------------------------------------//
  FsUnidos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.UNIDOS);

    byte[] buffer = getDiskBuffer ().data ();
    int offset = getDiskBuffer ().offset ();

    try
    {
      BlockReader blockReader1 = new BlockReader ("DISK 1", buffer, offset, UNIDOS_SIZE);
      blockReader1.setParameters (FileSystemFactory.dos4);

      FsDos3 fs1 = new FsDos3 (blockReader1);

      if (fs1 != null && fs1.getTotalCatalogBlocks () > 0)
      {
        BlockReader blockReader2 =
            new BlockReader ("DISK 2", buffer, offset + UNIDOS_SIZE, UNIDOS_SIZE);
        blockReader2.setParameters (FileSystemFactory.dos4);

        FsDos3 fs2 = new FsDos3 (blockReader2);

        if (fs2 != null && fs2.getTotalCatalogBlocks () > 0)
        {
          addFileSystem (fs1);
          addFileSystem (fs2);

          fs1.readCatalogBlocks ();
          fs2.readCatalogBlocks ();
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

    return Utility.rtrim (text);
  }
}
