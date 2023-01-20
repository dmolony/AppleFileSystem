package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public interface AppleBlock
// -----------------------------------------------------------------------------------//
{
  int getBlockNo ();

  int getTrackNo ();

  int getSectorNo ();

  boolean isValid ();

  byte[] read ();

  void write (byte[] buffer);

  void write ();
}
