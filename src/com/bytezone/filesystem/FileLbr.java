package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class FileLbr extends AbstractAppleFile
// -----------------------------------------------------------------------------------//
{
  int status;
  String extension;
  int index;
  int length;
  int crc;
  int pad;
  int creationDate;
  int creationTime;
  int modifiedDate;
  int modifiedTime;

  String alternateName;
  String alternateExtension;
  String extraText;

  String fileName;
  String fileTypeText;

  // ---------------------------------------------------------------------------------//
  FileLbr (FsLbr fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    status = buffer[ptr] & 0xFF;
    fileName = Utility.string (buffer, ptr + 1, 8);
    extension = Utility.string (buffer, ptr + 9, 3);
    index = Utility.unsignedShort (buffer, ptr + 12);
    length = Utility.unsignedShort (buffer, ptr + 14);
    crc = Utility.unsignedShort (buffer, ptr + 16);
    creationDate = Utility.unsignedShort (buffer, ptr + 18);
    modifiedDate = Utility.unsignedShort (buffer, ptr + 20);
    creationTime = Utility.unsignedShort (buffer, ptr + 22);
    modifiedTime = Utility.unsignedShort (buffer, ptr + 24);
    pad = Utility.unsignedShort (buffer, ptr + 26);

    fileTypeText = extension;

    if (index == 0)         // directory entry
      return;

    for (int blockNo = index; blockNo < index + length; blockNo++)
    {
      AppleBlock block = fs.getBlock (blockNo, BlockType.FILE_DATA);
      if (block == null)
      {
        throw new FileFormatException ("Invalid FileLbr data block");
        //        System.out.println ("null block in FileLBR " + fileName);
        //        break;
      }
      dataBlocks.add (block);
      block.setFileOwner (this);
    }

    if (extension.charAt (1) == 'Z')
      check (buffer, 'Z', (byte) 0xFE, 2);          // crunch

    if (extension.charAt (1) == 'Z')
      check (buffer, 'Z', (byte) 0xDF, 2);

    if (extension.charAt (1) == 'Y')
      check (buffer, 'Y', (byte) 0xFD, 2);

    if (extension.charAt (1) == 'Q')
      check (buffer, 'Q', (byte) 0xD8, 4);

    if (extension.charAt (1) == 'Q')
      check (buffer, 'Q', (byte) 0xFF, 4);          // squeeze
  }

  // ---------------------------------------------------------------------------------//
  private void check (byte[] buffer, char c, byte b, int nameStart)
  // ---------------------------------------------------------------------------------//
  {
    if (dataBlocks.size () == 0 || extension.charAt (1) != c)
      return;

    buffer = dataBlocks.get (0).getBuffer ();

    if (buffer[0] != 0x76 || buffer[1] != b)
      return;

    String name = Utility.getCString (buffer, nameStart);
    if (name.isBlank ())
      return;

    int pos1 = name.indexOf ('.');
    int pos2 = name.indexOf ('[', pos1);

    if (pos1 > 0)
    {
      alternateName = name.substring (0, pos1);
      if (pos2 > 0)
      {
        alternateExtension = name.substring (pos1 + 1, pos2);
        extraText = name.substring (pos2);
      }
      else
      {
        alternateExtension = name.substring (pos1 + 1);
        extraText = "";
      }

      fileTypeText = alternateExtension;
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
    return 0;
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
  @Override
  public int getFileLength ()
  // ---------------------------------------------------------------------------------//
  {
    return length * parentFileSystem.getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return length;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    return String.format ("%-8s %3s  %3d  %3d  %-8s %-3s", fileName, extension, index,
        length, alternateName, extraText);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    formatText (text, "Status", 2, status,
        status == 0 ? "Active" : status == 0xFF ? "Unused" : "Deleted");
    formatText (text, "Extension ............. %s%n", extension);
    formatText (text, "Aternate name ......... %s%n",
        alternateName == null ? "" : alternateName);
    formatText (text, "Aternate extension .... %s%n",
        alternateExtension == null ? "" : alternateExtension);
    formatText (text, "Extra text", extraText == null ? "" : extraText);
    formatText (text, "Index", 4, index);
    formatText (text, "Length", 4, length);
    formatText (text, "Pad", 2, pad);
    formatText (text, "Creation date", 4, creationDate);
    formatText (text, "Creation time", 4, creationTime);
    formatText (text, "Modified date", 4, modifiedDate);
    formatText (text, "Modified time", 4, modifiedTime);
    formatText (text, "CRC", 4, crc);

    return Utility.rtrim (text);
  }
}
