package com.bytezone.filesystem;

import java.util.List;

// -----------------------------------------------------------------------------------//
public class FsBinary2 extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final byte[] BIN2 = { 0x0A, 0x47, 0x4C };

  private FileSystemFactory factory;

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
    setFileSystemName ("Binary II");
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
        for (AppleFileSystem fs : checkLibraryFile (file))
          addFile (fs);
      else
        addFile (file);

      nextBlock += ((file.getEof () - 1) / 128 + 2);
    } while (file.getFilesFollowing () > 0);
  }

  // ---------------------------------------------------------------------------------//
  private List<AppleFileSystem> checkLibraryFile (FileBinary2 file)
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

    if (factory == null)
      factory = new FileSystemFactory ();

    return factory.getFileSystems (file.getName (), file.read ());
  }
}
