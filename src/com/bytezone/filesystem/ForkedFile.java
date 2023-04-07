package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
public interface ForkedFile
// -----------------------------------------------------------------------------------//
{
  public List<AppleFile> getForks ();

  public String getCatalog ();
}
