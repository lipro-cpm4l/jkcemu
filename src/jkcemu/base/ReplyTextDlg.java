/*
 * (c) 2008-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Eingabe-Dialog fuer einen Text
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.Window;


public class ReplyTextDlg extends AbstractReplyDlg
{
  protected ReplyTextDlg(
		Window owner,
		String msg,
		String title,
		String defaultReply )
  {
    super( owner, msg, title, defaultReply );
    fitWindowBounds();
  }


  public static String showDlg(
			Component owner,
			String    msg,
			String    title,
			String    defaultReply )
  {
    String       reply       = null;
    ReplyTextDlg dlg         = null;
    Window       ownerWindow = EmuUtil.getWindow( owner );
    if( ownerWindow != null ) {
      dlg = new ReplyTextDlg( ownerWindow, msg, title, defaultReply );
    }
    if( dlg != null ) {
      dlg.setVisible( true );
      reply = dlg.getReplyText();
    }
    return reply;
  }
}
