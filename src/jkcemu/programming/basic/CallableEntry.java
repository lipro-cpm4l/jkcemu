/*
 * (c) 2012-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer einen Struktureintrag eines aufrufbaren Unterprogramms
 */

package jkcemu.programming.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import jkcemu.programming.PrgException;
import jkcemu.programming.PrgSource;


public abstract class CallableEntry extends BasicSourcePos
{
  private String                                 name;
  private String                                 label;
  private BasicSourcePos                         firstCallSourcePos;
  private String[]                               argNames;
  private BasicCompiler.DataType[]               argTypes;
  private int[]                                  argIYOffs;
  private java.util.List<String>                 varNames;
  private java.util.List<BasicCompiler.DataType> varTypes;
  private int[]                                  varIYOffs;
  private Map<String,BasicSourcePos>             var2SourcePos;
  private Set<String>                            varNamesRead;
  private Set<String>                            varNamesWrite;
  private Map<String,Integer>                    name2IYOffs;
  private Map<String,BasicCompiler.DataType>     name2Type;
  private int                                    asmCodePos;
  private int                                    totalArgSize;
  private int                                    totalVarSize;
  private boolean                                implemented;
  private boolean                                stackFrame;


  protected CallableEntry(
		PrgSource source,
		long      basicLineNum,
		String    name,
		String    label )
  {
    super( source, basicLineNum );
    this.name               = name;
    this.label              = label;
    this.firstCallSourcePos = null;
    this.argNames           = null;
    this.argTypes           = null;
    this.argIYOffs          = null;
    this.varNames           = new ArrayList<>();
    this.varTypes           = new ArrayList<>();
    this.varIYOffs          = null;
    this.var2SourcePos      = new HashMap<>();
    this.varNamesRead       = new TreeSet<>();
    this.varNamesWrite      = new TreeSet<>();
    this.name2IYOffs        = null;
    this.name2Type          = null;
    this.asmCodePos         = -1;
    this.totalArgSize       = 0;
    this.totalVarSize       = 0;
    this.implemented        = false;
    this.stackFrame         = false;
  }


  public synchronized void addVar(
			PrgSource              source,
			long                   basicLineNum,
			String                 varName,
			BasicCompiler.DataType varType ) throws PrgException
  {
    if( (this.name2IYOffs != null) || (this.varIYOffs != null) ) {
      throw new RuntimeException(
	"CallableEntry.addVar(...) nach Berechnung der IY-Positionen" );
    }
    this.var2SourcePos.put(
		varName,
		new BasicSourcePos( source, basicLineNum ) );
    this.varNames.add( varName );
    this.varTypes.add( varType );
    this.totalVarSize += BasicUtil.getDataTypeSize( varType );
    if( this.totalVarSize > 128 ) {
      throw new PrgException( "Maximale Gesamtgr\u00F6\u00DFe der"
		+ " lokalen Variablen \u00FCberschritten" );
    }
    this.name2Type = null;
  }


  public int getArgCount()
  {
    return this.argNames != null ? this.argNames.length : 0;
  }


  public synchronized int getArgIYOffs( int idx )
  {
    checkComputeIYOffs();
    return this.argIYOffs[ idx ];
  }


  public synchronized BasicCompiler.DataType getArgOrVarType( String name )
  {
    if( this.name2Type == null ) {
      this.name2Type = new HashMap<>();
      int nArgs = getArgCount();
      for( int i = 0; i < nArgs; i++ ) {
	this.name2Type.put( this.argNames[ i ], this.argTypes[ i ] );
      }
      int nVars = getVarCount();
      for( int i = 0; i < nVars; i++ ) {
	this.name2Type.put(
			this.varNames.get( i ),
			this.varTypes.get( i ) );
      }
    }
    return this.name2Type.get( name );
  }


  public BasicCompiler.DataType getArgType( int idx )
  {
    return this.argTypes[ idx ];
  }


  public int getAsmCodePos()
  {
    return this.asmCodePos;
  }


  public BasicSourcePos getFirstCallSourcePos()
  {
    return this.firstCallSourcePos;
  }


  public synchronized Integer getIYOffs( String name )
  {
    checkComputeIYOffs();
    return this.name2IYOffs != null ? this.name2IYOffs.get( name ) : null;
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
    return this.totalArgSize;
  }


  public int getTotalVarSize()
  {
    return this.totalVarSize;
  }


  public int getVarCount()
  {
    return this.varNames.size();
  }


  public synchronized int getVarIYOffs( int idx )
  {
    checkComputeIYOffs();
    return this.varIYOffs[ idx ];
  }


  public String getVarName( int idx )
  {
    return this.varNames.get( idx );
  }


  public BasicSourcePos getVarSourcePos( String varName )
  {
    return varName != null ? this.var2SourcePos.get( varName ) : null;
  }


  public BasicCompiler.DataType getVarType( int idx )
  {
    return this.varTypes.get( idx );
  }


  public boolean hasStackFrame()
  {
    return this.stackFrame;
  }


  public boolean isCalled()
  {
    return (this.firstCallSourcePos != null);
  }


  public boolean isImplemented()
  {
    return this.implemented;
  }


  public boolean isRead( String varName )
  {
    return this.varNamesRead.contains( varName );
  }


  public boolean isWritten( String varName )
  {
    return this.varNamesWrite.contains( varName );
  }


  public void putCallPos(
			PrgSource source,
			long      basicLineNum )
  {
    if( this.firstCallSourcePos == null )
      this.firstCallSourcePos = new BasicSourcePos( source, basicLineNum );
  }


  public synchronized void setArgs(
		java.util.List<String>                 argNames,
		java.util.List<BasicCompiler.DataType> argTypes )
							throws PrgException
  {
    if( (this.name2IYOffs != null) || (this.argIYOffs != null) ) {
      throw new RuntimeException(
	"CallableEntry.setArgs(...) nach Berechnung der IY-Positionen" );
    }
    try {
      this.argNames = argNames.toArray( new String[ argNames.size() ] );
      this.argTypes = argTypes.toArray(
			new BasicCompiler.DataType[ argTypes.size() ] );
      this.totalArgSize = 0;
      for( BasicCompiler.DataType t : this.argTypes ) {
	this.totalArgSize += BasicUtil.getDataTypeSize( t );
      }
      if( this.totalArgSize > 124 ) {
	throw new PrgException( "Maximale Gesamtgr\u00F6\u00DFe der"
		+ " Argumente \u00FCberschritten" );
      }
      this.name2Type = null;
    }
    catch( ArrayStoreException ex ) {}
  }


  public void setAsmCodePos( int pos )
  {
    this.asmCodePos = pos;
  }


  public void setImplemented()
  {
    this.implemented = true;
  }


  public void setStackFrameCreated()
  {
    this.stackFrame = true;
  }


  public synchronized void updArgName( int idx, String argName )
  {
    if( this.argNames != null ) {
      if( (idx >= 0) && (idx < this.argNames.length) ) {
	this.argNames[ idx ] = argName;
	this.name2Type       = null;
      }
    }
  }


  public void updVarUsage(
			String                   varName,
			BasicCompiler.AccessMode accessMode )
  {
    if( varName != null ) {
      switch( accessMode ) {
	case READ:
	  this.varNamesRead.add( varName );
	  break;
	case WRITE:
	  this.varNamesWrite.add( varName );
	  break;
	case READ_WRITE:
	  this.varNamesRead.add( varName );
	  this.varNamesWrite.add( varName );
	  break;
      }
    }
  }


	/* --- private Methoden --- */

  private void checkComputeIYOffs()
  {
    if( (this.name2IYOffs == null)
	|| (this.argIYOffs == null)
	|| (this.varIYOffs == null) )
    {
      this.name2IYOffs = new HashMap<>();
      this.varIYOffs   = new int[ this.varNames.size() ];
      int offs         = 0;
      for( int i = 0; i < this.varIYOffs.length; i++ ) {
	offs -= BasicUtil.getDataTypeSize( getVarType( i ) );
	this.varIYOffs[ i ] = offs;
	this.name2IYOffs.put( this.varNames.get( i ), offs );
      }
      if( this.argNames != null ) {
	this.argIYOffs = new int[ this.argNames.length ];
	offs           = 4;
	for( int i = this.argNames.length - 1; i >= 0; --i ) {
	  this.argIYOffs[ i ] = offs;
	  this.name2IYOffs.put( this.argNames[ i ], offs );
	  offs += BasicUtil.getDataTypeSize( getArgType( i ) );
	}
      }
    }
  }
}
