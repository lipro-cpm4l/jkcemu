/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Eingabe-Dialog fuer eine ganze Zahl
 */

package jkcemu.base;

import java.awt.Frame;
import java.lang.*;


public class ReplyIntDlg extends AbstractReplyDlg
{
  private IntegerDocument docInt;
  private Integer         reply;


  public ReplyIntDlg(
		Frame   owner,
		String  msg,
		Integer defaultValue,
		Integer minValue,
		Integer maxValue )
  {
    super( owner, msg, "Eingabe Ganzzahl", null );
    this.reply  = null;
    this.docInt = new IntegerDocument(
				this.replyTextField,
				minValue,
				maxValue );
    this.replyTextField.setColumns( 10 );
    this.docInt.setValue( defaultValue );
    fitWindowBounds();
  }


  public Integer getReply()
  {
    return this.reply;
  }


	/* --- ueberschriebende Methoden --- */

  protected boolean approveSelection()
  {
    boolean rv = false;
    try {
      this.reply = new Integer( this.docInt.intValue() );
      if( this.reply != null )
	rv = true;
    }
    catch( NumberFormatException ex ) {
      showErrorDlg( this, ex.getMessage() );
    }
    return rv;
  }
}

