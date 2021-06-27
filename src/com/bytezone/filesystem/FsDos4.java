package com.bytezone.filesystem;

public class FsDos4 extends AbstractFileSystem
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
    fileSystemName = "Dos" + switch (version)
    {
      case 0x41 -> "4.1";
      case 0x42 -> "4.2";
      default -> "?.?";
    };
    dosVersion = version & 0xFF;
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
          track = buffer[ptr] & 0xFF;
          sector = buffer[ptr + 1] & 0xFF;

          track &= 0x3F;
          sector &= 0x1F;

          AppleBlock tsSector = getSector (track, sector);
          if (!tsSector.isValid ())
            return;

          //          String name = string (buffer, ptr + 3, 30);
          //          System.out.println (name);
        }

        ptr += ENTRY_SIZE;
      }
      ++catalogBlocks;
    }
  }
}
