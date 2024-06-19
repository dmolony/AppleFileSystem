package com.bytezone.filesystem;

public record DataRecord (byte[] data, int offset, int length)
{
  public int max ()
  {
    return offset + length;
  }
}
