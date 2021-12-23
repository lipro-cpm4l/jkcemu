/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Optionen fuer den Compiler/Assembler
 */

package jkcemu.programming;

import java.io.File;
import java.util.Properties;
import jkcemu.base.EmuThread;
import jkcemu.programming.assembler.Z80Assembler;
import jkcemu.programming.basic.BasicOptions;


public class PrgOptions
{
  public static final String OPTION_PREFIX = "jkcemu.programming.";

  private static final String OPTION_ALLOW_UNDOC_INSTRUCTIONS
		= OPTION_PREFIX + "allow_undocumented_instructions";

  private static final String OPTION_LABELS_CASE_SENSITIVE
		= OPTION_PREFIX + "labels.case_sensitive";

  private static final String OPTION_ASM_LISTING
		= OPTION_PREFIX + "asm.listing";

  private static final String OPTION_LABELS_PRINT
		= OPTION_PREFIX + "labels.print";

  private static final String OPTION_REPLACE_TOO_LONG_REL_JUMPS
		= OPTION_PREFIX + "replace_too_long_rel_jumps";

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

  private static final String OPTION_LABELS_UPDATE_BREAKPOINTS_ONLY
		= OPTION_PREFIX + "labels_update_breakpoints_only";

  private static final String OPTION_WARN_NON_ASCII_CHARS
			= OPTION_PREFIX + "warn_non_ascii_chars";

  private static final String VALUE_ASM_SYNTAX_ALL      = "all";
  private static final String VALUE_ASM_SYNTAX_ROBOTRON = "robotron";
  private static final String VALUE_ASM_SYNTAX_ZILOG    = "zilog";

  private Z80Assembler.Syntax asmSyntax;
  private boolean             asmListing;
  private boolean             allowUndocInst;
  private boolean             labelsCaseSensitive;
  private boolean             printLabels;
  private boolean             replaceTooLongRelJumps;
  private boolean             codeToEmu;
  private boolean             codeToSecondSys;
  private boolean             codeToFile;
  private File                codeFile;
  private boolean             forceRun;
  private boolean             labelsToDebugger;
  private boolean             labelsUpdBreakpointsOnly;
  private boolean             labelsToReass;
  private boolean             formatSource;
  private boolean             warnNonAsciiChars;


  public PrgOptions()
  {
    this( null );
  }


  public PrgOptions( PrgOptions src )
  {
    if( src != null ) {
      this.asmSyntax                = src.asmSyntax;
      this.asmListing               = src.asmListing;
      this.allowUndocInst           = src.allowUndocInst;
      this.labelsCaseSensitive      = src.labelsCaseSensitive;
      this.printLabels              = src.printLabels;
      this.replaceTooLongRelJumps   = src.replaceTooLongRelJumps;
      this.codeToEmu                = src.codeToEmu;
      this.codeToSecondSys          = src.codeToSecondSys;
      this.codeToFile               = src.codeToFile;
      this.codeFile                 = src.codeFile;
      this.forceRun                 = src.forceRun;
      this.labelsToDebugger         = src.labelsToDebugger;
      this.labelsToReass            = src.labelsToReass;
      this.formatSource             = src.formatSource;
      this.warnNonAsciiChars        = src.warnNonAsciiChars;
      this.labelsUpdBreakpointsOnly = src.labelsUpdBreakpointsOnly;
    } else {
      this.asmSyntax                = Z80Assembler.Syntax.ALL;
      this.asmListing               = false;
      this.allowUndocInst           = false;
      this.labelsCaseSensitive      = false;
      this.printLabels              = false;
      this.replaceTooLongRelJumps   = false;
      this.codeToEmu                = false;
      this.codeToSecondSys          = false;
      this.codeToFile               = false;
      this.codeFile                 = null;
      this.forceRun                 = false;
      this.labelsToDebugger         = false;
      this.labelsUpdBreakpointsOnly = false;
      this.labelsToReass            = false;
      this.formatSource             = false;
      this.warnNonAsciiChars        = true;
    }
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


  public boolean getCreateAsmListing()
  {
    return this.asmListing;
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


  public boolean getLabelsUpdateBreakpointsOnly()
  {
    return this.labelsUpdBreakpointsOnly;
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

      Boolean asmListing = getBoolean(
			props,
			OPTION_ASM_LISTING );

      Boolean allowUndocInst = getBoolean(
			props,
			OPTION_ALLOW_UNDOC_INSTRUCTIONS );

      Boolean labelsCaseSensitive = getBoolean(
			props,
			OPTION_LABELS_CASE_SENSITIVE );

      Boolean printLabels = getBoolean(
			props,
			OPTION_LABELS_PRINT );

      Boolean replaceTooLongRelJumps = getBoolean(
			props,
			OPTION_REPLACE_TOO_LONG_REL_JUMPS );

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

      Boolean labelsUpdBreakpointsOnly = getBoolean(
			props,
			OPTION_LABELS_UPDATE_BREAKPOINTS_ONLY );

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
	  || (asmListing != null)
	  || (allowUndocInst != null)
	  || (labelsCaseSensitive != null)
	  || (printLabels != null)
	  || (replaceTooLongRelJumps != null)
	  || (codeToEmu != null)
	  || (codeToSecondSys != null)
	  || (codeToFile != null)
	  || (codeFileName != null)
	  || (labelsToDebugger != null)
	  || (labelsToReass != null)
	  || (labelsUpdBreakpointsOnly != null)
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
	  if( asmListing != null ) {
	    options.asmListing = asmListing.booleanValue();
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
	  if( replaceTooLongRelJumps != null ) {
	    options.replaceTooLongRelJumps
			= replaceTooLongRelJumps.booleanValue();
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
	  if( labelsUpdBreakpointsOnly != null ) {
	    options.labelsUpdBreakpointsOnly
			= labelsUpdBreakpointsOnly.booleanValue();
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


  public boolean getReplaceTooLongRelJumps()
  {
    return this.replaceTooLongRelJumps;
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
		OPTION_ASM_LISTING,
		Boolean.toString( this.asmListing ) );

      props.setProperty(
		OPTION_ALLOW_UNDOC_INSTRUCTIONS,
		Boolean.toString( this.allowUndocInst ) );

      props.setProperty(
		OPTION_LABELS_CASE_SENSITIVE,
		Boolean.toString( this.labelsCaseSensitive ) );

      props.setProperty(
		OPTION_LABELS_PRINT,
		Boolean.toString( this.printLabels ) );

      props.setProperty(
		OPTION_REPLACE_TOO_LONG_REL_JUMPS,
		Boolean.toString( replaceTooLongRelJumps ) );

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
		OPTION_LABELS_UPDATE_BREAKPOINTS_ONLY,
		Boolean.toString( this.labelsUpdBreakpointsOnly ) );

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


  /*
   * Die Methode vergleicht nur die eigentlichen Optionen,
   * die auch in die Profildatei geschrieben werden.
   */
  public boolean sameOptions( PrgOptions o )
  {
    return o != null ?
	(equals( this.asmSyntax, o.asmSyntax )
		&& equals( this.codeFile, o.codeFile )
		&& (this.allowUndocInst      == o.allowUndocInst)
		&& (this.asmListing          == o.asmListing)
		&& (this.labelsCaseSensitive == o.labelsCaseSensitive)
		&& (this.printLabels         == o.printLabels)
		&& (this.replaceTooLongRelJumps
				== o.replaceTooLongRelJumps)
		&& (this.codeToEmu           == o.codeToEmu)
		&& (this.codeToSecondSys     == o.codeToSecondSys)
		&& (this.codeToFile          == o.codeToFile)
		&& (this.labelsToDebugger    == o.labelsToDebugger)
		&& (this.formatSource        == o.formatSource)
		&& (this.warnNonAsciiChars   == o.warnNonAsciiChars)
		&& (this.labelsToReass       == o.labelsToReass)
		&& (this.labelsUpdBreakpointsOnly
				== o.labelsUpdBreakpointsOnly))
	: false;
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


  public void setCreateAsmListing( boolean state )
  {
    this.asmListing = state;
  }


  public void setFormatSource( boolean state )
  {
    this.formatSource = state;
  }


  public void setForceRun( boolean state )
  {
    this.forceRun = state;
  }


  public void setLabelsCaseSensitive( boolean state )
  {
    this.labelsCaseSensitive = state;
  }


  public void setLabelsToDebugger( boolean state )
  {
    this.labelsToDebugger = state;
  }


  public void setLabelsToReassembler( boolean state )
  {
    this.labelsToReass = state;
  }


  public void setLabelsUpdateBreakpointsOnly( boolean state )
  {
    this.labelsUpdBreakpointsOnly = state;
  }


  public void setPrintLabels( boolean state )
  {
    this.printLabels = state;
  }


  public void setReplaceTooLongRelJumps( boolean state )
  {
    this.replaceTooLongRelJumps = state;
  }


  public void setWarnNonAsciiChars( boolean state )
  {
    this.warnNonAsciiChars = state;
  }


	/* --- geschuetzte Methoden --- */

  protected static boolean equals( Object o1, Object o2 )
  {
    boolean rv = false;
    if( (o1 != null) && (o2 != null) ) {
      rv = o1.equals( o2 );
    } else if( (o1 == null) && (o2 == null) ) {
      rv = true;
    }
    return rv;
  }


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
}
