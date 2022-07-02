package com.bytezone.prodos.write;

public class FileAlreadyExistsException extends Exception
{
  public FileAlreadyExistsException (String message)
  {
    super (message);
  }
}
