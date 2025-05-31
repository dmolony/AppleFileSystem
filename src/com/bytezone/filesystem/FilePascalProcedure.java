package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FilePascalProcedure extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  // all procedures have these fields
  int procHeader;
  int offset;
  int procNo;
  int dataLength;

  // only valid procedures have these fields
  int procedureNo;
  int lexLevel;
  int entryIC;
  int exitIC;
  int parmSize;
  int dataSize;

  Buffer record;
  FilePascalSegment parent;

  String fileName;
  int fileType;
  String fileTypeText;

  // ---------------------------------------------------------------------------------//
  FilePascalProcedure (FilePascalSegment parent, byte[] buffer, int eof, int procNo)
  // ---------------------------------------------------------------------------------//
  {
    super (parent.getParentFileSystem ());

    this.parent = parent;
    this.procNo = procNo;               // 1-based

    fileName = String.format ("proc-%02d", procNo);
    fileTypeText = "PRC";
    fileType = 99;                      // pascal procedure

    int p = eof - 2 - procNo * 2;
    offset = Utility.unsignedShort (buffer, p);
    procHeader = p - offset;

    if (procHeader > 0)
    {
      procedureNo = buffer[procHeader] & 0xFF;
      lexLevel = buffer[procHeader + 1] & 0xFF;

      entryIC = Utility.unsignedShort (buffer, procHeader - 2);
      exitIC = Utility.unsignedShort (buffer, procHeader - 4);
      parmSize = Utility.unsignedShort (buffer, procHeader - 6);
      dataSize = Utility.unsignedShort (buffer, procHeader - 8);

      int start = procHeader - 2 - entryIC;
      dataLength = entryIC + 4;

      rawFileBuffer = new Buffer (buffer, start, dataLength);
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return fileName;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileType ()
  // ---------------------------------------------------------------------------------//
  {
    return fileType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileTypeText ()
  // ---------------------------------------------------------------------------------//
  {
    return fileTypeText;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isLocked ()
  // ---------------------------------------------------------------------------------//
  {
    return false;
  }

  // ---------------------------------------------------------------------------------//
  public int getCodeStart ()
  // ---------------------------------------------------------------------------------//
  {
    return procHeader - 2 - entryIC;
  }

  // ---------------------------------------------------------------------------------//
  public int getCodeEnd ()
  // ---------------------------------------------------------------------------------//
  {
    return procHeader - 4 - exitIC;
  }

  // ---------------------------------------------------------------------------------//
  public int getProcHeader ()
  // ---------------------------------------------------------------------------------//
  {
    return procHeader;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    return dataLength;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    String firstHalf = String.format (" %2d   %04X", procNo, offset);
    if (procHeader <= 0)
      return firstHalf;

    return String.format ("%s   %04X  %3d   %04X   %04X   %04X   %04X   %04X", firstHalf,
        procHeader, lexLevel, entryIC, exitIC, parmSize, dataSize, dataLength);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (parent.toString ());

    text.append ("\n\n");
    text.append ("------ Procedure ------\n");
    text.append (String.format ("Procedure # ........... %,6d%n", procNo));
    text.append (String.format ("Proc offset ........... %,6d  %<04X%n", offset));

    if (procHeader > 0)
    {
      text.append (String.format ("Proc header ........... %,6d  %<04X%n", procHeader));
      if (procNo != procedureNo)
        text.append (
            String.format ("Procedure no ......**** %,6d  %<04X%n", procedureNo));
      text.append (String.format ("Lex level ............. %,6d  %<04X%n", lexLevel));
      text.append (String.format ("Entry instruction ..... %,6d  %<04X%n", entryIC));
      text.append (String.format ("Exit instruction ...... %,6d  %<04X%n", exitIC));
      text.append (String.format ("Paramater size ........ %,6d  %<04X%n", parmSize));
      text.append (String.format ("Data size ............. %,6d  %<04X%n", dataSize));
      text.append (String.format ("Proc size ............. %,6d  %<04X%n", dataLength));

    }

    return Utility.rtrim (text);
  }
}
