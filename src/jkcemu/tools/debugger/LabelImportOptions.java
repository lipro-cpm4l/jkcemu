/*
 * (c) 2015-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Optionen zum Importieren von Marken in den Debugger
 */

package jkcemu.tools.debugger;

import java.io.File;


public class LabelImportOptions
{
  public enum LabelSource { CLIPBOARD, FILE };


  private LabelSource labelSource;
  private File        file;
  private boolean     caseSensitive;
  private boolean     updBreakpointsOnly;
  private boolean     removeObsoleteLabels;


  public LabelImportOptions(
		LabelSource labelSource,
		File        file,
		boolean     caseSensitive,
		boolean     updBreakpointsOnly,
		boolean     removeObsoleteLabels )
  {
    this.labelSource          = labelSource;
    this.file                 = file;
    this.caseSensitive        = caseSensitive;
    this.updBreakpointsOnly   = updBreakpointsOnly;
    this.removeObsoleteLabels = removeObsoleteLabels;
  }


  public File getFile()
  {
    return this.file;
  }


  public boolean getCaseSensitive()
  {
    return this.caseSensitive;
  }


  public LabelSource getLabelSource()
  {
    return this.labelSource;
  }


  public boolean getRemoveObsoleteLabels()
  {
    return this.removeObsoleteLabels;
  }


  public boolean getUpdateBreakpointsOnly()
  {
    return this.updBreakpointsOnly;
  }


  public void setCaseSensitive( boolean state )
  {
    this.caseSensitive = state;
  }


  public void setFile( File file )
  {
    this.file = file;
  }


  public void setLabelSource( LabelSource labelSource )
  {
    this.labelSource = labelSource;
  }


  public void setRemoveObsoleteLabels( boolean state )
  {
    this.removeObsoleteLabels = state;
  }


  public void setUpdateBreakpointsOnly( boolean state )
  {
    this.updBreakpointsOnly = state;
  }
}
