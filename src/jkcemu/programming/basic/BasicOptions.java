/*
 * (c) 2008-2013 Jens Mueller
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
  private int            begAddr;
  private int            heapSize;
  private int            stackSize;
  private boolean        checkStack;
  private boolean        checkBounds;
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
    this.begAddr             = -1;
    this.heapSize            = DEFAULT_HEAP_SIZE;
    this.stackSize           = DEFAULT_STACK_SIZE;
    this.checkStack          = true;
    this.checkBounds         = true;
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

      Integer begAddr = getInteger(
			props,
			"jkcemu.programming.basic.address.begin" );

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

      Boolean preferRelJumps = getBoolean(
			props,
			"jkcemu.programming.basic.prefer_relative_jumps" );

      Boolean printLineNumOnAbort = getBoolean(
			props,
			"jkcemu.programming.basic.print_line_num_on_abort" );

      Boolean showAsmText = getBoolean(
			props,
			"jkcemu.programming.basic.show_assembler_source" );

      Boolean warnUnusedItems = getBoolean(
			props,
			"jkcemu.programming.basic.warn_unused_items" );

      String breakOptionText = props.getProperty(
			"jkcemu.programming.basic.breakoption" );

      if( (appName != null)
	  || (langCode != null)
	  || (targetText != null)
	  || (begAddr != null)
	  || (heapSize != null)
	  || (stackSize != null)
	  || (checkStack != null)
	  || (checkBounds != null)
	  || (preferRelJumps != null)
	  || (printLineNumOnAbort != null)
	  || (showAsmText != null)
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
	if( begAddr != null ) {
	  options.begAddr = begAddr.intValue();
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
	if( preferRelJumps != null ) {
	  options.preferRelJumps = preferRelJumps.booleanValue();
	}
	if( printLineNumOnAbort != null ) {
	  options.printLineNumOnAbort = printLineNumOnAbort.booleanValue();
	}
	if( showAsmText != null ) {
	  options.showAsmText = showAsmText.booleanValue();
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


  public int getBegAddr()
  {
    return this.begAddr;
  }


  public BreakOption getBreakOption()
  {
    return this.breakOption;
  }


  public boolean getCheckBounds()
  {
    return this.checkBounds;
  }


  public boolean getCheckStack()
  {
    return this.checkStack;
  }


  public EmuSys getEmuSys()
  {
    return this.emuSys;
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


  public void setAppName( String appName )
  {
    this.appName = appName;
  }


  public void setBegAddr( int value )
  {
    this.begAddr = value;
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


  public void setEmuSys( EmuSys emuSys )
  {
    this.emuSys = emuSys;
  }


  public void setHeapSize( int value )
  {
    this.heapSize = value;
  }


  public void setLangCode( String langCode )
  {
    this.langCode = langCode;
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
	    && (options.begAddr             == this.begAddr)
	    && (options.heapSize            == this.heapSize)
	    && (options.stackSize           == this.stackSize)
	    && (options.checkStack          == this.checkStack)
	    && (options.checkBounds         == this.checkBounds)
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
		"jkcemu.programming.basic.address.begin",
                Integer.toString( this.begAddr ) );

      props.setProperty(
		"jkcemu.programming.basic.heap.size",
                Integer.toString( this.heapSize ) );

      props.setProperty(
		"jkcemu.programming.basic.bounds.check",
                Boolean.toString( this.checkBounds ) );

      props.setProperty(
		"jkcemu.programming.basic.stack.check",
                Boolean.toString( this.checkStack ) );

      props.setProperty(
		"jkcemu.programming.basic.stack.size",
                Integer.toString( this.stackSize ) );

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

