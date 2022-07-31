package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsBinary2 extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] BIN2 = { 0x0A, 0x47, 0x4C };

  private String suffix;

  // ---------------------------------------------------------------------------------//
  public FsBinary2 (String name, byte[] buffer, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    this (name, buffer, 0, buffer.length, blockReader);
  }

  // ---------------------------------------------------------------------------------//
  public FsBinary2 (String name, byte[] buffer, int offset, int length, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer, offset, length, blockReader);

    setFileSystemName ("Bin II");

    assert Utility.isMagic (buffer, 0, BIN2);

    int pos = name.lastIndexOf ('.');
    if (pos > 0)
      suffix = name.substring (pos + 1).toLowerCase ();
  }

  // ---------------------------------------------------------------------------------//
  public String getSuffix ()
  // ---------------------------------------------------------------------------------//
  {
    return suffix;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    int nextBlock = 0;
    FileBinary2 file = null;

    do
    {
      file = new FileBinary2 (this, nextBlock);

      if (file.getFileType () == ProdosConstants.FILE_TYPE_LBR)
        addFileSystem (this, file);
      else
      {
        addFile (file);
        ++totalFiles;
      }

      nextBlock += ((file.getEof () - 1) / 128 + 2);
    } while (file.getFilesFollowing () > 0);
  }

  // ---------------------------------------------------------------------------------//
  private void showDetails (FileBinary2 file)
  // ---------------------------------------------------------------------------------//
  {
    String description = switch (file.getAuxType ())
    {
      case 0x0001 -> "AppleSingle file";
      case 0x0005 -> "DiskCopy file";
      case 0x0130 -> "2IMG file";
      case 0x8000 -> "Binary II file";
      case 0x8002 -> "Shrinkit (NuFX) file";
      case 0x8004 -> "Davex file";
      default -> "Unknown aux";
    };

    System.out.printf ("%04X  %s - %s%n", file.getAuxType (), file.getName (), description);
  }
}
