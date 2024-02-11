package com.bytezone.utility;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// -----------------------------------------------------------------------------------//
public class DateTime
// -----------------------------------------------------------------------------------//
{
  // use Locale.US to force 3 character months
  private static final DateTimeFormatter dtf =
      DateTimeFormatter.ofPattern ("dd-MMM-yy HH:mm", Locale.US);

  private final int second;
  private final int minute;
  private final int hour;
  private final int year;
  private final int day;
  private final int month;
  private final int weekDay;

  // ---------------------------------------------------------------------------------//
  public DateTime (byte[] buffer, int ptr)
  // ---------------------------------------------------------------------------------//
  {
    second = buffer[ptr++] & 0xFF;
    minute = buffer[ptr++] & 0xFF;
    hour = buffer[ptr++] & 0xFF;
    year = buffer[ptr++] & 0xFF;
    day = buffer[ptr++] & 0xFF;
    month = buffer[ptr++] & 0xFF;
    ptr++;                              // empty
    weekDay = buffer[ptr] & 0xFF;
  }

  // ---------------------------------------------------------------------------------//
  public String format ()
  // ---------------------------------------------------------------------------------//
  {
    LocalDateTime dateTime = getLocalDateTime ();
    return dateTime == null ? "" : getLocalDateTime ().format (dtf);
  }

  // ---------------------------------------------------------------------------------//
  public LocalDateTime getLocalDateTime ()
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      int adjustedYear = year + (year > 70 ? 1900 : 2000);
      return LocalDateTime.of (adjustedYear, month + 1, day + 1, hour, minute);
    }
    catch (DateTimeException e)
    {
      return null;
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    return "DateTime [second=" + second + ", minute=" + minute + ", hour=" + hour
        + ", year=" + year + ", day=" + day + ", month=" + month + ", weekDay=" + weekDay
        + "]";
  }
}