package com.bytezone.filesystem;

import static com.bytezone.utility.Utility.formatText;

import java.util.Objects;

import com.bytezone.filesystem.BlockReader.AddressType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
abstract class AbstractBlock implements AppleBlock
// -----------------------------------------------------------------------------------//
{
  protected final AppleFileSystem fileSystem;
  protected final BlockReader blockReader;

  protected AppleFile fileOwner;
  protected BlockType blockType;
  protected String blockSubType = "";

  protected final int blockNo;
  protected final int trackNo;
  protected final int sectorNo;

  protected byte[] buffer;
  protected boolean dirty;

  protected Object userData;

  // ---------------------------------------------------------------------------------//
  AbstractBlock (AppleFileSystem fileSystem, int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystem = Objects.requireNonNull (fileSystem, "File System is null");
    this.blockReader = fileSystem.getBlockReader ();
    this.blockNo = blockNo;

    int blocksPerTrack = fileSystem.getBlocksPerTrack ();
    if (blocksPerTrack > 0)
    {
      trackNo = blockNo / blocksPerTrack;
      sectorNo = blockNo % blocksPerTrack;
    }
    else
    {
      trackNo = -1;
      sectorNo = -1;
    }
  }

  // ---------------------------------------------------------------------------------//
  AbstractBlock (AppleFileSystem fileSystem, int trackNo, int sectorNo)
  // ---------------------------------------------------------------------------------//
  {
    this.fileSystem = Objects.requireNonNull (fileSystem, "File System is null");
    this.blockReader = fileSystem.getBlockReader ();
    this.blockNo = fileSystem.getBlocksPerTrack () * trackNo + sectorNo;

    this.trackNo = trackNo;
    this.sectorNo = sectorNo;

    assert fileSystem.getAddressType () == AddressType.SECTOR;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleFileSystem getFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystem;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void setBlockType (BlockType blockType)
  // ---------------------------------------------------------------------------------//
  {
    this.blockType = blockType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public BlockType getBlockType ()
  // ---------------------------------------------------------------------------------//
  {
    return blockType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void setBlockSubType (String blockSubType)
  // ---------------------------------------------------------------------------------//
  {
    this.blockSubType = blockSubType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getBlockSubType ()
  // ---------------------------------------------------------------------------------//
  {
    return blockSubType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void setFileOwner (AppleFile appleFile)
  // ---------------------------------------------------------------------------------//
  {
    this.fileOwner = appleFile;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleFile getFileOwner ()
  // ---------------------------------------------------------------------------------//
  {
    return this.fileOwner;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlockNo ()
  // ---------------------------------------------------------------------------------//
  {
    return blockNo;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTrackNo ()
  // ---------------------------------------------------------------------------------//
  {
    return trackNo;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getSectorNo ()
  // ---------------------------------------------------------------------------------//
  {
    return sectorNo;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFree ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystem.isFree (this);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isDirty ()
  // ---------------------------------------------------------------------------------//
  {
    return dirty;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void markDirty ()
  // ---------------------------------------------------------------------------------//
  {
    dirty = true;
    fileSystem.markDirty (this);        // add to the laundry basket
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void markClean ()
  // ---------------------------------------------------------------------------------//
  {
    dirty = false;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] getBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    if (buffer == null)
      buffer = blockReader.read (this);

    return buffer;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void setBuffer (byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    this.buffer = buffer;         // still needs to be copied back to the disk buffer
    dirty = true;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void setUserData (Object userData)
  // ---------------------------------------------------------------------------------//
  {
    this.userData = userData;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Object getUserData ()
  // ---------------------------------------------------------------------------------//
  {
    return userData;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void dump ()
  // ---------------------------------------------------------------------------------//
  {
    System.out.println (Utility.format (getBuffer ()));
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    String dos = trackNo >= 0 ? String.format ("(%02X/%02X)", trackNo, sectorNo) : "";

    formatText (text, "Block type", (this instanceof BlockDos) ? "DOS" : "PRD");
    formatText (text, "Address type", fileSystem.getAddressType ().toString ());
    formatText (text, "Block no", 4, blockNo, dos);
    formatText (text, "Block Type", blockType.toString ());
    formatText (text, "Block subtype", blockSubType);
    formatText (text, "File name", fileOwner == null ? "" : fileOwner.getFileName ());

    return Utility.rtrim (text);
  }
}
