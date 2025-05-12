package com.bytezone.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.filesystem.BlockReader.AddressType;
import com.bytezone.utility.Utility;

// -----------------------------------------------------------------------------------//
abstract class AbstractFileSystem implements AppleFileSystem
// -----------------------------------------------------------------------------------//
{
  protected FileSystemFactory factory;          // never static!!

  protected final BlockReader blockReader;
  protected int totalCatalogBlocks;
  protected int freeBlocks;

  protected final List<AppleFile> files = new ArrayList<> ();
  protected final List<AppleFileSystem> fileSystems = new ArrayList<> ();

  protected final FileSystemType fileSystemType;
  protected String errorMessage = "";

  protected boolean partOfHybrid;     // this FS is one of two file systems on the disk
  private byte[] empty = new byte[1024];
  protected BitSet volumeBitMap;

  protected List<DiskHeader> diskHeaders = new ArrayList<> ();

  // ---------------------------------------------------------------------------------//
  AbstractFileSystem (BlockReader blockReader, FileSystemType fileSystemType)
  // ---------------------------------------------------------------------------------//
  {
    this.blockReader = Objects.requireNonNull (blockReader);
    this.fileSystemType = fileSystemType;
  }

  // ---------------------------------------------------------------------------------//
  int getTotalCatalogBlocks ()
  // ---------------------------------------------------------------------------------//
  {
    return totalCatalogBlocks;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isHybridComponent ()
  // ---------------------------------------------------------------------------------//
  {
    return partOfHybrid;
  }

  // ---------------------------------------------------------------------------------//
  void setTotalCatalogBlocks (int total)
  // ---------------------------------------------------------------------------------//
  {
    totalCatalogBlocks = total;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock allocate ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "allocate() not implemented in " + fileSystemType);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void sort ()
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException (
        "sort () not implemented in " + fileSystemType);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isValidAddress (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.isValidAddress (blockNo);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isValidAddress (int trackNo, int sectorNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.isValidAddress (trackNo, sectorNo);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public BlockReader getBlockReader ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public FileSystemType getFileSystemType ()
  // ---------------------------------------------------------------------------------//
  {
    return fileSystemType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AddressType getAddressType ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getAddressType ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlocksPerTrack ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getBlocksPerTrack ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlockSize ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getBlockSize ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock getBlock (int blockNo, BlockType blockType)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getBlock (this, blockNo, blockType);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock getSector (int track, int sector, BlockType blockType)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getSector (this, track, sector, blockType);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock getSector (byte[] buffer, int offset, BlockType blockType)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getSector (this, buffer, offset, blockType);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public boolean isFree (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    return volumeBitMap == null ? false : volumeBitMap.get (block.getBlockNo ());
  }

  // cleans all blocks marked as free
  // ---------------------------------------------------------------------------------//
  @Override
  public void cleanDisk ()
  // ---------------------------------------------------------------------------------//
  {
    for (int i = 0; i < blockReader.getTotalBlocks (); i++)
    {
      AppleBlock block = blockReader.getBlock (this, i);
      if (block.isFree () && block.getBlockType () != BlockType.EMPTY)
      {
        byte[] buffer = block.getBuffer ();
        System.arraycopy (empty, 0, buffer, 0, blockReader.getBlockSize ());
        block.markDirty ();
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock getBlock (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getBlock (this, blockNo);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock getSector (int track, int sector)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getSector (this, track, sector);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public AppleBlock getSector (byte[] buffer, int offset)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getSector (this, buffer, offset);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] readBlock (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.read (block);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public byte[] readBlocks (List<AppleBlock> blocks)
  // ---------------------------------------------------------------------------------//
  {
    int blockSize = blockReader.getBlockSize ();
    byte[] buffer = new byte[blocks.size () * blockSize];

    int count = 0;
    for (AppleBlock block : blocks)
    {
      byte[] blockBuffer = block.getBlockNo () == 0 ? empty : blockReader.read (block);
      System.arraycopy (blockBuffer, 0, buffer, blockSize * count++, blockSize);
    }

    return buffer;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void writeBlock (AppleBlock block)
  // ---------------------------------------------------------------------------------//
  {
    blockReader.write (block);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void writeBlocks (List<AppleBlock> blocks, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    int blockSize = blockReader.getBlockSize ();

    int count = 0;
    for (AppleBlock block : blocks)
      System.arraycopy (buffer, blockSize * count++, blockReader.read (block), 0,
          blockSize);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void markDirty (AppleBlock dirtyBlock)
  // ---------------------------------------------------------------------------------//
  {
    blockReader.markDirty (dirtyBlock);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getFileName ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getName ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFileSystem (AppleFileSystem fileSystem)
  // ---------------------------------------------------------------------------------//
  {
    fileSystems.add (fileSystem);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFileSystem> getFileSystems ()
  // ---------------------------------------------------------------------------------//
  {
    return new ArrayList<AppleFileSystem> (fileSystems);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void addFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    files.add (file);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public List<AppleFile> getFiles ()
  // ---------------------------------------------------------------------------------//
  {
    return new ArrayList<AppleFile> (files);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Optional<AppleFile> getFile (String fileName)
  // ---------------------------------------------------------------------------------//
  {
    for (AppleFile file : files)
      if (file.getFileName ().equals (fileName))
        return Optional.of (file);

    return Optional.empty ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void putFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    System.out.println ("putFile() not implemented in " + fileSystemType);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public Buffer getDiskBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getDiskBuffer ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()              // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getTotalBlocks ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalFreeBlocks ()         // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return freeBlocks;
  }

  // ---------------------------------------------------------------------------------//
  protected FileSystemFactory getFactory ()
  // ---------------------------------------------------------------------------------//
  {
    if (factory == null)
      factory = new FileSystemFactory ();

    return factory;
  }

  // Called when an AppleFile contains a FileSystem (LIB, PAR, FileZip etc)
  // ---------------------------------------------------------------------------------//
  protected AppleFileSystem addEmbeddedFileSystem (AppleFile file, int offset)
  // ---------------------------------------------------------------------------------//
  {
    Buffer dataRecord = file.getRawFileBuffer ();

    BlockReader blockReader = new BlockReader (file.getFileName (), dataRecord.data (),
        dataRecord.offset () + offset, dataRecord.length () - offset);

    AppleFileSystem fs = getFactory ().getFileSystem (blockReader);
    ((AbstractAppleFile) file).embedFileSystem (fs);            // embedded FS

    return fs;
  }

  // ---------------------------------------------------------------------------------//
  protected AppleFileSystem addFileSystem (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    return addFileSystem (new BlockReader (name, buffer, 0, buffer.length));
  }

  // ---------------------------------------------------------------------------------//
  protected AppleFileSystem addFileSystem (BlockReader blockReader)
  // ---------------------------------------------------------------------------------//
  {
    AppleFileSystem fs = getFactory ().getFileSystem (blockReader);

    if (fs != null)
      addFileSystem (fs);

    return fs;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return "";
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getCatalogText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("Default catalog for : %s%n%n", getFileName ()));

    if (getFiles ().size () > 0)
    {
      text.append ("Files:\n");
      for (AppleFile file : getFiles ())
      {
        text.append (file.getCatalogLine ());
        text.append ("\n");
      }
    }

    if (getFileSystems ().size () > 0)
    {
      text.append ("File systems:\n");
      for (AppleFileSystem fileSystem : getFileSystems ())
        text.append (String.format ("%-5s %s%n", fileSystem.getFileSystemType (),
            fileSystem.getFileName ()));
    }

    if (!errorMessage.isEmpty ())
      text.append ("\n" + errorMessage);

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getErrorMessage ()
  // ---------------------------------------------------------------------------------//
  {
    return errorMessage;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void setErrorMessage (String errorMessage)
  // ---------------------------------------------------------------------------------//
  {
    this.errorMessage = errorMessage;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void create (String fileName)
  // ---------------------------------------------------------------------------------//
  {
    File file = new File (fileName);
    if (file.exists ())
      System.out.printf ("%s already exists%n", fileName);
    else
    {
      blockReader.clean ();         // move dirty blocks back to disk buffer 
      byte[] diskBuffer = blockReader.getDiskBuffer ().data ();

      try
      {
        if (file.createNewFile ())
        {
          OutputStream Stream = new FileOutputStream (fileName);
          Stream.write (diskBuffer);
          Stream.close ();
        }
      }
      catch (IOException e)
      {
        e.printStackTrace ();
      }
    }
  }

  // debugging
  // ---------------------------------------------------------------------------------//
  private void showUsed (BitSet bitMap, int size)
  // ---------------------------------------------------------------------------------//
  {
    int count = 0;
    for (int i = 0; i < size; i++)
      if (!bitMap.get (i))        // off = used
      {
        count++;
        System.out.printf ("%04X  %<4d%n", i);
      }

    System.out.printf ("total %d%n", count);
  }

  // debugging
  // ---------------------------------------------------------------------------------//
  protected void dump (BitSet bitMap, int size)
  // ---------------------------------------------------------------------------------//
  {
    for (int i = 0; i < size; i++)
    {
      if (i % 8 == 0)
        System.out.println ();
      System.out.printf ("%s ", bitMap.get (i) ? "1" : "0");
    }
    System.out.println ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void setDiskHeaders (List<DiskHeader> diskHeaders)
  // ---------------------------------------------------------------------------------//
  {
    this.diskHeaders = diskHeaders;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    String msg = files.size () == 1 && files.get (0).hasEmbeddedFileSystem ()
        ? msg = " (embedded file system)" : "";

    StringBuilder text = new StringBuilder ();

    for (DiskHeader diskHeader : diskHeaders)
    {
      text.append ("----- Disk Header -----\n");
      text.append (diskHeader);
      text.append ("\n\n");
    }

    text.append ("----- File System -----\n");
    Utility.formatMeta (text, "File name", getFileName ());
    Utility.formatMeta (text, "File system type", fileSystemType.toString ());
    text.append ("\n");

    Utility.formatMeta (text, "Catalog blocks", 2, totalCatalogBlocks);
    Utility.formatMeta (text, "Total file systems", 2, fileSystems.size ());
    Utility.formatMeta (text, "Total files", 4, files.size (), msg);
    text.append ("\n");

    text.append ("---- Block Reader -----\n");
    text.append (blockReader.toString ());
    text.append ("\n\n");

    return text.toString ();
  }
}
