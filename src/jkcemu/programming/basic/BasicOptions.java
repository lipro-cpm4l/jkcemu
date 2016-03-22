/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Optionen fuer den Basic-Compiler
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.util.Properties;
import jkcemu.base.*;
import jkcemu.programming.PrgOptions;
import jkcemu.programming.assembler.Z80Assembler;
import jkcemu.text.TextUtil;


public class BasicOptions extends PrgOptions
{
  public static enum BreakOption { NEVER, INPUT, ALWAYS };

  public static final int MAX_HEAP_SIZE      = 0x8000;
  public static final int MIN_HEAP_SIZE      = 0x0200;
  public static final int MIN_STACK_SIZE     = 64;
  public static final int DEFAULT_HEAP_SIZE  = 1024;
  public static final int DEFAULT_STACK_SIZE = 128;

  public static final String DEFAULT_APP_NAME = "MYAPP";

  private String         appName;
  private String         langCode;
  private String         targetText;
  private AbstractTarget target;
  private EmuSys         emuSys;
  private int            codeBegAddr;
  private int            bssBegAddr;
  private int            heapSize;
  private int            stackSize;
  private boolean        checkStack;
  private boolean        checkBounds;
  private boolean        openCrtEnabled;
  private boolean        openLptEnabled;
  private boolean        openFileEnabled;
  private boolean        openVdipEnabled;
  private boolean        inclBasicLines;
  private boolean        preferRelJumps;
  private boolean        printLineNumOnAbort;
  private boolean        showAsmText;
  private boolean        warnUnusedItems;
  private BreakOption    breakOption;


  public BasicOptions()
  {
    this.appName             = DEFAULT_APP_NAME;
    this.langCode            = null;
    this.targetText          = null;
    this.target              = null;
    this.emuSys              = null;
    this.codeBegAddr         = -1;
    this.bssBegAddr          = -1;
    this.heapSize            = DEFAULT_HEAP_SIZE;
    this.stackSize           = DEFAULT_STACK_SIZE;
    this.checkStack          = true;
    this.checkBounds         = true;
    this.openCrtEnabled      = true;
    this.openLptEnabled      = true;
    this.openFileEnabled     = true;
    this.openVdipEnabled     = true;
    this.inclBasicLines      = true;
    this.preferRelJumps      = true;
    this.printLineNumOnAbort = true;
    this.showAsmText         = false;
    this.warnUnusedItems     = true;
    this.breakOption         = BreakOption.ALWAYS;
    setAsmSyntax( Z80Assembler.Syntax.ZILOG_ONLY );
    setAllowUndocInst( false );
    setLabelsCaseSensitive( false );
    setPrintLabels( false );
  }


  public boolean canBreakAlways()
  {
    return this.breakOption == BreakOption.ALWAYS;
  }


  public boolean canBreakOnInput()
  {
    return (this.breakOption == BreakOption.ALWAYS)
	   || (this.breakOption == BreakOption.INPUT);
  }


  public String getAppName()
  {
    return this.appName;
  }


  public static BasicOptions getBasicOptions( Properties props )
  {
    BasicOptions options = null;
    if( props != null ) {
      String appName  = props.getProperty(
			"jkcemu.programming.basic.name" );

      String langCode = props.getProperty(
			"jkcemu.programming.basic.language.code" );

      String targetText = props.getProperty(
			"jkcemu.programming.basic.target" );

      Integer codeBegAddr = getInteger(
			props,
			"jkcemu.programming.basic.address.begin.code" );

      Integer bssBegAddr = getInteger(
			props,
			"jkcemu.programming.basic.address.begin.bss" );

      Integer heapSize = getInteger(
			props,
			"jkcemu.programming.basic.heap.size" );

      Integer stackSize = getInteger(
			props,
			"jkcemu.programming.basic.stack.size" );

      Boolean checkStack = getBoolean(
			props,
			"jkcemu.programming.basic.stack.check" );

      Boolean checkBounds = getBoolean(
			props,
			"jkcemu.programming.basic.bounds.check" );

      Boolean openCrtEnabled = getBoolean(
			props,
			"jkcemu.programming.basic.open.crt.enabled" );

      Boolean openLptEnabled = getBoolean(
			props,
			"jkcemu.programming.basic.open.lpt.enabled" );

      Boolean openFileEnabled = getBoolean(
			props,
			"jkcemu.programming.basic.open.file.enabled" );

      Boolean openVdipEnabled = getBoolean(
			props,
			"jkcemu.programming.basic.open.vdip.enabled" );

      Boolean preferRelJumps = getBoolean(
			props,
			"jkcemu.programming.basic.prefer_relative_jumps" );

      Boolean printLineNumOnAbort = getBoolean(
			props,
			"jkcemu.programming.basic.print_line_num_on_abort" );

      Boolean showAsmText = getBoolean(
			props,
			"jkcemu.programming.basic.show_assembler_source" );

      Boolean inclBasicLines = getBoolean(
			props,
			"jkcemu.programming.basic.include_basic_lines" );

      Boolean warnUnusedItems = getBoolean(
			props,
			"jkcemu.programming.basic.warn_unused_items" );

      String breakOptionText = props.getProperty(
			"jkcemu.programming.basic.breakoption" );

      if( (appName != null)
	  || (langCode != null)
	  || (targetText != null)
	  || (codeBegAddr != null)
	  || (bssBegAddr != null)
	  || (heapSize != null)
	  || (stackSize != null)
	  || (checkStack != null)
	  || (checkBounds != null)
	  || (openCrtEnabled != null)
	  || (openLptEnabled != null)
	  || (openFileEnabled != null)
	  || (openVdipEnabled != null)
	  || (preferRelJumps != null)
	  || (printLineNumOnAbort != null)
	  || (showAsmText != null)
	  || (inclBasicLines != null)
	  || (warnUnusedItems != null)
	  || (breakOptionText != null) )
      {
	options = new BasicOptions();

	if( appName != null ) {
	  options.appName = appName;
	}
	if( langCode != null ) {
	  options.langCode = langCode;
	}
	if( targetText != null ) {
	  options.targetText = targetText;
	}
	if( codeBegAddr != null ) {
	  options.codeBegAddr = codeBegAddr.intValue();
	}
	if( bssBegAddr != null ) {
	  options.bssBegAddr = bssBegAddr.intValue();
	}
	if( heapSize != null ) {
	  options.heapSize = heapSize.intValue();
	}
	if( stackSize != null ) {
	  options.stackSize = stackSize.intValue();
	}
	if( checkStack != null ) {
	  options.checkStack = checkStack.booleanValue();
	}
	if( checkBounds != null ) {
	  options.checkBounds = checkBounds.booleanValue();
	}
	if( openCrtEnabled != null ) {
	  options.openCrtEnabled = openCrtEnabled.booleanValue();
	}
	if( openLptEnabled != null ) {
	  options.openLptEnabled = openLptEnabled.booleanValue();
	}
	if( openFileEnabled != null ) {
	  options.openFileEnabled = openFileEnabled.booleanValue();
	}
	if( openVdipEnabled != null ) {
	  options.openVdipEnabled = openVdipEnabled.booleanValue();
	}
	if( preferRelJumps != null ) {
	  options.preferRelJumps = preferRelJumps.booleanValue();
	}
	if( printLineNumOnAbort != null ) {
	  options.printLineNumOnAbort = printLineNumOnAbort.booleanValue();
	}
	if( showAsmText != null ) {
	  options.showAsmText = showAsmText.booleanValue();
	}
	if( inclBasicLines != null ) {
	  options.inclBasicLines = inclBasicLines.booleanValue();
	}
	if( warnUnusedItems != null ) {
	  options.warnUnusedItems = warnUnusedItems.booleanValue();
	}
	if( breakOptionText != null ) {
	  if( breakOptionText.equals( "never" ) ) {
	    options.breakOption = BreakOption.NEVER;
	  } else if( breakOptionText.equals( "input" ) ) {
	    options.breakOption = BreakOption.INPUT;
	  } else {
	    options.breakOption = BreakOption.ALWAYS;
	  }
	}
      }
    }
    return options;
  }


  public BreakOption getBreakOption()
  {
    return this.breakOption;
  }


  public int getBssBegAddr()
  {
    return this.bssBegAddr;
  }


  public boolean getCheckBounds()
  {
    return this.checkBounds;
  }


  public boolean getCheckStack()
  {
    return this.checkStack;
  }


  public int getCodeBegAddr()
  {
    return this.codeBegAddr;
  }


  public EmuSys getEmuSys()
  {
    return this.emuSys;
  }


  public boolean getIncludeBasicLines()
  {
    return this.inclBasicLines;
  }


  public int getHeapSize()
  {
    return this.heapSize;
  }


  public String getLangCode()
  {
    return this.langCode;
  }


  public boolean getPreferRelativeJumps()
  {
    return this.preferRelJumps;
  }


  public boolean getPrintLineNumOnAbort()
  {
    return this.printLineNumOnAbort;
  }


  public boolean getShowAssemblerText()
  {
    return this.showAsmText;
  }


  public int getStackSize()
  {
    return this.stackSize;
  }


  public AbstractTarget getTarget()
  {
    return this.target;
  }


  public String getTargetText()
  {
    return this.targetText;
  }


  public boolean getWarnUnusedItems()
  {
    return this.warnUnusedItems;
  }


  public boolean isOpenCrtEnabled()
  {
    return this.openCrtEnabled;
  }


  public boolean isOpenFileEnabled()
  {
    return this.openFileEnabled;
  }


  public boolean isOpenLptEnabled()
  {
    return this.openLptEnabled;
  }


  public boolean isOpenVdipEnabled()
  {
    return this.openVdipEnabled;
  }


  public void setAppName( String appName )
  {
    this.appName = appName;
  }


  public void setBreakOption( BreakOption value )
  {
    this.breakOption = value;
  }


  public void setCheckBounds( boolean state )
  {
    this.checkBounds = state;
  }


  public void setCheckStack( boolean state )
  {
    this.checkStack = state;
  }


  public void setBssBegAddr( int value )
  {
    this.bssBegAddr = value;
  }


  public void setCodeBegAddr( int value )
  {
    this.codeBegAddr = value;
  }


  public void setEmuSys( EmuSys emuSys )
  {
    this.emuSys = emuSys;
  }


  public void setHeapSize( int value )
  {
    this.heapSize = value;
  }


  public void setIncludeBasicLines( boolean state )
  {
    this.inclBasicLines = state;
  }


  public void setLangCode( String langCode )
  {
    this.langCode = langCode;
  }


  public void setOpenCrtEnabled( boolean state )
  {
    this.openCrtEnabled = state;
  }


  public void setOpenLptEnabled( boolean state )
  {
    this.openLptEnabled = state;
  }


  public void setOpenFileEnabled( boolean state )
  {
    this.openFileEnabled = state;
  }


  public void setOpenVdipEnabled( boolean state )
  {
    this.openVdipEnabled = state;
  }


  public void setTarget( AbstractTarget target )
  {
    this.target     = target;
    this.targetText = target.toString();
  }


  public void setPreferRelativeJumps( boolean state )
  {
    this.preferRelJumps = state;
  }


  public void setPrintLineNumOnAbort( boolean state )
  {
    this.printLineNumOnAbort = state;
  }


  public void setShowAssemblerText( boolean state )
  {
    this.showAsmText = state;
  }


  public void setStackSize( int value )
  {
    this.stackSize = value;
  }


  public void setWarnUnusedItems( boolean state )
  {
    this.warnUnusedItems = state;
  }


	/* --- ueberschrieben Methoden --- */

  @Override
  public boolean equals( Object o )
  {
    boolean rv = false;
    if( o != null ) {
      if( super.equals( o ) && (o instanceof BasicOptions) ) {
	BasicOptions options = (BasicOptions) o;
	if( TextUtil.equals( options.appName, this.appName )
	    && TextUtil.equals( options.langCode, this.langCode )
	    && TextUtil.equals( options.targetText, this.targetText )
	    && (options.codeBegAddr         == this.codeBegAddr)
	    && (options.bssBegAddr          == this.bssBegAddr)
	    && (options.heapSize            == this.heapSize)
	    && (options.stackSize           == this.stackSize)
	    && (options.checkStack          == this.checkStack)
	    && (options.checkBounds         == this.checkBounds)
	    && (options.openCrtEnabled      == this.openCrtEnabled)
	    && (options.openLptEnabled      == this.openLptEnabled)
	    && (options.openFileEnabled     == this.openFileEnabled)
	    && (options.openVdipEnabled     == this.openVdipEnabled)
	    && (options.inclBasicLines      == this.inclBasicLines)
	    && (options.preferRelJumps      == this.preferRelJumps)
	    && (options.printLineNumOnAbort == this.printLineNumOnAbort)
	    && (options.showAsmText         == this.showAsmText)
	    && (options.warnUnusedItems     == this.warnUnusedItems)
	    && (options.breakOption         == this.breakOption) )
	{
	  rv = true;
	}
      }
    }
    return rv;
  }


  @Override
  public void putOptionsTo( Properties props )
  {
    super.putOptionsTo( props );
    if( props != null ) {
      props.setProperty(
		"jkcemu.programming.basic.name",
		this.appName != null ? this.appName : "" );

      props.setProperty(
		"jkcemu.programming.basic.language.code",
		this.langCode != null ? this.langCode : "" );

      props.setProperty(
		"jkcemu.programming.basic.target",
		this.targetText != null ? this.targetText : "" );

      props.setProperty(
		"jkcemu.programming.basic.address.begin.code",
                Integer.toString( this.codeBegAddr ) );

      props.setProperty(
		"jkcemu.programming.basic.address.begin.bss",
                Integer.toString( this.bssBegAddr ) );

      props.setProperty(
		"jkcemu.programming.basic.heap.size",
                Integer.toString( this.heapSize ) );

      props.setProperty(
		"jkcemu.programming.basic.stack.size",
                Integer.toString( this.stackSize ) );

      props.setProperty(
		"jkcemu.programming.basic.stack.check",
                Boolean.toString( this.checkStack ) );

      props.setProperty(
		"jkcemu.programming.basic.bounds.check",
                Boolean.toString( this.checkBounds ) );

      props.setProperty(
		"jkcemu.programming.basic.open.crt.enabled",
                Boolean.toString( this.openCrtEnabled ) );

      props.setProperty(
		"jkcemu.programming.basic.open.lpt.enabled",
                Boolean.toString( this.openLptEnabled ) );

      props.setProperty(
		"jkcemu.programming.basic.open.file.enabled",
                Boolean.toString( this.openFileEnabled ) );

      props.setProperty(
		"jkcemu.programming.basic.open.vdip.enabled",
                Boolean.toString( this.openVdipEnabled ) );

      props.setProperty(
		"jkcemu.programming.basic.prefer_relative_jumps",
                Boolean.toString( this.preferRelJumps ) );

      props.setProperty(
		"jkcemu.programming.basic.print_line_num_on_abort",
                Boolean.toString( this.printLineNumOnAbort ) );

      props.setProperty(
		"jkcemu.programming.basic.show_assembler_source",
                Boolean.toString( this.showAsmText ) );

      props.setProperty(
		"jkcemu.programming.basic.include_basic_lines",
                Boolean.toString( this.inclBasicLines ) );

      props.setProperty(
		"jkcemu.programming.warn_unused_items",
		Boolean.toString( this.warnUnusedItems ) );

      if( this.breakOption != null ) {
	String value = "always";
	switch( this.breakOption ) {
	  case NEVER:
	    value = "never";
	    break;
	  case INPUT:
	    value = "input";
	    break;
	}
	props.setProperty( "jkcemu.programming.basic.breakoption", value );
      }
    }
  }
}
