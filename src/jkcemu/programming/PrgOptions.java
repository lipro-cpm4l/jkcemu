/*
 * (c) 2008-2016 Jens Mueller
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
  public static final String OPTION_PREFIX = "jkcemu.programming.";

  private static final String OPTION_ASM_ALLOW_UNDOC_INSTRUCTIONS
		= OPTION_PREFIX + "asm.allow_undocumented_instructions";

  private static final String OPTION_ASM_LABELS_CASE_SENSITIVE
		= OPTION_PREFIX + "asm.labels.case_sensitive";

  private static final String OPTION_ASM_LABELS_PRINT
		= OPTION_PREFIX + "asm.labels.print";

  private static final String OPTION_ASM_SYNTAX
		= OPTION_PREFIX + "asm.syntax";

  private static final String OPTION_CODE_FILENAME
		= OPTION_PREFIX + "code.file.name";

  private static final String OPTION_CODE_TO_EMULATOR
		= OPTION_PREFIX + "code.to_emulator";

  private static final String OPTION_CODE_TO_FILE
		= OPTION_PREFIX + "code.to_file";

  private static final String OPTION_CODE_TO_SECOND_SYSTEM
		= OPTION_PREFIX + "code.to_second_system";

  private static final String OPTION_FORMAT_SOURCE
		= OPTION_PREFIX + "format.source";

  private static final String OPTION_LABELS_TO_DEBUGGER
		= OPTION_PREFIX + "labels_to_debugger";

  private static final String OPTION_LABELS_TO_REASSEMBLER
		= OPTION_PREFIX + "labels_to_reassembler";

  private static final String OPTION_SUPPRESS_LABEL_RECREATE_IN_DEBUGGER
		= OPTION_PREFIX + "suppress_label_recreate_in_debugger";

  private static final String OPTION_WARN_NON_ASCII_CHARS
			= OPTION_PREFIX + "warn_non_ascii_chars";

  private static final String VALUE_ASM_SYNTAX_ALL      = "all";
  private static final String VALUE_ASM_SYNTAX_ROBOTRON = "robotron";
  private static final String VALUE_ASM_SYNTAX_ZILOG    = "zilog";

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
  private boolean             suppressLabelRecreateInDebugger;
  private boolean             labelsToReass;
  private boolean             formatSource;
  private boolean             warnNonAsciiChars;


  public PrgOptions()
  {
    this.asmSyntax                       = Z80Assembler.Syntax.ALL;
    this.allowUndocInst                  = false;
    this.labelsCaseSensitive             = false;
    this.printLabels                     = false;
    this.codeToEmu                       = false;
    this.codeToSecondSys                 = false;
    this.codeToFile                      = false;
    this.codeFile                        = null;
    this.forceRun                        = false;
    this.labelsToDebugger                = false;
    this.suppressLabelRecreateInDebugger = false;
    this.labelsToReass                   = false;
    this.formatSource                    = false;
    this.warnNonAsciiChars               = true;
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
      String syntaxText = props.getProperty( OPTION_ASM_SYNTAX );

      Boolean allowUndocInst = getBoolean(
			props,
			OPTION_ASM_ALLOW_UNDOC_INSTRUCTIONS );

      Boolean labelsCaseSensitive = getBoolean(
			props,
			OPTION_ASM_LABELS_CASE_SENSITIVE );

      Boolean printLabels = getBoolean(
			props,
			OPTION_ASM_LABELS_PRINT );

      Boolean codeToEmu = getBoolean(
			props,
			OPTION_CODE_TO_EMULATOR );

      Boolean codeToSecondSys = getBoolean(
			props,
			OPTION_CODE_TO_SECOND_SYSTEM );

      Boolean codeToFile = getBoolean(
			props,
			OPTION_CODE_TO_FILE );

      String codeFileName = props.getProperty( OPTION_CODE_FILENAME );

      Boolean suppressLabelRecreateInDebugger = getBoolean(
			props,
			OPTION_SUPPRESS_LABEL_RECREATE_IN_DEBUGGER );

      Boolean labelsToDebugger = getBoolean(
			props,
			OPTION_LABELS_TO_DEBUGGER );

      Boolean labelsToReass = getBoolean(
			props,
			OPTION_LABELS_TO_REASSEMBLER );

      Boolean formatSource = getBoolean(
			props,
			OPTION_FORMAT_SOURCE );

      Boolean warnNonAsciiChars = getBoolean(
			props,
			OPTION_WARN_NON_ASCII_CHARS );

      if( (syntaxText != null)
	  || (allowUndocInst != null)
	  || (labelsCaseSensitive != null)
	  || (printLabels != null)
	  || (codeToEmu != null)
	  || (codeToSecondSys != null)
	  || (codeToFile != null)
	  || (codeFileName != null)
	  || (labelsToDebugger != null)
	  || (suppressLabelRecreateInDebugger != null)
	  || (labelsToReass != null)
	  || (formatSource != null)
	  || (warnNonAsciiChars != null) )
      {
	if( options == null ) {
	  options = new PrgOptions();
	}
	if( syntaxText != null ) {
	  if( syntaxText.equals( VALUE_ASM_SYNTAX_ZILOG ) ) {
	    options.asmSyntax = Z80Assembler.Syntax.ZILOG_ONLY;
	  } else if( syntaxText.equals( VALUE_ASM_SYNTAX_ROBOTRON ) ) {
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
	  if( suppressLabelRecreateInDebugger != null ) {
	    options.suppressLabelRecreateInDebugger
			= suppressLabelRecreateInDebugger.booleanValue();
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


  public boolean getSuppressLabelRecreateInDebugger()
  {
    return this.suppressLabelRecreateInDebugger;
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
	    props.setProperty( OPTION_ASM_SYNTAX, VALUE_ASM_SYNTAX_ZILOG );
	    break;

	  case ROBOTRON_ONLY:
	    props.setProperty( OPTION_ASM_SYNTAX, VALUE_ASM_SYNTAX_ROBOTRON );
	    break;

	  default:
	    props.setProperty( OPTION_ASM_SYNTAX, VALUE_ASM_SYNTAX_ALL );
	}
      }

      props.setProperty(
		OPTION_ASM_ALLOW_UNDOC_INSTRUCTIONS,
		Boolean.toString( this.allowUndocInst ) );

      props.setProperty(
		OPTION_ASM_LABELS_CASE_SENSITIVE,
		Boolean.toString( this.labelsCaseSensitive ) );

      props.setProperty(
		OPTION_ASM_LABELS_PRINT,
		Boolean.toString( this.printLabels ) );

      props.setProperty(
		OPTION_CODE_TO_EMULATOR,
		Boolean.toString( this.codeToEmu ) );

      props.setProperty(
		OPTION_CODE_TO_SECOND_SYSTEM,
		Boolean.toString( this.codeToSecondSys ) );

      props.setProperty(
		OPTION_CODE_TO_FILE,
		Boolean.toString( this.codeToFile ) );

      String codeFileName = null;
      if( this.codeFile != null ) {
	codeFileName = this.codeFile.getPath();
      }
      props.setProperty(
		OPTION_CODE_FILENAME,
		codeFileName != null ? codeFileName : "" );

      props.setProperty(
		OPTION_LABELS_TO_DEBUGGER,
		Boolean.toString( this.labelsToDebugger ) );

      props.setProperty(
		OPTION_SUPPRESS_LABEL_RECREATE_IN_DEBUGGER,
		Boolean.toString( this.suppressLabelRecreateInDebugger ) );

      props.setProperty(
		OPTION_LABELS_TO_REASSEMBLER,
		Boolean.toString( this.labelsToReass ) );

      props.setProperty(
		OPTION_FORMAT_SOURCE,
		Boolean.toString( this.formatSource ) );

      props.setProperty(
		OPTION_WARN_NON_ASCII_CHARS,
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


  public void setSuppressLabelRecreateInDebugger( boolean state )
  {
    this.suppressLabelRecreateInDebugger = state;
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
	    && (options.suppressLabelRecreateInDebugger
				== this.suppressLabelRecreateInDebugger)
	    && (options.labelsToReass       == this.labelsToReass)
	    && (options.formatSource        == this.formatSource)
	    && (options.warnNonAsciiChars   == this.warnNonAsciiChars) )
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
