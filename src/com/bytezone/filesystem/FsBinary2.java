package com.bytezone.filesystem;

import java.nio.file.Path;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsBinary2 extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] BIN2 = { 0x0A, 0x47, 0x4C };

  private String suffix;

  // ---------------------------------------------------------------------------------//
  public FsBinary2 (Path path, BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (path, blockReader);

    String name = filePath.toFile ().getName ();
    int pos = name.lastIndexOf ('.');
    if (pos > 0)
      suffix = name.substring (pos + 1).toLowerCase ();

    readCatalog ();
  }

  // ---------------------------------------------------------------------------------//
  public FsBinary2 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader);

    readCatalog ();
  }

  // ---------------------------------------------------------------------------------//
  private void readCatalog ()
  // ---------------------------------------------------------------------------------//
  {
    setFileSystemName ("Bin II");

    assert Utility.isMagic (blockReader.diskBuffer, blockReader.diskOffset, BIN2)
        && blockReader.diskBuffer[blockReader.diskOffset + 18] == 0x02;

    int nextBlock = 0;
    FileBinary2 file = null;
    int filesRemaining = 0;

    do
    {
      if (!isValidBlockNo (nextBlock))
      {
        System.out.printf ("Invalid block number %d in %s%n", nextBlock, getName ());
        break;
      }

      file = new FileBinary2 (this, nextBlock);

      if (!file.isValid ())         // not all the blocks are available
        break;

      filesRemaining = file.getFilesFollowing ();

      if (file.getFileType () == ProdosConstants.FILE_TYPE_LBR)
        addFileSystem (this, file);
      else
        addFile (file);

      nextBlock += ((file.getEof () - 1) / 128 + 2);

    } while (filesRemaining > 0);

    if (false)
      if (filesRemaining > 0)
        System.out.println (filesRemaining + " files unavailable");
  }

  // ---------------------------------------------------------------------------------//
  public String getSuffix ()
  // ---------------------------------------------------------------------------------//
  {
    return suffix;
  }
}
