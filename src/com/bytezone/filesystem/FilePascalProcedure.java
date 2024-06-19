package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FilePascalProcedure extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  // all procedures have these fields
  byte[] buffer;
  int procHeader;
  int offset;
  int procNo;
  boolean valid;
  int dataLength;

  // only valid procedures have these fields
  int procedureNo;
  int procLevel;
  int codeStart;
  int codeEnd;
  int parmSize;
  int dataSize;

  byte[] data;
  DataRecord record;

  // ---------------------------------------------------------------------------------//
  FilePascalProcedure (FilePascalCode parent, byte[] buffer, int eof, int procNo)
  // ---------------------------------------------------------------------------------//
  {
    super (parent.getParentFileSystem ());

    this.buffer = buffer;
    this.procNo = procNo;               // 1-based

    fileName = String.format ("proc-%02d", procNo);
    fileTypeText = "PRC";
    fileType = 99;                      // pascal procedure

    int p = eof - 2 - procNo * 2;
    offset = Utility.unsignedShort (buffer, p);
    procHeader = p - offset;
    valid = procHeader > 0;

    if (valid)
    {
      procedureNo = buffer[procHeader] & 0xFF;
      procLevel = buffer[procHeader + 1] & 0xFF;

      codeStart = Utility.unsignedShort (buffer, procHeader - 2);
      codeEnd = Utility.unsignedShort (buffer, procHeader - 4);
      parmSize = Utility.unsignedShort (buffer, procHeader - 6);
      dataSize = Utility.unsignedShort (buffer, procHeader - 8);

      int start = procHeader - codeStart - 2;
      dataLength = codeStart + 4;

      data = new byte[dataLength];
      System.arraycopy (buffer, start, data, 0, dataLength);
      dataRecord = new DataRecord (buffer, start, dataLength);
    }
    else
      data = new byte[0];
  }

  // ---------------------------------------------------------------------------------//
  public int getCodeStart ()
  // ---------------------------------------------------------------------------------//
  {
    return procHeader - codeStart - 2;
  }

  // ---------------------------------------------------------------------------------//
  public int getCodeEnd ()
  // ---------------------------------------------------------------------------------//
  {
    return procHeader - codeEnd - 4;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    return dataLength;
  }

  // ---------------------------------------------------------------------------------//
  //  @Override
  //  public byte[] read ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return data;
  //  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    String firstHalf = String.format (" %2d   %04X", procNo, offset);
    if (!valid)
      return firstHalf;

    return String.format ("%s   %04X  %3d   %04X   %04X   %04X   %04X   %04X", firstHalf,
        procHeader, procLevel, codeStart, codeEnd, parmSize, dataSize, dataLength);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Procedure # ........... %6d%n", procNo));
    text.append (String.format ("Proc offset ........... %6d  %<04X%n", offset));

    if (valid)
    {
      text.append (String.format ("Proc header ........... %6d  %<04X%n", procHeader));
      if (procNo != procedureNo)
        text.append (String.format ("Proc no ...........**** %6d  %<04X%n", procedureNo));
      text.append (String.format ("Proc level ............ %6d  %<04X%n", procLevel));
      text.append (String.format ("Code start ............ %6d  %<04X%n", codeStart));
      text.append (String.format ("Code end .............. %6d  %<04X%n", codeEnd));
      text.append (String.format ("Parm size ............. %6d  %<04X%n", parmSize));
      text.append (String.format ("Data size ............. %6d  %<04X%n", dataSize));
      text.append (String.format ("Proc size ............. %6d  %<04X%n", dataLength));

    }

    return Utility.rtrim (text);
  }
}
