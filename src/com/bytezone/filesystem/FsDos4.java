package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public class FsDos4 extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final int ENTRY_SIZE = 35;
  int dosVersion;

  // ---------------------------------------------------------------------------------//
  public FsDos4 (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length);
  }

  // ---------------------------------------------------------------------------------//
  public FsDos4 (String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length);
    setFileSystemName ("Dos4.x");
  }

  // ---------------------------------------------------------------------------------//
  private void setVersion (byte version)
  // ---------------------------------------------------------------------------------//
  {
    dosVersion = version & 0xFF;
    setFileSystemName ("Dos" + switch (version)
    {
      case 0x41 -> "4.1";
      case 0x42 -> "4.2";
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

    if (buffer[3] != 0x41 && buffer[3] != 0x42)
      return;

    setVersion (buffer[3]);

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
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("Dos version ........... %02X%n", dosVersion));

    return text.toString ();
  }
}
