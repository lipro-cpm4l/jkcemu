/*
 * (c) 2008-2018 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse zur Anzeige einer Fehlermeldung aus einem Nicht-Swing-Thread
 *
 * Die Klasse verhindert die gleichzeitige Anzeige
 * gleichlautender Fehlermeldungen
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.EventQueue;
import java.util.Set;
import java.util.TreeSet;


public class ErrorMsg implements Runnable
{
  private static Set<String> pendingMessages = new TreeSet<>();

  private Component owner;
  private String    msg;


  public static void showLater( Component owner, Exception ex )
  {
    showLater( owner, null, ex );
  }


  public static void showLater( Component owner, String msg, Exception ex )
  {
    msg = EmuUtil.createErrorMsg( msg, ex );
    if( !pendingMessages.contains( msg ) ) {
      EventQueue.invokeLater( new ErrorMsg( owner, msg ) );
    }
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    BaseDlg.showErrorDlg( this.owner, this.msg );
    pendingMessages.remove( msg );
  }


	/* --- Konstruktor --- */

  private ErrorMsg( Component owner, String msg )
  {
    this.owner = owner;
    this.msg   = msg;
  }
}
