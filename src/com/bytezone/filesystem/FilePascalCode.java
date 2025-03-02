package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FilePascalCode extends FilePascal implements AppleContainer
// -----------------------------------------------------------------------------------//
{
  private static final int SIZE_PTR = 0x00;
  private static final int NAME_PTR = 0x40;
  private static final int KIND_PTR = 0xC0;

  private final List<AppleFile> segments = new ArrayList<> ();
  private final List<AppleFileSystem> notPossible = new ArrayList<> (0);

  String comment;

  // ---------------------------------------------------------------------------------//
  FilePascalCode (FsPascal fs, CatalogEntryPascal catalogEntry)
  // ---------------------------------------------------------------------------------//
  {
    super (fs, catalogEntry);

    AppleBlock block = fs.getBlock (getFirstBlock ());
    byte[] buffer = block.getBuffer ();          // code catalog

    if (!validSegmentDictionary (buffer))
      return;

    int nonameCounter = 0;
    int namePtr = NAME_PTR;
    int sizePtr = SIZE_PTR;

    // Create segment list (up to 16 segments)
    for (int i = 0; i < 16; i++)
    {
      String segmentName = Utility.string (buffer, namePtr, 8).trim ();
      namePtr += 8;

      int size = Utility.unsignedShort (buffer, sizePtr + 2);
      sizePtr += 4;

      if (size > 0)
      {
        if (segmentName.length () == 0)
          segmentName = "NONAME-" + ++nonameCounter;

        FilePascalSegment segment = new FilePascalSegment (this, buffer, i, segmentName);
        segments.add (segment);
      }
    }

    if (false)
    {
      for (AppleFile appleFile : segments)
      {
        FilePascalSegment fps = (FilePascalSegment) appleFile;
        if (fps.getFileLength () != fps.totSize)
          System.out.printf ("%8.8s Eof: %,7d, Size: %,7d%n", fps.getFileName (),
              fps.getFileLength (), fps.totSize);
        else
          System.out.printf ("%8.8s Eof: %,7d%n", fps.getFileName (),
              fps.getFileLength ());
      }
      System.out.println ();
    }

    comment = Utility.getPascalString (buffer, 0x1B0);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("cannot add File to " + getFileName ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()
  // ---------------------------------------------------------------------------------//
  {
    return segments;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Optional<AppleFile> getFile (String fileName)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleFile segment : segments)
      if (segment.getFileName ().equals (fileName))
        return Optional.of (segment);

    return Optional.empty ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFileSystem (AppleFileSystem fileSystem)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "cannot add FileSystem to " + getFileName ());
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFileSystem> getFileSystems ()
  // ---------------------------------------------------------------------------------//
  {
    return notPossible;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("Segment Dictionary\n==================\n\n");
    text.append (
        "Slot Addr Size    Eof    Name     Kind            Txt Seg Mch Ver I/S I/S Proc\n");
    text.append (
        "---- ---- ---- -------  --------  --------------- --- --- --- --- --- --- ----\n");

    for (AppleFile segment : segments)
      text.append (segment.getCatalogLine () + "\n");

    text.append ("\nComment : " + comment);

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return "NO PATH";
  }

  // ---------------------------------------------------------------------------------//
  private boolean validSegmentDictionary (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    int kindPtr = KIND_PTR;
    int sizePtr = SIZE_PTR;

    for (int i = 0; i < 16; i++)
    {
      int start = Utility.unsignedShort (buffer, sizePtr);
      int size = Utility.unsignedShort (buffer, sizePtr + 2);
      sizePtr += 4;

      if (start == 0 && size == 0)
        break;

      int kind = Utility.unsignedShort (buffer, kindPtr);
      kindPtr += 2;

      if (kind > 7)                 // entire file is probably just 6502 code
        return false;
    }

    return true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void sort ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("sort () not implemented in FilePascalCode");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append ("\n\n");
    text.append ("-------- File ---------\n");
    text.append (String.format ("File name ............. %s%n", getFileName ()));
    text.append (String.format ("File system type ...... %s%n", getFileSystemType ()));
    text.append (String.format ("Total segments ........ %d%n", segments.size ()));
    text.append (String.format ("Comment ............... %s%n", comment));

    return Utility.rtrim (text);
  }
}
