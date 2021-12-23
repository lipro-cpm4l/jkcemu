/*
 * (c) 2017-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Informationen ueber eine Operation
 */

package jkcemu.programming.basic;


public class OpInfo
{
  private String               operator;
  private String               asmCodeI2;
  private String               asmCodeI4;
  private String               asmCodeD6;
  private BasicLibrary.LibItem libItemI2;
  private BasicLibrary.LibItem libItemI4;
  private BasicLibrary.LibItem libItemD6;
  private boolean              commutative;


  public OpInfo(
		String               operator,
		String               asmCodeI2,
		BasicLibrary.LibItem libItemI2,
		String               asmCodeI4,
		BasicLibrary.LibItem libItemI4,
		String               asmCodeD6,
		BasicLibrary.LibItem libItemD6,
		boolean              commutative )
  {
    this.operator    = operator;
    this.asmCodeI2   = asmCodeI2;
    this.asmCodeI4   = asmCodeI4;
    this.asmCodeD6   = asmCodeD6;
    this.libItemI2   = libItemI2;
    this.libItemI4   = libItemI4;
    this.libItemD6   = libItemD6;
    this.commutative = commutative;
  }


  public String getAsmCodeI2()
  {
    return this.asmCodeI2;
  }


  public String getAsmCodeI4()
  {
    return this.asmCodeI4;
  }


  public String getAsmCodeD6()
  {
    return this.asmCodeD6;
  }


  public BasicLibrary.LibItem getLibItemI2()
  {
    return this.libItemI2;
  }


  public BasicLibrary.LibItem getLibItemI4()
  {
    return this.libItemI4;
  }


  public BasicLibrary.LibItem getLibItemD6()
  {
    return this.libItemD6;
  }


  public String getOperator()
  {
    return this.operator;
  }


  public boolean isCommutative()
  {
    return this.commutative;
  }
}
