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

  //  void write (byte[] buffer);

  //  void write ();

  //  default void dump ()
  //  {
  //    System.out.println (toString ());
  //    System.out.println (Utility.format (read ()));
  //  };
}
