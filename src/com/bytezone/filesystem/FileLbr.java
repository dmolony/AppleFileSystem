package com.bytezone.filesystem;

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
  String squeezeName;
  String squeezeRest;

  // ---------------------------------------------------------------------------------//
  FileLbr (FsLbr fs, byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    status = buffer[ptr] & 0xFF;
    fileName = new String (buffer, ptr + 1, 8);
    extension = new String (buffer, ptr + 9, 3);
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
      dataBlocks.add (block);
    }

    if (extension.charAt (1) == 'Z')
      check (buffer, 'Z', (byte) 0xFE, 2);

    if (extension.charAt (1) == 'Y')
      check (buffer, 'Y', (byte) 0xFD, 2);

    if (extension.charAt (1) == 'Z')
      check (buffer, 'Z', (byte) 0xDF, 2);

    if (extension.charAt (1) == 'Q')
      check (buffer, 'Q', (byte) 0xD8, 4);

    if (extension.charAt (1) == 'Q')
      check (buffer, 'Q', (byte) 0xFF, 4);
  }

  // ---------------------------------------------------------------------------------//
  private void check (byte[] buffer, char c, byte b, int nameStart)
  // ---------------------------------------------------------------------------------//
  {
    if (dataBlocks.size () > 0 && extension.charAt (1) == c)
    {
      buffer = dataBlocks.get (0).read ();
      if (buffer[0] == 0x76 && buffer[1] == b)
      {
        String name = Utility.getCString (buffer, nameStart);
        if (!name.isBlank ())
        {
          int pos = name.indexOf ('.');
          int pos2 = name.indexOf ('[', pos);
          if (pos > 0)
          {
            squeezeName = name.substring (0, pos);
            if (pos2 > 0)
            {
              extension = name.substring (pos + 1, pos2);
              squeezeRest = name.substring (pos2);
            }
            else
            {
              extension = name.substring (pos + 1);
              squeezeRest = "";
            }
            fileTypeText = extension;
          }
        }
      }
    }
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
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    text.append (String.format ("Status ................ %d%n", status));
    text.append (String.format ("Extension ............. %s%n", extension));
    text.append (String.format ("Squeeze name .......... %s%n", squeezeName));
    text.append (String.format ("Squeeze rest .......... %s%n", squeezeRest));
    text.append (String.format ("Index ................. %,d%n", index));
    text.append (String.format ("Length ................ %,d%n", length));
    text.append (String.format ("Creation date ......... %,d%n", creationDate));
    text.append (String.format ("Creation time ......... %,d%n", creationTime));
    text.append (String.format ("Modified date ......... %,d%n", modifiedDate));
    text.append (String.format ("Modified time ......... %,d%n", modifiedTime));
    text.append (String.format ("CRC ................... %,d%n", crc));

    return Utility.rtrim (text);
  }
}
