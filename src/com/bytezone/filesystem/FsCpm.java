package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FsCpm extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final int EMPTY_BYTE_VALUE = 0xE5;

  // ---------------------------------------------------------------------------------//
  public FsCpm (String name, byte[] buffer, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public FsCpm (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);

    setFileSystemName ("CPM");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    assert getTotalCatalogBlocks () == 0;

    int catalogBlocks = 0;
    FileCpm currentFile = null;

    out: for (int i = 0; i < 2; i++)
    {
      byte[] buffer = getBlock (12 + i).read ();
      //      System.out.println (Utility.format (buffer));

      for (int j = 0; j < buffer.length; j += 32)
      {
        int b1 = buffer[j] & 0xFF;          // user number
        if (b1 == EMPTY_BYTE_VALUE)         // deleted file??
          continue;
        if (b1 > 31)
          //          throw new FileFormatException ("CPM: bad user number: " + b1);
          break out;

        int b2 = buffer[j + 1] & 0xFF;      // first letter of filename
        if (b2 <= 32 || (b2 > 126 && b2 != EMPTY_BYTE_VALUE))
          //          throw new FileFormatException ("CPM: bad name value");
          break out;

        if (currentFile == null || currentFile.isComplete ())
        {
          currentFile = new FileCpm (this, buffer, j);
          this.addFile (currentFile);
        }
        else
          currentFile.append (buffer, j);
      }

      ++catalogBlocks;
    }

    setCatalogBlocks (catalogBlocks);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toText ());

    text.append ("\n");
    text.append (String.format ("Entry length .......... %d%n", 32));
    text.append (String.format ("Entries per block ..... %d%n", getBlockSize () / 32));
    text.append (String.format ("File count ............ %d", getFiles ().size ()));

    return text.toString ();
  }
}
