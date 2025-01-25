package com.bytezone.filesystem;

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
  public byte[] read ()
  // ---------------------------------------------------------------------------------//
  {
    if (buffer == null)
      buffer = blockReader.read (this);

    return buffer;
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
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    String dos = trackNo >= 0 ? String.format ("(%02X/%02X)", trackNo, sectorNo) : "";

    text.append (String.format ("Block type ............ %s%n",
        (this instanceof BlockDos) ? "DOS" : "PRD"));
    text.append (
        String.format ("Address type .......... %s%n", fileSystem.getAddressType ()));
    text.append (String.format ("Block no .............. %04X %s%n", blockNo, dos));
    text.append (String.format ("Block Type ............ %s%n", blockType));
    text.append (String.format ("Block subtype ......... %s%n", blockSubType));
    text.append (String.format ("File name ............. %s%n",
        fileOwner == null ? "" : fileOwner.getFileName ()));

    return Utility.rtrim (text);
  }
}
