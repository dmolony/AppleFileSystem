package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

//***************** obsolete ********************
// -----------------------------------------------------------------------------------//
public class PascalProcedure
// -----------------------------------------------------------------------------------//
{
  // all procedures have these fields
  byte[] buffer;
  int procHeader;
  int offset;
  int procNo;
  boolean valid;

  // only valid procedures have these fields
  int procedureNo;
  int procLevel;
  int codeStart;
  int codeEnd;
  int parmSize;
  int dataSize;

  //  List<PascalCodeStatement> statements = new ArrayList<> ();
  //  AssemblerProgram assembler;
  int jumpTable = -8;

  // ---------------------------------------------------------------------------------//
  public PascalProcedure (byte[] buffer, int eof, int procNo)
  // ---------------------------------------------------------------------------------//
  {
    this.procNo = procNo;               // 1-based

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
    }
  }

  // ---------------------------------------------------------------------------------//
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    String firstHalf = String.format (" %2d  %04X  %04X", procNo, offset, procHeader);
    if (!valid)
      return firstHalf;

    return String.format ("%s  %04X  %04X  %04X  %04X  %04X", firstHalf, procLevel,
        codeStart, codeEnd, parmSize, dataSize);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Procedure # ........... %d%n", procNo));
    text.append (String.format ("Proc offset ........... %6d  %<04X%n", offset));
    text.append (String.format ("Proc header ........... %6d  %<04X%n", procHeader));

    if (valid)
    {
      if (procNo != procedureNo)
        text.append (String.format ("Proc no .....****...... %6d  %<04X%n", procedureNo));
      text.append (String.format ("Proc level ............ %6d  %<04X%n", procLevel));
      text.append (String.format ("Code start ............ %6d  %<04X%n", codeStart));
      text.append (String.format ("Code end .............. %6d  %<04X%n", codeEnd));
      text.append (String.format ("Parm size ............. %6d  %<04X%n", parmSize));
      text.append (String.format ("Data size ............. %6d  %<04X%n", dataSize));

    }

    return Utility.rtrim (text);
  }
}
