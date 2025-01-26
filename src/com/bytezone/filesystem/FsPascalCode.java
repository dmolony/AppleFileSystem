package com.bytezone.filesystem;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

//***************** obsolete ********************
// -----------------------------------------------------------------------------------//
public class FsPascalCode extends AbstractFileSystem
// -----------------------------------------------------------------------------------//
{
  private static final int SIZE_PTR = 0x02;
  private static final int NAME_PTR = 0x40;
  String comment;

  // ---------------------------------------------------------------------------------//
  public FsPascalCode (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    super (blockReader, FileSystemType.PASCAL_CODE);

    AppleBlock block = getBlock (0, BlockType.FS_DATA);
    byte[] buffer = block.getBuffer ();
    block.setBlockSubType ("CATALOG");
    setTotalCatalogBlocks (1);

    int nonameCounter = 0;
    int namePtr = NAME_PTR;
    int sizePtr = SIZE_PTR;

    // Create segment list (up to 16 segments)
    for (int i = 0; i < 16; i++)
    {
      String segmentName = Utility.string (buffer, namePtr, 8).trim ();
      namePtr += 8;

      int size = Utility.unsignedShort (buffer, sizePtr);
      sizePtr += 4;

      if (size > 0)
      {
        if (segmentName.length () == 0)
          segmentName = "NONAME-" + ++nonameCounter;

        FilePascalCodeSegment filePascalCode =
            new FilePascalCodeSegment (this, buffer, i, segmentName);
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
    text.append (
        "Slot Addr Size Eof    Name     Kind            Txt Seg Mch Ver I/S I/S Proc\n");
    text.append (
        "---- ---- ---- ----  --------  --------------- --- --- --- --- --- --- ----\n");

    for (AppleFile segment : getFiles ())
      text.append (segment.getCatalogLine () + "\n");

    text.append ("\nComment : " + comment);

    return text.toString ();
  }
}
