package com.bytezone.filesystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.bytezone.filesystem.AppleBlock.BlockType;
import com.bytezone.filesystem.BlockReader.AddressType;

// -----------------------------------------------------------------------------------//
public abstract class AbstractFileSystem implements AppleFileSystem
// -----------------------------------------------------------------------------------//
{
  protected FileSystemFactory factory;          // never static!!

  protected final BlockReader blockReader;
  protected int totalCatalogBlocks;
  protected int freeBlocks;

  // If this file is a container (FS, folder, forked file, hybrid) then the children are
  // stored here
  protected List<AppleFile> files = new ArrayList<> ();
  protected List<AppleFileSystem> fileSystems = new ArrayList<> ();

  protected FileSystemType fileSystemType;
  protected String errorMessage = "";

  protected boolean partOfHybrid;     // this FS is one of two file systems on the disk

  // ---------------------------------------------------------------------------------//
  public AbstractFileSystem (BlockReader blockReader, FileSystemType fileSystemType)
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
  public boolean isHybrid ()
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
  boolean isValidBlockNo (int blockNo)
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.isValidBlockNo (blockNo);
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
  public AddressType getType ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.addressType;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlocksPerTrack ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.blocksPerTrack;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getBlockSize ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.bytesPerBlock;
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
    return blockReader.read (blocks);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void writeBlock (AppleBlock block, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    blockReader.write (block, buffer);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public void writeBlocks (List<AppleBlock> blocks, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    blockReader.write (blocks, buffer);
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
  public byte[] getDiskBuffer ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getDiskBuffer ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getDiskOffset ()
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getDiskOffset ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getDiskLength ()               // in bytes
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.getDiskLength ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public int getTotalBlocks ()              // in blocks
  // ---------------------------------------------------------------------------------//
  {
    return blockReader.totalBlocks;
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

  // ---------------------------------------------------------------------------------//
  protected AppleFileSystem addEmbeddedFileSystem (AppleFile file, int offset)
  // ---------------------------------------------------------------------------------//
  {
    byte[] buffer = file.read ();

    BlockReader blockReader =
        new BlockReader (file.getFileName (), buffer, offset, buffer.length - offset);

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
  public String getCatalog ()
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
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (String.format ("File name ............. %s%n", getFileName ()));
    text.append (String.format ("File system type ...... %s%n%n", fileSystemType));

    text.append (blockReader.toString ());
    text.append ("\n\n");

    text.append (String.format ("Catalog blocks ........ %d%n", totalCatalogBlocks));
    text.append (String.format ("Total file systems .... %d%n", fileSystems.size ()));
    text.append (String.format ("Total files ........... %d%n%n", files.size ()));

    return text.toString ();
  }
}
