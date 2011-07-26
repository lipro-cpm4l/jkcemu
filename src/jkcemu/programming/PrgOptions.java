/*
 * (c) 2008-2011 Jens Mueller
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
import jkcemu.programming.basic.BasicOptions;


public class PrgOptions
{
  public static enum Syntax { ALL, ZILOG_ONLY, ROBOTRON_ONLY };


  private Syntax  syntax;
  private boolean allowUndocInst;
  private boolean labelsCaseSensitive;
  private boolean printLabels;
  private boolean codeToEmu;
  private boolean codeToSecondSys;
  private boolean codeToFile;
  private File    codeFile;
  private String  codeFileFmt;
  private char    codeFileType;
  private String  codeFileDesc;
  private boolean labelsToDebugger;
  private boolean labelsToReass;
  private boolean formatSource;


  public PrgOptions()
  {
    this.syntax              = Syntax.ALL;
    this.allowUndocInst      = false;
    this.labelsCaseSensitive = false;
    this.printLabels         = false;
    this.codeToEmu           = false;
    this.codeToSecondSys     = false;
    this.codeToFile          = false;
    this.codeFile            = null;
    this.codeFileFmt         = null;
    this.codeFileType        = '\u0020';
    this.codeFileDesc        = null;
    this.labelsToDebugger    = false;
    this.labelsToReass       = false;
    this.formatSource        = false;
  }


  public PrgOptions( PrgOptions src )
  {
    this.syntax              = src.syntax;
    this.allowUndocInst      = src.allowUndocInst;
    this.labelsCaseSensitive = src.labelsCaseSensitive;
    this.printLabels         = src.printLabels;
    this.labelsToDebugger    = src.labelsToDebugger;
    this.labelsToReass       = src.labelsToReass;
    this.formatSource        = src.formatSource;
    copyCodeDestOptionsFrom( src );
  }


  public void copyCodeDestOptionsFrom( PrgOptions src )
  {
    this.codeToEmu       = src.codeToEmu;
    this.codeToSecondSys = src.codeToSecondSys;
    this.codeToFile      = src.codeToFile;
    this.codeFile        = src.codeFile;
    this.codeFileFmt     = src.codeFileFmt;
    this.codeFileType    = src.codeFileType;
    this.codeFileDesc    = src.codeFileDesc;
  }


  public boolean getAllowUndocInst()
  {
    return this.allowUndocInst;
  }


  public File getCodeFile()
  {
    return this.codeFile;
  }


  public char getCodeFileType()
  {
    return this.codeFileType;
  }


  public String getCodeFileDesc()
  {
    return this.codeFileDesc;
  }


  public String getCodeFileFormat()
  {
    return this.codeFileFmt;
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


  public boolean getLabelsToDebugger()
  {
    return this.labelsToDebugger;
  }


  public boolean getLabelsToReassembler()
  {
    return this.labelsToReass;
  }


  public boolean getFormatSource()
  {
    return this.formatSource;
  }


  public boolean getPrintLabels()
  {
    return this.printLabels;
  }


  public boolean getLabelsCaseSensitive()
  {
    return this.labelsCaseSensitive;
  }


  public static PrgOptions getPrgOptions(
				EmuThread  emuThread,
				Properties props )
  {
    PrgOptions options = BasicOptions.getBasicOptions( emuThread, props );
    if( props != null ) {
      String syntaxText = props.getProperty( "jkcemu.programmimg.asm.syntax" );

      Boolean allowUndocInst = getBoolean(
		props,
		"jkcemu.programmimg.asm.allow_undocumented_instructions" );

      Boolean labelsCaseSensitive = getBoolean(
		props,
		"jkcemu.programmimg.asm.labels.case_sensitive" );

      Boolean printLabels = getBoolean(
		props,
		"jkcemu.programmimg.asm.labels.print" );

      Boolean codeToEmu = getBoolean(
		props,
		"jkcemu.programmimg.code.to_emulator" );

      Boolean codeToSecondSys = getBoolean(
		props,
		"jkcemu.programmimg.code.to_second_system" );

      Boolean codeToFile = getBoolean(
		props,
		"jkcemu.programmimg.code.to_file" );

      String codeFileName = props.getProperty(
		"jkcemu.programmimg.code.file.name" );

      String codeFileFmt = props.getProperty(
		"jkcemu.programmimg.code.file.format" );

      String codeFileType = props.getProperty(
		"jkcemu.programmimg.code.file.type" );

      String codeFileDesc = props.getProperty(
		"jkcemu.programmimg.code.file.description" );

      Boolean labelsToDebugger = getBoolean(
		props,
		"jkcemu.programmimg.labels_to_debugger" );

      Boolean labelsToReass = getBoolean(
		props,
		"jkcemu.programmimg.labels_to_reassembler" );

      Boolean formatSource = getBoolean(
		props,
		"jkcemu.programmimg.format.source" );

      if( (syntaxText != null)
	  || (allowUndocInst != null)
	  || (labelsCaseSensitive != null)
	  || (printLabels != null)
	  || (codeToEmu != null)
	  || (codeToSecondSys != null)
	  || (codeToFile != null)
	  || (codeFileName != null)
	  || (codeFileFmt != null)
	  || (codeFileType != null)
	  || (codeFileDesc != null)
	  || (labelsToDebugger != null)
	  || (labelsToReass != null)
	  || (formatSource != null) )
      {
	if( options == null ) {
	  options = new PrgOptions();
	}
	if( syntaxText != null ) {
	  if( syntaxText.equals( "zilog" ) ) {
	    options.syntax = Syntax.ZILOG_ONLY;
	  } else if( syntaxText.equals( "robotron" ) ) {
	    options.syntax = Syntax.ROBOTRON_ONLY;
	  } else {
	    options.syntax = Syntax.ALL;
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
	  options.codeFile    = codeFile;
	  options.codeFileFmt = codeFileFmt;
	  if( codeFileType != null ) {
	    if( !codeFileType.isEmpty() ) {
	      options.codeFileType = codeFileType.charAt( 0 );
	    }
	  }
	  options.codeFileDesc = codeFileDesc;
	  if( labelsToDebugger != null ) {
	    options.labelsToDebugger = labelsToDebugger.booleanValue();
	  }
	  if( labelsToReass != null ) {
	    options.labelsToReass = labelsToReass.booleanValue();
	  }
	  if( formatSource != null ) {
	    options.formatSource = formatSource.booleanValue();
	  }
	}
      }
    }
    return options;
  }


  public Syntax getSyntax()
  {
    return this.syntax;
  }


  public void putOptionsTo( Properties props )
  {
    if( props != null ) {
      if( this.syntax != null ) {
	switch( this.syntax ) {
	  case ZILOG_ONLY:
	    props.setProperty( "jkcemu.programmimg.asm.syntax", "zilog" );
	    break;

	  case ROBOTRON_ONLY:
	    props.setProperty( "jkcemu.programmimg.asm.syntax", "robotron" );
	    break;

	  default:
	    props.setProperty( "jkcemu.programmimg.asm.syntax", "all" );
	}
      }

      props.setProperty(
		"jkcemu.programmimg.asm.allow_undocumented_instructions",
		Boolean.toString( this.allowUndocInst ) );

      props.setProperty(
		"jkcemu.programmimg.asm.labels.case_sensitive",
		Boolean.toString( this.labelsCaseSensitive ) );

      props.setProperty(
		"jkcemu.programmimg.asm.labels.print",
		Boolean.toString( this.printLabels ) );

      props.setProperty(
		"jkcemu.programmimg.code.to_emulator",
		Boolean.toString( this.codeToEmu ) );

      props.setProperty(
		"jkcemu.programmimg.code.to_second_system",
		Boolean.toString( this.codeToSecondSys ) );

      props.setProperty(
		"jkcemu.programmimg.code.to_file",
		Boolean.toString( this.codeToFile ) );

      String codeFileName = null;
      if( this.codeFile != null ) {
	codeFileName = this.codeFile.getPath();
      }
      props.setProperty(
		"jkcemu.programmimg.code.file.name",
		codeFileName != null ? codeFileName : "" );

      props.setProperty(
		"jkcemu.programmimg.code.file.format",
		this.codeFileFmt != null ? this.codeFileFmt : "" );

      props.setProperty(
		"jkcemu.programmimg.code.file.type",
		Character.toString( this.codeFileType ) );

      props.setProperty(
		"jkcemu.programmimg.code.file.description",
		this.codeFileDesc != null ? this.codeFileDesc : "" );

      props.setProperty(
		"jkcemu.programmimg.labels_to_debugger",
		Boolean.toString( this.labelsToDebugger ) );

      props.setProperty(
		"jkcemu.programmimg.labels_to_reassembler",
		Boolean.toString( this.labelsToReass ) );

      props.setProperty(
		"jkcemu.programmimg.format.source",
		Boolean.toString( this.formatSource ) );
    }
  }


  public void setAllowUndocInst( boolean state )
  {
    this.allowUndocInst = state;
  }


  public void setCodeToEmu( boolean state )
  {
    this.codeToEmu = state;
  }


  public void setCodeToFile(
		boolean state,
		File    file,
		String  fileFmt,
		char    fileType,
		String  fileDesc )
  {
    this.codeToFile   = state;
    this.codeFile     = file;
    this.codeFileFmt  = fileFmt;
    this.codeFileType = fileType;
    this.codeFileDesc = fileDesc;
  }


  public void setCodeToSecondSystem( boolean state )
  {
    this.codeToSecondSys = state;
  }


  public void setLabelsToDebugger( boolean state )
  {
    this.labelsToDebugger = state;
  }


  public void setLabelsToReassembler( boolean state )
  {
    this.labelsToReass = state;
  }


  public void setFormatSource( boolean state )
  {
    this.formatSource = state;
  }


  public void setLabelsCaseSensitive( boolean state )
  {
    this.labelsCaseSensitive = state;
  }


  public void setPrintLabels( boolean state )
  {
    this.printLabels = state;
  }


  public void setSyntax( Syntax syntax )
  {
    this.syntax = syntax;
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
}

