package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

public class Folder extends AbstractAppleFile implements AppleContainer
{
  List<AppleFile> files = new ArrayList<> ();
  List<AppleFileSystem> fileSystems = new ArrayList<> ();

  // This class is not used by FileSystem, but may be used by other projects when
  // displaying NuFX/Zip/Gzip file contents (eg DiskBrowser2 and AppleFormat)
  // ---------------------------------------------------------------------------------//
  public Folder (AppleFileSystem parent, String name)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    this.fileName = name;

    isFolder = true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    files.add (file);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()
  // ---------------------------------------------------------------------------------//
  {
    return files;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFileSystem (AppleFileSystem fileSystem)
  // ---------------------------------------------------------------------------------//
  {
    fileSystems.add (fileSystem);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFileSystem> getFileSystems ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystems;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    //    String line = "----  ---------  ---  - -  ----\n";

    //    text.append ("User  Name       Typ  R S  Size\n");
    //    text.append (line);

    for (AppleFile file : getFiles ())
    {
      text.append (file.getCatalogLine ());
      text.append ("\n");
    }

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("File type ............. %s", "Folder"));

    return Utility.rtrim (text);
  }
}
