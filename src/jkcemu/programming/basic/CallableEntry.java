/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer einen Struktureintrag eines aufrufbaren Unterprogramms
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.util.*;


public abstract class CallableEntry extends StructureEntry
{
  private String                 name;
  private String                 label;
  private int                    firstCallSourceLineNum;
  private long                   firstCallBasicLineNum;
  private java.util.List<String> args;
  private java.util.List<String> vars;
  private Map<String,Integer>    var2SourceLineNum;
  private Map<String,Long>       var2BasicLineNum;
  private Set<String>            usedVars;
  private Map<String,Integer>    name2iyOffs;
  private boolean                implemented;
  private boolean                stackFrame;


  protected CallableEntry(
		int    sourceLineNum,
		long   basicLineNum,
		String name,
		String label )
  {
    super( sourceLineNum, basicLineNum );
    this.name                   = name;
    this.label                  = label;
    this.firstCallSourceLineNum = 0;
    this.firstCallBasicLineNum  = -1;
    this.args                   = new ArrayList<String>();
    this.vars                   = new ArrayList<String>();
    this.var2SourceLineNum      = new HashMap<String,Integer>();
    this.var2BasicLineNum       = new HashMap<String,Long>();
    this.usedVars               = new TreeSet<String>();
    this.name2iyOffs            = new HashMap<String,Integer>();
    this.implemented            = false;
    this.stackFrame             = false;
  }


  public void addVar(
		int    sourceLineNum,
		long   basicLineNum,
		String varName )
  {
    this.name2iyOffs.put(
		varName,
		new Integer( getVarIYOffs( this.vars.size() ) ) );
    this.var2SourceLineNum.put( varName, new Integer( sourceLineNum ) );
    this.var2BasicLineNum.put( varName, new Long( basicLineNum ) );
    this.vars.add( varName );
  }


  public boolean equalsArgType( int idx, String argName )
  {
    return (idx >= 0) && (idx < this.args.size()) ?
	(this.args.get( idx ).endsWith( "$" ) == argName.endsWith( "$" ))
	: false;
  }


  public int getArgCount()
  {
    return this.args.size();
  }


  public int getArgIYOffs( int idx, int nArgs )
  {
    return 4 + ((nArgs - idx - 1) * 2);
  }


  public BasicCompiler.DataType getArgType( int idx )
  {
    return this.args.get( idx ).endsWith( "$" ) ?
					BasicCompiler.DataType.STRING
					: BasicCompiler.DataType.INTEGER;
  }


  public long getFirstCallBasicLineNum()
  {
    return this.firstCallBasicLineNum;
  }


  public int getFirstCallSourceLineNum()
  {
    return this.firstCallSourceLineNum;
  }


  public Integer getIYOffs( String varName )
  {
    return this.name2iyOffs.get( varName );
  }


  public String getLabel()
  {
    return this.label;
  }


  public String getName()
  {
    return this.name;
  }


  public int getTotalArgSize()
  {
    return this.args.size() * 2;
  }


  public int getTotalVarSize()
  {
    return this.vars.size() * 2;
  }


  public int getVarCount()
  {
    return this.vars.size();
  }


  public int getVarIYOffs( int idx )
  {
    return -(idx * 2) - 2;
  }


  public long getVarBasicLineNum( String varName )
  {
    long rv = -1;
    if( varName != null ) {
      Long lineObj = this.var2BasicLineNum.get( varName );
      if( lineObj != null ) {
	rv = lineObj.longValue();
      }
    }
    return rv;
  }


  public String getVarName( int idx )
  {
    return (idx >= 0) && (idx < this.vars.size()) ?
					this.vars.get( idx )
					: null;
  }


  public int getVarSourceLineNum( String varName )
  {
    int rv = -1;
    if( varName != null ) {
      Integer lineObj = this.var2SourceLineNum.get( varName );
      if( lineObj != null ) {
	rv = lineObj.intValue();
      }
    }
    return rv;
  }


  public BasicCompiler.DataType getVarType( int idx )
  {
    return this.vars.get( idx ).endsWith( "$" ) ?
					BasicCompiler.DataType.STRING
					: BasicCompiler.DataType.INTEGER;
  }


  public boolean hasStackFrame()
  {
    return this.stackFrame;
  }


  public boolean isCalled()
  {
    return (this.firstCallSourceLineNum > 0);
  }


  public boolean isImplemented()
  {
    return this.implemented;
  }


  public boolean isVarUsed( String varName )
  {
    return varName != null ? this.usedVars.contains( varName ) : false;
  }


  public void putCallLineNum(
			int  sourceLineNum,
			long basicLineNum )
  {
    if( this.firstCallSourceLineNum <= 0 ) {
      this.firstCallSourceLineNum = sourceLineNum;
      this.firstCallBasicLineNum  = basicLineNum;
    }
  }


  public void setArg( int idx, String argName )
  {
    if( (idx >= 0) && (idx < this.args.size()) ) {
      this.name2iyOffs.remove( this.args.get( idx ) );
      this.name2iyOffs.put(
		argName,
		new Integer( getArgIYOffs( idx, this.args.size() ) ) );
      this.args.set( idx, argName );
    }
  }


  public void setArgs( java.util.List<String> args )
  {
    this.args.clear();
    if( args != null ) {
      int nArgs = args.size();
      if( nArgs > 0 ) {
	for( String arg : args ) {
	  this.name2iyOffs.put(
		arg,
		new Integer( getArgIYOffs( this.args.size(), nArgs ) ) );
	  this.args.add( arg );
	}
      }
    }
  }


  public void setImplemented()
  {
    this.implemented = true;
  }


  public void setStackFrameCreated()
  {
    this.stackFrame = true;
  }


  public void setVarUsed( String varName )
  {
    if( varName != null )
      this.usedVars.add( varName );
  }
}
