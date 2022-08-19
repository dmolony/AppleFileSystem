package com.bytezone.filesystem;

import java.io.File;

// -----------------------------------------------------------------------------------//
public class FileLocal extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  File file;

  // ---------------------------------------------------------------------------------//
  FileLocal (File file)
  // ---------------------------------------------------------------------------------//
  {
    super (null);

    this.file = file;
    this.name = file.getName ();
    this.isFolder = file.isDirectory ();
    this.isFile = true;
  }
}
