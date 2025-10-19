package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public record Buffer (byte[] data, int offset, int length)
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  public Buffer
  // ---------------------------------------------------------------------------------//
  {
    if (offset + length > data.length)
    {
      throw new java.lang.IllegalArgumentException (
          String.format ("Invalid Buffer size: Offset: %d, Length: %d", offset, length));
    }
  }

  // ---------------------------------------------------------------------------------//
  public int max ()
  // ---------------------------------------------------------------------------------//
  {
    return offset + length;
  }

  // ---------------------------------------------------------------------------------//
  public boolean equals (Buffer other)
  // ---------------------------------------------------------------------------------//
  {
    return data == other.data && offset == other.offset && length == other.length;
  }

  // ---------------------------------------------------------------------------------//
  public byte[] copyData ()
  // ---------------------------------------------------------------------------------//
  {
    byte[] newData = new byte[length];
    System.arraycopy (data, offset, newData, 0, length);

    return newData;
  }

  // ---------------------------------------------------------------------------------//
  public Buffer copyBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return new Buffer (copyData (), 0, length);
  }

  // ---------------------------------------------------------------------------------//
  public boolean isMagic (int offset, byte[] magic)
  // ---------------------------------------------------------------------------------//
  {
    return Utility.isMagic (data, this.offset + offset, magic);
  }

  // ---------------------------------------------------------------------------------//
  public boolean byteAt (int offset, byte magic)
  // ---------------------------------------------------------------------------------//
  {
    return data ()[this.offset + offset] == magic;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Buffer ... %04X %<d%n", data.length));
    text.append (String.format ("Offset ... %04X %<d%n", offset));
    text.append (String.format ("Length ... %04X %<d%n", length));
    text.append (Utility.format (data, offset, length, true, offset));

    return text.toString ();
  }
}
