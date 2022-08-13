package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FsUnidos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final int UNIDOS_SIZE = 409_600;

  private boolean debug = false;

  // ---------------------------------------------------------------------------------//
  public FsUnidos (String name, byte[] buffer, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public FsUnidos (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);

    setFileSystemName ("Unidos");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = getBuffer ();
    int offset = getOffset ();

    try
    {
      FsDos fs1 = new FsDos ("Disk 1", buffer, offset, UNIDOS_SIZE, blockReader);
      fs1.readCatalog ();

      if (fs1 != null && fs1.getTotalCatalogBlocks () > 0)
      {
        FsDos fs2 = new FsDos ("Disk 2", buffer, offset + UNIDOS_SIZE, UNIDOS_SIZE, blockReader);
        fs2.readCatalog ();

        if (fs2 != null && fs2.getTotalCatalogBlocks () > 0)
        {
          addFile (fs1);
          addFile (fs2);
          totalFileSystems += 2;
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
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toText ());

    //    text.append (String.format ("Entry length .......... %d%n", entryLength));

    return text.toString ();
  }
}
