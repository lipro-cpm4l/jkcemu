/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Optionen zum Importieren von Marken in den Debugger
 */

package jkcemu.tools.debugger;

import java.io.File;
import java.lang.*;


public class LabelImportOptions
{
  public enum LabelSource { CLIPBOARD, FILE };


  private LabelSource labelSource;
  private File        file;
  private boolean     suppressRecreateRemovedLabels;
  private boolean     removeObsoleteLabels;
  private boolean     caseSensitive;


  public LabelImportOptions(
		LabelSource labelSource,
		File        file,
		boolean     suppressRecreateRemovedLabels,
		boolean     removeObsoleteLabels,
		boolean     caseSensitive )
  {
    this.labelSource                   = labelSource;
    this.file                          = file;
    this.suppressRecreateRemovedLabels = suppressRecreateRemovedLabels;
    this.removeObsoleteLabels          = removeObsoleteLabels;
    this.caseSensitive                 = caseSensitive;
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


  public boolean getSuppressRecreateRemovedLabels()
  {
    return this.suppressRecreateRemovedLabels;
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


  public void setSuppressRecreateRemovedLabels( boolean state )
  {
    this.suppressRecreateRemovedLabels = state;
  }
}

