package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FsDos4 extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final int ENTRY_SIZE = 35;
  int dosVersion;

  // ---------------------------------------------------------------------------------//
  public FsDos4 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.DOS4);

    assert getTotalCatalogBlocks () == 0;
    int catalogBlocks = 0;

    AppleBlock vtoc = getSector (17, 0);
    byte[] buffer = vtoc.read ();

    if (buffer[3] != 0x41 && buffer[3] != 0x42)
      return;

    dosVersion = buffer[3] & 0xFF;

    while (true)
    {
      int track = buffer[1] & 0xFF;
      int sector = buffer[2] & 0xFF;

      if (track == 0)
        break;

      track &= 0x3F;
      sector &= 0x1F;

      AppleBlock catalogSector = getSector (track, sector);
      if (!catalogSector.isValid ())
        return;

      buffer = catalogSector.read ();

      int ptr = 11;

      while (ptr < buffer.length && buffer[ptr] != 0)
      {
        if ((buffer[ptr] & 0x80) != 0)        // deleted file
        {

        }
        else
        {
          try
          {
            FileDos4 file = new FileDos4 (this, buffer, ptr);
            addFile (file);
          }
          catch (FileFormatException e)
          {
            break;
          }
        }

        ptr += ENTRY_SIZE;
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
    StringBuilder text = new StringBuilder (super.toText () + "\n\n");

    text.append ("\n");
    text.append (String.format ("Dos version ........... %02X", dosVersion));

    return text.toString ();
  }
}
