/*
 * (c) 2012-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Variablendeklaration
 */

package jkcemu.programming.basic;

import jkcemu.programming.PrgSource;


public class VarDecl extends BasicSourcePos
{
  public static final String LABEL_PREFIX_INT2   = "V_I2_";
  public static final String LABEL_PREFIX_INT4   = "V_I4_";
  public static final String LABEL_PREFIX_FLOAT4 = "V_F4_";
  public static final String LABEL_PREFIX_DEC6   = "V_D6_";
  public static final String LABEL_PREFIX_STRING = "V_S_";

  private BasicCompiler.DataType dataType;
  private int                    dim1;
  private int                    dim2;
  private int                    nDims;
  private int                    elemSize;
  private int                    totalSize;
  private boolean                read;
  private boolean                written;
  private String                 label;
  private String                 infoText;


  public VarDecl(
		PrgSource              source,
		long                   basicLineNum,
		String                 name,
		BasicCompiler.DataType dataType,
		int                    dim1,
		int                    dim2 )
  {
    super( source, basicLineNum );
    this.dataType = dataType;
    this.dim1     = dim1;
    this.dim2     = dim2;
    this.nDims    = 0;
    this.elemSize = 2;
    this.read     = false;
    this.written  = false;
    this.label    = LABEL_PREFIX_INT2 + name;
    this.infoText = "Variable ";
    switch( this.dataType ) {
      case INT4:
	this.label    = LABEL_PREFIX_INT4 + name;
	this.elemSize = 4;
	break;
      case FLOAT4:
	this.label    = LABEL_PREFIX_FLOAT4 + name;
	this.elemSize = 4;
	break;
      case DEC6:
	this.label    = LABEL_PREFIX_DEC6 + name;
	this.elemSize = 6;
	break;
      case STRING:
	this.label = LABEL_PREFIX_STRING
				+ name.substring( 0, name.length() - 1 );
	this.elemSize = 2;
	break;
    }
    this.totalSize = this.elemSize;
    if( dim1 > 0 ) {
      this.nDims++;
      this.totalSize *= (dim1 + 1);
      if( dim2 > 0 ) {
	this.nDims++;
	this.totalSize *= (dim2 + 1);
      }
    }
    if( this.nDims > 0 ) {
      this.infoText = "Feldvariable ";
    }
    this.infoText += name;
  }


  public VarDecl(
		PrgSource              source,
		long                   basicLineNum,
		String                 name,
		BasicCompiler.DataType dataType )
  {
    this( source, basicLineNum, name, dataType, 0, 0 );
  }


  public BasicCompiler.DataType getDataType()
  {
    return this.dataType;
  }


  public int getDim1()
  {
    return this.dim1;
  }


  public int getDim2()
  {
    return this.dim2;
  }


  public int getDimCount()
  {
    return this.nDims;
  }


  public int getElemSize()
  {
    return this.elemSize;
  }


  public String getLabel()
  {
    return this.label;
  }


  public int getTotalSize()
  {
    return this.totalSize;
  }


  public boolean isRead()
  {
    return this.read;
  }


  public boolean isWritten()
  {
    return this.written;
  }


  public void setRead()
  {
    this.read = true;
  }


  public void setWritten()
  {
    this.written = true;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.infoText;
  }
}
