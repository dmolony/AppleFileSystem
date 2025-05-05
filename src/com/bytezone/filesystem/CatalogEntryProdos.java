package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatMeta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
class CatalogEntryProdos extends CatalogEntry
// -----------------------------------------------------------------------------------//
{
  private static Locale US = Locale.US;                 // to force 3 character months
  private static final DateTimeFormatter df =
      DateTimeFormatter.ofPattern ("d-LLL-yy", US);
  private static final DateTimeFormatter tf = DateTimeFormatter.ofPattern ("H:mm");
  private static final String NO_DATE = "<NO DATE>";

  int fileType;
  boolean isLocked;

  int storageType;
  int keyPtr;                 // blockNumber 
  int blocksUsed;
  int eof;
  int auxType;
  int headerPtr;              // block number of the directory header

  int version;
  int minVersion;
  int access;

  LocalDateTime created;
  LocalDateTime modified;
  String dateCreated, timeCreated;
  String dateModified, timeModified;

  String fileTypeText;
  String storageTypeText;
  List<String> blockChainText;

  // ---------------------------------------------------------------------------------//
  CatalogEntryProdos (AppleBlock catalogBlock, int slot)
  // ---------------------------------------------------------------------------------//
  {
    super (catalogBlock, slot);

    read ();

    buildBlockChain ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void read ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = 4 + slot * ProdosConstants.ENTRY_SIZE;

    byte[] buffer = catalogBlock.getBuffer ();
    storageType = (buffer[ptr] & 0xF0) >>> 4;         // 0=deleted

    int nameLength = buffer[ptr] & 0x0F;
    fileName = nameLength > 0 ? Utility.string (buffer, ptr + 1, nameLength) : "";

    fileType = buffer[ptr + 0x10] & 0xFF;
    keyPtr = Utility.unsignedShort (buffer, ptr + 0x11);
    blocksUsed = Utility.unsignedShort (buffer, ptr + 0x13);
    eof = Utility.unsignedTriple (buffer, ptr + 0x15);

    created = Utility.getAppleDate (buffer, ptr + 0x18);
    version = buffer[ptr + 0x1C] & 0xFF;
    minVersion = buffer[ptr + 0x1D] & 0xFF;
    access = buffer[ptr + 0x1E] & 0xFF;

    auxType = Utility.unsignedShort (buffer, ptr + 0x1F);
    modified = Utility.getAppleDate (buffer, ptr + 0x21);
    headerPtr = Utility.unsignedShort (buffer, ptr + 0x25);

    isLocked = (access & 0xC2) == 0;

    fileTypeText = ProdosConstants.fileTypes[fileType];
    storageTypeText = ProdosConstants.storageTypes[storageType];

    dateCreated = created == null ? NO_DATE : created.format (df);
    timeCreated = created == null ? "" : created.format (tf);

    dateModified = modified == null ? NO_DATE : modified.format (df);
    timeModified = modified == null ? "" : modified.format (tf);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void write ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = 4 + slot * ProdosConstants.ENTRY_SIZE;

    byte[] buffer = catalogBlock.getBuffer ();

    int nameLength = fileName.length ();
    buffer[ptr] = (byte) ((storageType << 4) | (nameLength & 0x0F));

    int namePtr = ptr + 1;
    for (int i = 0; i < nameLength; i++)
      buffer[namePtr++] = (byte) fileName.charAt (i);

    buffer[ptr + 0x10] = (byte) fileType;
    Utility.writeShort (buffer, ptr + 0x11, keyPtr);
    Utility.writeShort (buffer, ptr + 0x13, blocksUsed);
    Utility.writeTriple (buffer, ptr + 0x15, eof);

    Utility.putAppleDate (buffer, ptr + 0x18, created);
    buffer[ptr + 0x1C] = (byte) version;
    buffer[ptr + 0x1D] = (byte) minVersion;
    buffer[ptr + 0x1E] = (byte) access;

    Utility.writeShort (buffer, ptr + 0x1F, auxType);
    Utility.putAppleDate (buffer, ptr + 0x21, modified);
    Utility.writeShort (buffer, ptr + 0x25, headerPtr);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  void delete ()
  // ---------------------------------------------------------------------------------//
  {
    int ptr = 4 + slot * ProdosConstants.ENTRY_SIZE;

    // mark this file's entry as deleted
    byte[] buffer = catalogBlock.getBuffer ();
    buffer[ptr] = 0;              // deleted
    catalogBlock.markDirty ();

    // reduce total files in header block
    AppleFileSystem fs = catalogBlock.getFileSystem ();
    AppleBlock headerBlock = fs.getBlock (headerPtr);

    buffer = headerBlock.getBuffer ();
    int fileCount = Utility.unsignedShort (buffer, 0x25);
    assert fileCount > 0;

    Utility.writeShort (buffer, 0x25, fileCount - 1);
    headerBlock.markDirty ();
  }

  // ---------------------------------------------------------------------------------//
  private void buildBlockChain ()
  // ---------------------------------------------------------------------------------//
  {
    blockChainText = new ArrayList<> ();

    // build chain of catalog blocks
    int prevBlock = Utility.unsignedShort (buffer, 0);
    FsProdos fs = (FsProdos) catalogBlock.getFileSystem ();
    StringBuilder chain = new StringBuilder ();

    while (prevBlock != 0)
    {
      chain.append (String.format ("--> %04X ", prevBlock));
      byte[] prevBuffer = fs.getBlock (prevBlock).getBuffer ();
      prevBlock = Utility.unsignedShort (prevBuffer, 0);
    }

    int max = 36;
    while (chain.length () > max)
    {
      blockChainText.add (chain.substring (0, max));
      chain.delete (0, max);
    }

    blockChainText.add (chain.toString ());
  }

  // ---------------------------------------------------------------------------------//
  private String dateBytes (int offset)
  // ---------------------------------------------------------------------------------//
  {
    int ptr = 4 + slot * ProdosConstants.ENTRY_SIZE + offset;
    StringBuilder text = new StringBuilder ();

    for (int i = 0; i < 4; i++)
      text.append (String.format ("%02X ", buffer[ptr + i]));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    formatMeta (text, "Storage type", 2, storageType, storageTypeText);
    formatMeta (text, "File type", 2, fileType, fileTypeText);

    formatMeta (text, "Block", 4, catalogBlock.getBlockNo (), blockChainText.get (0));

    if (blockChainText.size () > 1)
      for (int i = 1; i < blockChainText.size (); i++)
        text.append (String.format ("%40s %s%n", "", blockChainText.get (i)));

    formatMeta (text, "Key ptr", 4, keyPtr);
    formatMeta (text, "Blocks used", 4, blocksUsed);
    formatMeta (text, "EOF", 6, eof, eof == 0 ? "<-- zero" : "");

    formatMeta (text, "Created", dateBytes (0x18), dateCreated, timeCreated);
    formatMeta (text, "Modified", dateBytes (0x21), dateModified, timeModified);

    formatMeta (text, "Version", 2, version);
    formatMeta (text, "Min version", 2, minVersion);

    formatMeta (text, "Access", 2, access, Utility.getAccessText (access));
    formatMeta (text, "Auxtype", 4, auxType);
    formatMeta (text, "VDH/SDH ptr", 4, headerPtr);

    return Utility.rtrim (text);
  }
}
