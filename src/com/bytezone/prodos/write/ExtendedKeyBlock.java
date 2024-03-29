package com.bytezone.prodos.write;

import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
public class ExtendedKeyBlock
// -----------------------------------------------------------------------------------//
{
  private final ProdosDisk disk;
  private final int ptr;

  private MiniEntry dataFork;
  private MiniEntry resourceFork;

  enum ForkType
  {
    DATA, RESOURCE;
  }

  // ---------------------------------------------------------------------------------//
  public ExtendedKeyBlock (ProdosDisk disk, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    this.disk = disk;
    this.ptr = ptr;
  }

  // ---------------------------------------------------------------------------------//
  void addMiniEntry (ForkType type, FileEntry fileEntry)
  // ---------------------------------------------------------------------------------//
  {
    addMiniEntry (type, fileEntry.storageType, fileEntry.keyPtr, fileEntry.size,
        fileEntry.eof);
  }

  // ---------------------------------------------------------------------------------//
  void addMiniEntry (ForkType type, FileWriter fileWriter)
  // ---------------------------------------------------------------------------------//
  {
    addMiniEntry (type, fileWriter.storageType, fileWriter.keyPointer, fileWriter.blocksUsed,
        fileWriter.eof);
  }

  // ---------------------------------------------------------------------------------//
  private void addMiniEntry (ForkType type, byte storageType, int keyPointer, int blocksUsed,
      int eof)
  // ---------------------------------------------------------------------------------//
  {
    if (type == ForkType.DATA)
      dataFork = new MiniEntry (storageType, keyPointer, blocksUsed, eof);
    else
      resourceFork = new MiniEntry (storageType, keyPointer, blocksUsed, eof);
  }

  // ---------------------------------------------------------------------------------//
  void read (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    if (buffer[ptr] != 0)
      dataFork = new MiniEntry (buffer, 0);

    if (buffer[ptr + 0x100] != 0)
      resourceFork = new MiniEntry (buffer, 0x100);
  }

  // ---------------------------------------------------------------------------------//
  void write ()
  // ---------------------------------------------------------------------------------//
  {
    if (dataFork != null)                     // else zero buffer??
      dataFork.write (disk.getBuffer (), ptr);

    if (resourceFork != null)
      resourceFork.write (disk.getBuffer (), ptr + 0x100);
  }

  // ---------------------------------------------------------------------------------//
  class MiniEntry
  // ---------------------------------------------------------------------------------//
  {
    byte storageType;       // uses low nibble
    int keyPtr;
    int size;
    int eof;

    // -------------------------------------------------------------------------------//
    MiniEntry (byte[] buffer, int ptr)
    // -------------------------------------------------------------------------------//
    {
      read (buffer, ptr);
    }

    // -------------------------------------------------------------------------------//
    MiniEntry (byte storageType, int keyBlock, int blocksUsed, int eof)
    // -------------------------------------------------------------------------------//
    {
      this.storageType = storageType;
      this.keyPtr = keyBlock;
      this.size = blocksUsed;
      this.eof = eof;
    }

    // -------------------------------------------------------------------------------//
    void read (byte[] buffer, int ptr)
    // -------------------------------------------------------------------------------//
    {
      storageType = buffer[ptr];
      keyPtr = Utility.unsignedShort (buffer, ptr + 1);
      size = Utility.unsignedShort (buffer, ptr + 3);
      eof = Utility.unsignedTriple (buffer, ptr + 5);
    }

    // -------------------------------------------------------------------------------//
    void write (byte[] buffer, int ptr)
    // -------------------------------------------------------------------------------//
    {
      buffer[ptr] = storageType;
      Utility.writeShort (buffer, ptr + 1, keyPtr);
      Utility.writeShort (buffer, ptr + 3, size);
      Utility.writeTriple (buffer, ptr + 5, eof);
    }
  }
}
