/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Eingabe-Dialog fuer eine Hexadezimalzahl
 */

package jkcemu.base;

import java.awt.Frame;
import java.lang.*;


public class ReplyHexDlg extends AbstractReplyDlg
{
  private HexDocument docHex;
  private Integer     reply;


  public ReplyHexDlg(
		Frame   owner,
		String  msg,
		int     numDigits,
		Integer value )
  {
    super( owner, msg, "Eingabe Hexadezimalzahl", null );
    this.reply  = null;
    this.docHex = new HexDocument( this.replyTextField, numDigits );
    if( value != null ) {
      this.docHex.setValue( value.intValue(), numDigits );
    }
    this.replyTextField.setColumns( numDigits + 1 );
    fitWindowBounds();
  }


  public Integer getReply()
  {
    return this.reply;
  }


	/* --- ueberschriebende Methoden --- */

  @Override
  protected boolean approveSelection()
  {
    boolean rv = false;
    try {
      this.reply = new Integer( this.docHex.intValue() );
      if( this.reply != null )
	rv = true;
    }
    catch( NumberFormatException ex ) {
      showErrorDlg( this, ex.getMessage() );
    }
    return rv;
  }
}

