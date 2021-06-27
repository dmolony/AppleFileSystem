package com.bytezone.filesystem;

// -----------------------------------------------------------------------------------//
public interface AppleBlock
// -----------------------------------------------------------------------------------//
{
  int getBlockNo ();

  int getTrack ();

  int getSector ();

  boolean isValid ();

  byte[] read ();

  void write (byte[] buffer);

  void write ();
}
