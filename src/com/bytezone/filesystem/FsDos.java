package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FsDos extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final int ENTRY_SIZE = 35;
  int dosVersion;

  // ---------------------------------------------------------------------------------//
  public FsDos (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length);
  }

  // ---------------------------------------------------------------------------------//
  public FsDos (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length);
    setFileSystemName ("Dosx.x");
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
  void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    assert catalogBlocks == 0;

    AppleBlock vtoc = getSector (17, 0);
    byte[] buffer = vtoc.read ();
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
            files.add (file);
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
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toText ());

    text.append (String.format ("Dos version ........... %02X%n", dosVersion));

    return text.toString ();
  }
}
