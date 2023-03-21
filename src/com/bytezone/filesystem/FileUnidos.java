package com.bytezone.filesystem;

public class FileUnidos extends AbstractAppleFile
{

  // ---------------------------------------------------------------------------------//
  FileUnidos (FsUnidos fs, String name, byte[] buffer, int offset, int length)
  // ---------------------------------------------------------------------------------//
  {
    super (fs);

    //    try
    //    {
    //      BlockReader blockReader1 = new BlockReader ("DISK 1", buffer, offset, UNIDOS_SIZE);
    //      blockReader1.setParameters (256, AddressType.SECTOR, 0, 32);
    //      FsDos fs1 = new FsDos (blockReader1);
    //
    //      if (fs1 != null && fs1.getTotalCatalogBlocks () > 0)
    //      {
    //        BlockReader blockReader2 =
    //            new BlockReader ("DISK 2", buffer, offset + UNIDOS_SIZE, UNIDOS_SIZE);
    //        blockReader2.setParameters (256, AddressType.SECTOR, 0, 32);
    //        FsDos fs2 = new FsDos (blockReader2);
    //
    //        if (fs2 != null && fs2.getTotalCatalogBlocks () > 0)
    //        {
    //          addFile (fs1);
    //          addFile (fs2);
    //        }
    //      }
    //    }
    //    catch (FileFormatException e)
    //    {
    //      if (debug)
    //        System.out.println (e);
    //    }
  }
}
