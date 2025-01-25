package com.bytezone.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class FolderProdos extends AbstractAppleFile implements AppleContainer
// -----------------------------------------------------------------------------------//
{
  private static Locale US = Locale.US;          // to force 3 character months
  protected static final DateTimeFormatter sdf =
      DateTimeFormatter.ofPattern ("d-LLL-yy", US);
  protected static final DateTimeFormatter stf = DateTimeFormatter.ofPattern ("H:mm");
  protected static final String NO_DATE = "<NO DATE>";

  FileEntryProdos fileEntry;                  // SDH only
  DirectoryEntryProdos directoryEntry;        // both VDH and SDH
  AppleContainer parent;

  List<AppleFile> files = new ArrayList<> ();
  List<AppleFileSystem> fileSystems = new ArrayList<> ();

  // This file is used by both a VDH and SDH. The VDH has only a DirectoryEntry,
  // but an SDH is created first as a normal file (with a FileEntry), and then has
  // the DirectoryEntry added when the subdirectory is processed.
  // ---------------------------------------------------------------------------------//
  FolderProdos (FsProdos parent, AppleContainer container, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (parent);

    this.parent = container;

    if ((buffer[ptr] & 0xF0) == 0xF0)         // Volume Directory Header
    {
      fileTypeText = "VOL";
      directoryEntry = new DirectoryEntryProdos (buffer, ptr);
      //      fileTypeText = ProdosConstants.fileTypes[directoryEntry.fileType];
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
    return fileEntry.blocksUsed;
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
    fileSystems.add (fileSystem);           // Not used AFAIK
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
    return fileEntry.eof;
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public byte[] read ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return null;
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return parent.getPath () + "/" + getFileName ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Optional<AppleFile> getFile (String fileName)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleFile file : files)
      if (file.getFileName ().equals (fileName))
        return Optional.of (file);

    return Optional.empty ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void putFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    System.out.println ("FolderProdos.putFile() not written yet");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void deleteFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    System.out.println ("FolderProdos.deleteFile() not written yet");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    LocalDateTime created = getCreated ();
    LocalDateTime modified = getModified ();

    int fileLength = isForkedFile () ? 0 : getFileLength ();

    String dateCreated = created == null ? NO_DATE : created.format (sdf);
    String timeCreated = created == null ? "" : created.format (stf);
    String dateModified = modified == null ? NO_DATE : modified.format (sdf);
    String timeModified = modified == null ? "" : modified.format (stf);

    String forkFlag = isForkedFile () ? "+" : " ";

    text.append (String.format ("%s%-15s %3s%s  %5d  %9s %5s  %9s %5s %8d%n",
        isLocked () ? "*" : " ", getFileName (), getFileTypeText (), forkFlag,
        getTotalBlocks (), dateModified, timeModified, dateCreated, timeCreated,
        fileLength));

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("%s%n%n", getPath ()));

    text.append (" NAME           TYPE  BLOCKS  "
        + "MODIFIED         CREATED          ENDFILE SUBTYPE" + "\n\n");

    for (AppleFile file : getFiles ())
    {
      text.append (file.getCatalogLine ());
      text.append ("\n");
    }

    int totalBlocks = getParentFileSystem ().getTotalBlocks ();
    int freeBlocks = getParentFileSystem ().getFreeBlocks ();

    text.append (
        String.format ("%nBLOCKS FREE:%5d     BLOCKS USED:%5d     TOTAL BLOCKS:%5d%n",
            freeBlocks, totalBlocks - freeBlocks, totalBlocks));

    return text.toString ();
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
