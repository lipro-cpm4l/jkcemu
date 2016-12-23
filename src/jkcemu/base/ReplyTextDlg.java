/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Eingabe-Dialog fuer einen Text
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.Window;
import java.lang.*;


public class ReplyTextDlg extends AbstractReplyDlg
{
  private ReplyTextDlg(
		Window owner,
		String msg,
		String title,
		String defaultReply )
  {
    super( owner, msg, title, defaultReply );
    fitWindowBounds();
  }


  public static String showReplyTextDlg(
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
