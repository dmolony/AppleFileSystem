package com.bytezone.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.filesystem.BlockReader.AddressType;

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
    throw new UnsupportedOperationException ("allocate() not implemented");
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
      System.arraycopy (blockReader.read (block), 0, buffer, blockSize * count++,
          blockSize);

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
    return fileSystems;
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
    return files;
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
    System.out.println ("AbstractFileSystem.putFile() not written yet");
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void deleteFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    throw new UnsupportedOperationException ("deleteFile() not implemented");
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
  public int getFreeBlocks ()              // in blocks
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

  // Called when an AppleFile should contain a FileSystem (LIB, PAR, FileZip etc)
  // ---------------------------------------------------------------------------------//
  protected AppleFileSystem addEmbeddedFileSystem (AppleFile file, int offset)
  // ---------------------------------------------------------------------------------//
  {
    Buffer dataRecord = file.getFileBuffer ();

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
      blockReader.clean ();

      try
      {
        file.createNewFile ();

        OutputStream Stream = new FileOutputStream (fileName);
        Stream.write (blockReader.getDiskBuffer ().data ());
        Stream.close ();
      }
      catch (IOException e)
      {
        e.printStackTrace ();
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    String msg = files.size () == 1 && files.get (0).hasEmbeddedFileSystem ()
        ? msg = " (embedded file system)" : "";

    StringBuilder text = new StringBuilder ();

    text.append ("----- File System -----\n");
    text.append (String.format ("File name ............. %s%n", getFileName ()));
    text.append (String.format ("File system type ...... %s%n%n", fileSystemType));

    text.append (String.format ("Catalog blocks ........ %d%n", totalCatalogBlocks));
    text.append (String.format ("Total file systems .... %d%n", fileSystems.size ()));
    text.append (String.format ("Total files ........... %d%s%n%n", files.size (), msg));

    text.append ("---- Block Reader -----\n");
    text.append (blockReader.toString ());
    text.append ("\n\n");

    return text.toString ();
  }
}
