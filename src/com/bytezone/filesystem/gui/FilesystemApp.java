package com.bytezone.filesystem.gui;

import java.util.prefs.Preferences;

import com.bytezone.appbase.AppBase;
import com.bytezone.xmit.gui.OutputTabPane;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;

// -----------------------------------------------------------------------------------//
public class FilesystemApp extends AppBase
// -----------------------------------------------------------------------------------//
{
  private final SplitPane splitPane = new SplitPane ();
  private final OutputTabPane outputTabPane = new OutputTabPane ("Output");

  // ---------------------------------------------------------------------------------//
  @Override
  public void start (Stage primaryStage) throws Exception
  // ---------------------------------------------------------------------------------//
  {
    super.start (primaryStage);

    xmitStageManager.setSplitPane (splitPane);      // this must happen after show()
  }

  // ---------------------------------------------------------------------------------//
  @Override
  protected Parent createContent ()
  // ---------------------------------------------------------------------------------//
  {
    return null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  protected Preferences getPreferences ()
  // ---------------------------------------------------------------------------------//
  {
    return Preferences.userNodeForPackage (this.getClass ());
  }

  // ---------------------------------------------------------------------------------//
  public static void main (String[] args)
  // ---------------------------------------------------------------------------------//
  {
    Application.launch (args);
  }
}
