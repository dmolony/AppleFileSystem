package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FsDos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final int ENTRY_SIZE = 35;
  int dosVersion;

  enum FileType
  {
    Text, ApplesoftBasic, IntegerBasic, Binary, SS, Relocatable, AA, BB
  }

  // ---------------------------------------------------------------------------------//
  public FsDos (String name, byte[] buffer, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public FsDos (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  private void setVersion (byte version)
  // ---------------------------------------------------------------------------------//
  {
    dosVersion = version & 0xFF;
    setFileSystemName ("Dos" + switch (version)
    {
      case 0x01 -> "3.1";
      case 0x02 -> "3.2";
      case 0x03 -> "3.3";
      default -> "?.?";
    });
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    assert getTotalCatalogBlocks () == 0;
    int catalogBlocks = 0;

    AppleBlock vtoc = getSector (17, 0);
    if (!vtoc.isValid ())
      return;
    byte[] buffer = vtoc.read ();

    if (buffer[3] < 0x01 || buffer[3] > 0x03)
      return;

    setVersion (buffer[3]);

    while (true)
    {
      int track = buffer[1] & 0xFF;
      int sector = buffer[2] & 0xFF;

      if (track == 0)
        break;

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
            FileDos file = new FileDos (this, buffer, ptr);
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
    StringBuilder text = new StringBuilder (super.toText ());

    text.append ("\n");
    text.append (String.format ("Dos version ........... %02X", dosVersion));

    return text.toString ();
  }
}
