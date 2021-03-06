package com.bytezone.woz;

// -----------------------------------------------------------------------------------//
class DiskReader16Sector extends DiskReader
// -----------------------------------------------------------------------------------//
{
  private static final int RAW_BUFFER_SIZE = 342;
  private static final int BUFFER_WITH_CHECKSUM_SIZE = RAW_BUFFER_SIZE + 1;

  private final byte[] decodeA = new byte[BUFFER_WITH_CHECKSUM_SIZE];
  private final byte[] decodeB = new byte[RAW_BUFFER_SIZE];

  private final byte[] encodeA = new byte[RAW_BUFFER_SIZE];
  private final byte[] encodeB = new byte[BUFFER_WITH_CHECKSUM_SIZE];

  private final ByteTranslator byteTranslator = new ByteTranslator6and2 ();

  // ---------------------------------------------------------------------------------//
  DiskReader16Sector ()
  // ---------------------------------------------------------------------------------//
  {
    super (16);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  byte[] decodeSector (byte[] buffer, int offset) throws DiskNibbleException
  // ---------------------------------------------------------------------------------//
  {
    // rearrange 342 bytes into 256
    byte[] decodedBuffer = new byte[SECTOR_SIZE];             // 256 bytes

    // convert legal disk values to actual 6 bit values
    for (int i = 0; i < BUFFER_WITH_CHECKSUM_SIZE; i++)      // 343 bytes
      decodeA[i] = (byte) (byteTranslator.decode (buffer[offset++]) << 2);

    // reconstruct 342 bytes each with 6 bits
    byte chk = 0;
    for (int i = decodeB.length - 1; i >= 0; i--)            // 342 bytes
      chk = decodeB[i] = (byte) (decodeA[i + 1] ^ chk);
    if ((chk ^ decodeA[0]) != 0)
      throw new DiskNibbleException ("Checksum failed");

    // move 6 bits into place
    for (int i = 0; i < SECTOR_SIZE; i++)
      decodedBuffer[i] = decodeB[i + 86];

    // reattach each byte's last 2 bits
    for (int i = 0, j = 86, k = 172; i < 86; i++, j++, k++)
    {
      byte val = decodeB[i];

      decodedBuffer[i] |= reverse ((val & 0x0C) >> 2);
      decodedBuffer[j] |= reverse ((val & 0x30) >> 4);

      if (k < SECTOR_SIZE)
        decodedBuffer[k] |= reverse ((val & 0xC0) >> 6);
    }

    return decodedBuffer;
  }

  // convert 256 data bytes into 342 translated bytes plus a checksum
  // ---------------------------------------------------------------------------------//
  @Override
  byte[] encodeSector (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    byte[] encodedBuffer = new byte[BUFFER_WITH_CHECKSUM_SIZE];

    // move data buffer down to make room for the 86 extra bytes
    for (int i = 0; i < SECTOR_SIZE; i++)
      encodeA[i + 86] = buffer[i];

    // build extra 86 bytes from the bits stripped from the data bytes
    for (int i = 0; i < 86; i++)
    {
      int b1 = reverse (buffer[i] & 0x03) << 2;
      int b2 = reverse (buffer[i + 86] & 0x03) << 4;

      if (i < 84)
      {
        int b3 = reverse (buffer[i + 172] & 0x03) << 6;
        encodeA[i] = (byte) (b1 | b2 | b3);
      }
      else
        encodeA[i] = (byte) (b1 | b2);
    }

    // convert into checksum bytes
    byte checksum = 0;
    for (int i = 0; i < RAW_BUFFER_SIZE; i++)
    {
      encodeB[i] = (byte) (checksum ^ encodeA[i]);
      checksum = encodeA[i];
    }

    encodeB[RAW_BUFFER_SIZE] = checksum;        // add checksum to the end

    // remove two bits and convert to translated bytes
    for (int i = 0; i < BUFFER_WITH_CHECKSUM_SIZE; i++)
      encodedBuffer[i] = byteTranslator.encode (encodeB[i]);

    return encodedBuffer;
  }
}
