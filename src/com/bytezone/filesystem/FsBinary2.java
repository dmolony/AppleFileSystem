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

    assert Utility.isMagic (buffer, offset, BIN2) && buffer[offset + 18] == 0x02;

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
      {
        addFile (file);
        //        ++totalFiles;
      }

      nextBlock += ((file.getEof () - 1) / 128 + 2);

    } while (filesRemaining > 0);

    if (filesRemaining > 0)
      System.out.println (filesRemaining + " files unavailable");
  }
}
