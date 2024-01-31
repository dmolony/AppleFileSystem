package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsBinary2 extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  static final byte[] BIN2 = { 0x0A, 0x47, 0x4C };

  // ---------------------------------------------------------------------------------//
  public FsBinary2 (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.BIN2);

    assert blockReader.isMagic (0, BIN2) && blockReader.byteAt (18, (byte) 0x02);

    int nextBlock = 0;
    FileBinary2 file = null;
    int filesRemaining = 0;

    do
    {
      if (!isValidBlockNo (nextBlock))
      {
        System.out.printf ("Invalid block number %d in %s%n", nextBlock, getFileName ());
        break;
      }

      file = new FileBinary2 (this, nextBlock);

      if (!file.isValid ())         // not all the blocks are available
        break;

      filesRemaining = file.getFilesFollowing ();

      if (!file.isPhantomFile ())
      {
        if (file.getFileType () == ProdosConstants.FILE_TYPE_LBR)
          addEmbeddedFileSystem (file, 0);
        addFile (file);
      }

      nextBlock += ((file.getEof () - 1) / 128 + 2);

    } while (filesRemaining > 0);

    if (false)
      if (filesRemaining > 0)
        System.out.println (filesRemaining + " files unavailable");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    for (AppleFile file : getFiles ())
    {
      text.append (file.getCatalogLine ());
      text.append ("\n");
    }

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return Utility.rtrim (new StringBuilder (super.toString ()));
  }
}
