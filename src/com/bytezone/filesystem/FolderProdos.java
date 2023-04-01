package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FolderProdos extends AbstractAppleFile implements AppleContainer
// -----------------------------------------------------------------------------------//
{
  private static final DateTimeFormatter df = DateTimeFormatter.ofPattern ("d-LLL-yy");
  private static final DateTimeFormatter tf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  FileEntryProdos fileEntry;                  // SDH only
  DirectoryEntryProdos directoryEntry;        // both VDH and SDH

  List<AppleFile> files = new ArrayList<> ();

  // This file is used by both a VDH and SDH. The VDH has only a DirectoryEntry,
  // but an SDH is created first as a normal file (with a FileEntry), and then has
  // the DirectoryEntry added when the subdirectory is processed.
  // ---------------------------------------------------------------------------------//
  FolderProdos (FsProdos parent, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    if ((buffer[ptr] & 0xF0) == 0xF0)         // Volume Directory Header
    {
      fileTypeText = "VOL";
      directoryEntry = new DirectoryEntryProdos (buffer, ptr);
    }
    else                                      // Subdirectory
    {
      fileEntry = new FileEntryProdos (buffer, ptr);
      fileName = fileEntry.fileName;
      fileType = fileEntry.fileType;
      fileTypeText = ProdosConstants.fileTypes[fileEntry.fileType];
    }

    isFolder = true;
  }

  // ---------------------------------------------------------------------------------//
  void addDirectoryEntry (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    this.directoryEntry = new DirectoryEntryProdos (buffer, ptr);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return directoryEntry.totalBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return directoryEntry.fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFileSystem (AppleFileSystem fileSystem)
  // ---------------------------------------------------------------------------------//
  {
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFileSystem> getFileSystems ()
  // ---------------------------------------------------------------------------------//
  {
    return null;
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
  public LocalDateTime getCreated ()
  // ---------------------------------------------------------------------------------//
  {
    return directoryEntry.created;
  }

  // ---------------------------------------------------------------------------------//
  public LocalDateTime getModified ()
  // ---------------------------------------------------------------------------------//
  {
    return fileEntry.modified;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    return 0;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] read ()
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

    //    LocalDateTime created = this.created;
    //    LocalDateTime modified = fileEntry.modified;

    String dateCreated =
        directoryEntry.created == null ? NO_DATE : directoryEntry.created.format (df);
    String timeCreated =
        directoryEntry.created == null ? "" : directoryEntry.created.format (tf);
    //    String dateModified = modified == null ? NO_DATE : modified.format (df);
    //    String timeModified = modified == null ? "" : modified.format (tf);

    text.append (String.format ("%s%-15s %3s   %5d  %9s %5s %n", isLocked ? "*" : " ",
        fileName, fileTypeText, directoryEntry.totalBlocks, dateCreated, timeCreated));

    return text.toString ();

    //
    //        text.append (String.format ("%s%-15s %3s   %5d  %9s %5s  %9s %5s %8d %n",
    //            file.isLocked () ? "*" : " ", file.getFileName (), file.getFileTypeText (),
    //            file.getTotalBlocks (), dateModified, timeModified, dateCreated, timeCreated,
    //            file.getFileLength ()));
    //      
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    if (fileEntry != null)
    {
      text.append (fileEntry);
      text.append ("\n\n");
    }

    text.append (directoryEntry);

    return Utility.rtrim (text);
  }
}
