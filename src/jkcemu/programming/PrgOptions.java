/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Optionen fuer den Compiler/Assembler
 */

package jkcemu.programming;

import java.io.File;
import java.lang.*;
import java.util.Properties;
import jkcemu.base.EmuThread;
import jkcemu.programming.assembler.Z80Assembler;
import jkcemu.programming.basic.BasicOptions;


public class PrgOptions
{
  private Z80Assembler.Syntax asmSyntax;
  private boolean             allowUndocInst;
  private boolean             labelsCaseSensitive;
  private boolean             printLabels;
  private boolean             codeToEmu;
  private boolean             codeToSecondSys;
  private boolean             codeToFile;
  private File                codeFile;
  private boolean             forceRun;
  private boolean             labelsToDebugger;
  private boolean             labelsToReass;
  private boolean             formatSource;
  private boolean             warnNonAsciiChars;


  public PrgOptions()
  {
    this.asmSyntax           = Z80Assembler.Syntax.ALL;
    this.allowUndocInst      = false;
    this.labelsCaseSensitive = false;
    this.printLabels         = false;
    this.codeToEmu           = false;
    this.codeToSecondSys     = false;
    this.codeToFile          = false;
    this.codeFile            = null;
    this.forceRun            = false;
    this.labelsToDebugger    = false;
    this.labelsToReass       = false;
    this.formatSource        = false;
    this.warnNonAsciiChars   = true;
  }


  public boolean getAllowUndocInst()
  {
    return this.allowUndocInst;
  }


  public Z80Assembler.Syntax getAsmSyntax()
  {
    return this.asmSyntax;
  }


  public File getCodeFile()
  {
    return this.codeFile;
  }


  public boolean getCodeToEmu()
  {
    return this.codeToEmu;
  }


  public boolean getCodeToFile()
  {
    return this.codeToFile;
  }


  public boolean getCodeToSecondSystem()
  {
    return this.codeToSecondSys;
  }


  public boolean getCreateCode()
  {
    return this.codeToEmu || this.codeToFile || this.forceRun;
  }


  public boolean getForceRun()
  {
    return this.forceRun;
  }


  public boolean getFormatSource()
  {
    return this.formatSource;
  }


  public boolean getLabelsCaseSensitive()
  {
    return this.labelsCaseSensitive;
  }


  public boolean getLabelsToDebugger()
  {
    return this.labelsToDebugger;
  }


  public boolean getLabelsToReassembler()
  {
    return this.labelsToReass;
  }


  public boolean getPrintLabels()
  {
    return this.printLabels;
  }


  public static PrgOptions getPrgOptions( Properties props )
  {
    PrgOptions options = BasicOptions.getBasicOptions( props );
    if( props != null ) {
      String syntaxText = props.getProperty( "jkcemu.programming.asm.syntax" );

      Boolean allowUndocInst = getBoolean(
		props,
		"jkcemu.programming.asm.allow_undocumented_instructions" );

      Boolean labelsCaseSensitive = getBoolean(
		props,
		"jkcemu.programming.asm.labels.case_sensitive" );

      Boolean printLabels = getBoolean(
		props,
		"jkcemu.programming.asm.labels.print" );

      Boolean codeToEmu = getBoolean(
		props,
		"jkcemu.programming.code.to_emulator" );

      Boolean codeToSecondSys = getBoolean(
		props,
		"jkcemu.programming.code.to_second_system" );

      Boolean codeToFile = getBoolean(
		props,
		"jkcemu.programming.code.to_file" );

      String codeFileName = props.getProperty(
		"jkcemu.programming.code.file.name" );

      Boolean labelsToDebugger = getBoolean(
		props,
		"jkcemu.programming.labels_to_debugger" );

      Boolean labelsToReass = getBoolean(
		props,
		"jkcemu.programming.labels_to_reassembler" );

      Boolean formatSource = getBoolean(
		props,
		"jkcemu.programming.format.source" );

      Boolean warnNonAsciiChars = getBoolean(
		props,
		"jkcemu.programming.warn_non_ascii_chars" );

      if( (syntaxText != null)
	  || (allowUndocInst != null)
	  || (labelsCaseSensitive != null)
	  || (printLabels != null)
	  || (codeToEmu != null)
	  || (codeToSecondSys != null)
	  || (codeToFile != null)
	  || (codeFileName != null)
	  || (labelsToDebugger != null)
	  || (labelsToReass != null)
	  || (formatSource != null)
	  || (warnNonAsciiChars != null) )
      {
	if( options == null ) {
	  options = new PrgOptions();
	}
	if( syntaxText != null ) {
	  if( syntaxText.equals( "zilog" ) ) {
	    options.asmSyntax = Z80Assembler.Syntax.ZILOG_ONLY;
	  } else if( syntaxText.equals( "robotron" ) ) {
	    options.asmSyntax = Z80Assembler.Syntax.ROBOTRON_ONLY;
	  } else {
	    options.asmSyntax = Z80Assembler.Syntax.ALL;
	  }
	  if( allowUndocInst != null ) {
	    options.allowUndocInst = allowUndocInst.booleanValue();
	  }
	  if( labelsCaseSensitive != null ) {
	    options.labelsCaseSensitive = labelsCaseSensitive.booleanValue();
	  }
	  if( printLabels != null ) {
	    options.printLabels = printLabels.booleanValue();
	  }
	  if( codeToEmu != null ) {
	    options.codeToEmu = codeToEmu.booleanValue();
	  }
	  if( codeToSecondSys != null ) {
	    options.codeToSecondSys = codeToSecondSys.booleanValue();
	  }
	  if( codeToFile != null ) {
	    options.codeToFile = codeToFile.booleanValue();
	  }
	  File codeFile = null;
	  if( codeFileName != null ) {
	    if( !codeFileName.isEmpty() ) {
	      codeFile = new File( codeFileName );
	    }
	  }
	  options.codeFile = codeFile;
	  if( labelsToDebugger != null ) {
	    options.labelsToDebugger = labelsToDebugger.booleanValue();
	  }
	  if( labelsToReass != null ) {
	    options.labelsToReass = labelsToReass.booleanValue();
	  }
	  if( formatSource != null ) {
	    options.formatSource = formatSource.booleanValue();
	  }
	  if( warnNonAsciiChars != null ) {
	    options.warnNonAsciiChars = warnNonAsciiChars.booleanValue();
	  }
	}
      }
    }
    return options;
  }


  public boolean getWarnNonAsciiChars()
  {
    return this.warnNonAsciiChars;
  }


  public void putOptionsTo( Properties props )
  {
    if( props != null ) {
      if( this.asmSyntax != null ) {
	switch( this.asmSyntax ) {
	  case ZILOG_ONLY:
	    props.setProperty( "jkcemu.programming.asm.syntax", "zilog" );
	    break;

	  case ROBOTRON_ONLY:
	    props.setProperty( "jkcemu.programming.asm.syntax", "robotron" );
	    break;

	  default:
	    props.setProperty( "jkcemu.programming.asm.syntax", "all" );
	}
      }

      props.setProperty(
		"jkcemu.programming.asm.allow_undocumented_instructions",
		Boolean.toString( this.allowUndocInst ) );

      props.setProperty(
		"jkcemu.programming.asm.labels.case_sensitive",
		Boolean.toString( this.labelsCaseSensitive ) );

      props.setProperty(
		"jkcemu.programming.asm.labels.print",
		Boolean.toString( this.printLabels ) );

      props.setProperty(
		"jkcemu.programming.code.to_emulator",
		Boolean.toString( this.codeToEmu ) );

      props.setProperty(
		"jkcemu.programming.code.to_second_system",
		Boolean.toString( this.codeToSecondSys ) );

      props.setProperty(
		"jkcemu.programming.code.to_file",
		Boolean.toString( this.codeToFile ) );

      String codeFileName = null;
      if( this.codeFile != null ) {
	codeFileName = this.codeFile.getPath();
      }
      props.setProperty(
		"jkcemu.programming.code.file.name",
		codeFileName != null ? codeFileName : "" );

      props.setProperty(
		"jkcemu.programming.labels_to_debugger",
		Boolean.toString( this.labelsToDebugger ) );

      props.setProperty(
		"jkcemu.programming.labels_to_reassembler",
		Boolean.toString( this.labelsToReass ) );

      props.setProperty(
		"jkcemu.programming.format.source",
		Boolean.toString( this.formatSource ) );

      props.setProperty(
		"jkcemu.programming.warn_non_ascii_chars",
		Boolean.toString( this.warnNonAsciiChars ) );
    }
  }


  public void setAllowUndocInst( boolean state )
  {
    this.allowUndocInst = state;
  }


  public void setAsmSyntax( Z80Assembler.Syntax asmSyntax )
  {
    this.asmSyntax = asmSyntax;
  }


  public void setCodeToEmu( boolean state )
  {
    this.codeToEmu = state;
  }


  public void setCodeToFile( boolean state, File file )
  {
    this.codeToFile = state;
    this.codeFile   = file;
  }


  public void setCodeToSecondSystem( boolean state )
  {
    this.codeToSecondSys = state;
  }


  public void setFormatSource( boolean state )
  {
    this.formatSource = state;
  }


  public void setForceRun( boolean state )
  {
    this.forceRun = state;
  }


  public void setLabelsToDebugger( boolean state )
  {
    this.labelsToDebugger = state;
  }


  public void setLabelsToReassembler( boolean state )
  {
    this.labelsToReass = state;
  }


  public void setLabelsCaseSensitive( boolean state )
  {
    this.labelsCaseSensitive = state;
  }


  public void setPrintLabels( boolean state )
  {
    this.printLabels = state;
  }


  public void setWarnNonAsciiChars( boolean state )
  {
    this.warnNonAsciiChars = state;
  }


	/* --- geschuetzte Methoden --- */

  protected static Boolean getBoolean( Properties props, String keyword )
  {
    Boolean rv = null;
    if( props != null ) {
      String value = props.getProperty( keyword );
      if( value != null ) {
	if( !value.isEmpty() ) {
	  rv = Boolean.valueOf( value );
	}
      }
    }
    return rv;
  }


  protected static Integer getInteger( Properties props, String keyword )
  {
    Integer rv = null;
    if( props != null ) {
      String value = props.getProperty( keyword );
      if( value != null ) {
	if( !value.isEmpty() ) {
	  try {
	    rv = Integer.valueOf( value );
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean equals( Object o )
  {
    boolean rv = false;
    if( o != null ) {
      rv = super.equals( o );
      if( !rv && (o instanceof PrgOptions) ) {
	PrgOptions options = (PrgOptions) o;
	if( options.asmSyntax.equals( this.asmSyntax )
	    && (options.allowUndocInst      == this.allowUndocInst)
	    && (options.labelsCaseSensitive == this.labelsCaseSensitive)
	    && (options.printLabels         == this.printLabels)
	    && (options.codeToEmu           == this.codeToEmu)
	    && (options.codeToSecondSys     == this.codeToSecondSys)
	    && (options.codeToFile          == this.codeToFile)
	    && (options.forceRun            == this.forceRun)
	    && (options.labelsToDebugger    == this.labelsToDebugger)
	    && (options.labelsToReass       == this.labelsToReass)
	    && (options.formatSource        == this.formatSource)
	    && (options.warnNonAsciiChars      == this.warnNonAsciiChars) )
	{
	  if( (options.codeFile != null) && (this.codeFile != null) ) {
	    rv = options.codeFile.equals( this.codeFile );
	  } else {
	    if( (options.codeFile == null) && (this.codeFile == null) ) {
	      rv = true;
	    }
	  }
	}
      }
    }
    return rv;
  }
}
