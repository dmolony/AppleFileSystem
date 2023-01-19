package com.bytezone.filesystem;

import java.nio.file.Path;

// https://applesaucefdc.com/moof-reference/
// -----------------------------------------------------------------------------------//
public class FsMoof extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  public FsMoof (Path path, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (path, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public FsMoof (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader);
  }
}
