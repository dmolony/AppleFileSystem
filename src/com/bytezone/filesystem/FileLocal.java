package com.bytezone.filesystem;

import java.io.File;

// -----------------------------------------------------------------------------------//
public class FileLocal extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  File file;

  // ---------------------------------------------------------------------------------//
  FileLocal (File file)
  // ---------------------------------------------------------------------------------//
  {
    super (null);

    this.file = file;
    this.fileName = file.getName ();
    this.isFolder = file.isDirectory ();
  }
}
