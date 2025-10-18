package com.bytezone.woz;

// https://www.bigmessowires.com/2021/11/12/the-amazing-disk-ii-controller-card/
// -----------------------------------------------------------------------------------//
abstract class DiskReader
// -----------------------------------------------------------------------------------//
{
  static final int SECTOR_SIZE = 256;
  static final int BLOCK_SIZE = 512;
  static final byte[] dataPrologue = { (byte) 0xD5, (byte) 0xAA, (byte) 0xAD };

  protected static DiskReader reader13;
  protected static DiskReader reader16;
  protected static DiskReader readerGCR;

  final int sectorsPerTrack;

  // ---------------------------------------------------------------------------------//
  DiskReader (int sectorsPerTrack)
  // ---------------------------------------------------------------------------------//
  {
    this.sectorsPerTrack = sectorsPerTrack;
  }

  // ---------------------------------------------------------------------------------//
  static DiskReader getInstance (int sectors)
  // ---------------------------------------------------------------------------------//
  {
    return switch (sectors)
    {
      case 13 -> diskReader13Sector ();
      case 16 -> diskReader16Sector ();
      case 0 -> diskReaderGCR ();
      default -> null;
    };
  }

  // ---------------------------------------------------------------------------------//
  private static DiskReader diskReader13Sector ()
  // ---------------------------------------------------------------------------------//
  {
    if (reader13 == null)
      reader13 = new DiskReader13Sector ();

    return reader13;
  }

  // ---------------------------------------------------------------------------------//
  private static DiskReader diskReader16Sector ()
  // ---------------------------------------------------------------------------------//
  {
    if (reader16 == null)
      reader16 = new DiskReader16Sector ();

    return reader16;
  }

  // ---------------------------------------------------------------------------------//
  private static DiskReader diskReaderGCR ()
  // ---------------------------------------------------------------------------------//
  {
    if (readerGCR == null)
      readerGCR = new DiskReaderGCR ();

    return readerGCR;
  }

  // ---------------------------------------------------------------------------------//
  byte[] decodeSector (byte[] buffer) throws DiskNibbleException
  // ---------------------------------------------------------------------------------//
  {
    return decodeSector (buffer, 0);
  }

  // ---------------------------------------------------------------------------------//
  abstract byte[] decodeSector (byte[] buffer, int offset) throws DiskNibbleException;
  // ---------------------------------------------------------------------------------//

  // ---------------------------------------------------------------------------------//
  abstract byte[] encodeSector (byte[] buffer);
  // ---------------------------------------------------------------------------------//
}
