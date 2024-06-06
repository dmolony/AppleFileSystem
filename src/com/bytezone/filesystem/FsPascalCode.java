package com.bytezone.filesystem;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FsPascalCode extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  String comment;

  // ---------------------------------------------------------------------------------//
  public FsPascalCode (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.PASCAL_CODE);

    byte[] buffer = blockReader.getDiskBuffer ();

    int nonameCounter = 0;

    // Create segment list (up to 16 segments)
    for (int i = 0; i < 16; i++)
    {
      String codeName = Utility.string (buffer, 0x40 + i * 8, 8).trim ();
      int size = Utility.unsignedShort (buffer, i * 4 + 2);

      if (size > 0)
      {
        if (codeName.length () == 0)
          codeName = "<NULL" + ++nonameCounter + ">";

        // this could throw an exception
        FilePascalCode filePascalCode = new FilePascalCode (this, buffer, i, codeName);
        addFile (filePascalCode);
      }
    }

    comment = Utility.getPascalString (buffer, 0x1B0);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("Segment Dictionary\n==================\n\n");

    text.append ("Slot Addr Blks Byte   Name     Kind"
        + "            Txt Seg Mch Ver I/S I/S Disk:Block\n");
    text.append ("---- ---- ---- ----  --------  ---------------"
        + " --- --- --- --- --- --- ---------------------\n");

    for (AppleFile segment : getFiles ())
      text.append (segment.toString () + "\n");

    text.append ("\nComment : " + comment);

    return text.toString ();
  }
}
