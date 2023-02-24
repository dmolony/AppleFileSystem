package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FsDos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final int ENTRY_SIZE = 35;
  int dosVersion;

  // ---------------------------------------------------------------------------------//
  public FsDos (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.DOS);

    readCatalog ();
  }

  // ---------------------------------------------------------------------------------//
  private void setVersion (byte version)
  // ---------------------------------------------------------------------------------//
  {
    dosVersion = version & 0xFF;
    //    setFileSystemName ("Dos" + switch (version)
    //    {
    //      case 0x01 -> "3.1";
    //      case 0x02 -> "3.2";
    //      case 0x03 -> "3.3";
    //      default -> "?.?";
    //    });
  }

  // ---------------------------------------------------------------------------------//
  private void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    assert getTotalCatalogBlocks () == 0;
    int catalogBlocks = 0;

    AppleBlock vtoc = getSector (17, 0);
    if (!vtoc.isValid ())
      throw new FileFormatException ("Dos: Invalid VTOC");

    byte[] buffer = vtoc.read ();

    if (buffer[3] < 0x01 || buffer[3] > 0x03)
      throw new FileFormatException ("Dos: byte 3 invalid");

    setVersion (buffer[3]);

    while (true)
    {
      int track = buffer[1] & 0xFF;
      int sector = buffer[2] & 0xFF;

      if (track == 0)
        break;

      AppleBlock catalogSector = getSector (track, sector);
      if (!catalogSector.isValid ())
        throw new FileFormatException ("Dos: Invalid catalog sector");

      buffer = catalogSector.read ();

      int ptr = 11;

      while (ptr < buffer.length && buffer[ptr] != 0)
      {
        if ((buffer[ptr] & 0x80) != 0)        // deleted file
        {
          // could make a list for Extras' panel
        }
        else
        {
          try
          {
            FileDos file = new FileDos (this, buffer, ptr);
            addFile (file);
          }
          catch (FileFormatException e)
          {
            // could prepare list of failures for Extras' panel
            //            String fileName = Utility.string (buffer, ptr + 3, 30).trim ();
            //            System.out.println (fileName + " failed");
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

    text.append (String.format ("Dos version ........... %02X", dosVersion));

    return text.toString ();
  }
}
