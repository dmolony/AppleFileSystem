package com.bytezone.filesystem;

import java.util.List;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileCpm extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  private final List<FileEntryCpm> fileEntries;

  // ---------------------------------------------------------------------------------//
  FileCpm (FsCpm parent, List<FileEntryCpm> fileEntries)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    assert fileEntries.size () > 0;

    this.fileEntries = fileEntries;
    //    this.fileTypeText = fileEntries.get (0).getFileTypeText ();
    //    this.fileName = fileEntries.get (0).getFileName ();

    for (FileEntryCpm fileEntry : fileEntries)
      for (int blockNo : fileEntry)
      {
        AppleBlock block = getParentFileSystem ().getBlock (blockNo, BlockType.FILE_DATA);
        if (block == null)
        {
          System.out.printf ("Null block: %d%n", blockNo);
        }
        else
        {
          block.setFileOwner (this);
          dataBlocks.add (block);
        }
      }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntries.get (0).getFileName ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntries.get (0).getFileTypeText ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntries.get (0).isReadOnly ();
  }

  // ---------------------------------------------------------------------------------//
  public boolean isSystemFile ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntries.get (0).isSystemFile ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    for (FileEntryCpm fileEntry : fileEntries)
    {
      text.append (fileEntry.getLine ());
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

    for (FileEntryCpm fileEntry : fileEntries)
    {
      text.append (fileEntry);
      text.append ("\n\n");
    }

    return Utility.rtrim (text);
  }
}
