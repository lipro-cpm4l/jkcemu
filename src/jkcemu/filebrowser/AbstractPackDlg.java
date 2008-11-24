/*
 * (c) 2008 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Datei-Browser
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.BasicDlg;


public abstract class AbstractPackDlg extends BasicDlg implements Runnable
{
  protected FileBrowserFrm fileBrowserFrm;
  protected boolean        canceled;

  private Thread       thread;
  private int          errorCount;
  private JTextArea    fldLog;
  private JProgressBar progressBar;
  private JButton      btnClose;


  protected AbstractPackDlg( FileBrowserFrm fileBrowserFrm )
  {
    super( fileBrowserFrm, Dialog.ModalityType.MODELESS );
    this.fileBrowserFrm = fileBrowserFrm;
    this.canceled       = false;
    this.thread         = new Thread( this );
    this.errorCount     = 0;


    // Fensterinhalt
    Container contentPane = getContentPane();
    contentPane.setLayout( new GridBagLayout() );
    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.fldLog = new JTextArea( 10, 32 );
    this.fldLog.setEditable( false );
    contentPane.add(
		new JScrollPane(
			this.fldLog,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED ),
		gbc );

    this.progressBar = new JProgressBar( JProgressBar.HORIZONTAL );
    this.progressBar.setBorderPainted( true );
    this.progressBar.setStringPainted( false );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.gridy++;
    contentPane.add( this.progressBar, gbc );

    this.btnClose = new JButton( "Abbrechen" );
    this.btnClose.addActionListener( this );
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridy++;
    contentPane.add( this.btnClose, gbc );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    this.fldLog.setColumns( 0 );
    this.fldLog.setRows( 0 );


    // Starten des Threads veranlassen
    final Thread thread = this.thread;
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    thread.start();
		  }
		} );
  }


  protected void appendToLog( final String msg )
  {
    if( !this.canceled ) {
      final JTextArea fld = this.fldLog;
      SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    fld.append( msg );
		  }
		} );
		    
    }
  }


  protected static void close( Closeable io )
  {
    if( io != null ) {
      try {
        io.close();
      }
      catch( IOException ex ) {}
    }
  }


  protected abstract void doProgress();


  protected void incErrorCount()
  {
    this.errorCount++;
  }


  /*
   * Die Methode oeffnet eine Datei zum Lesen.
   * Das Argument "nReads" gibt an, wie oft die Datei gelesen werden soll.
   * Das ist wichtig fuer die korrekte Funktion des Fortschrittbalkens.
   */
  protected FileProgressInputStream openInputFile(
					File file,
					int  nReads ) throws IOException
  {
    return new FileProgressInputStream( file, this.progressBar, nReads );
  }


	/* --- Runnable --- */

  public void run()
  {
    doProgress();
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    progressFinished();
		  }
		} );
  }


	/* --- ueberschriebene Methoden --- */

  public boolean doAction( EventObject e )
  {
    boolean matched = false;
    if( e.getSource() == this.btnClose ) {
      matched       = true;
      this.canceled = true;
      if( !this.thread.isAlive() ) {
	doClose();
      }
    }
    return matched;
  }


	/* --- private Methoden --- */

  private void progressFinished()
  {
    if( !this.canceled && (this.errorCount > 0) ) {
      this.progressBar.setMinimum( 0 );
      this.progressBar.setMaximum( 1 );
      this.progressBar.setValue( 0 );
      this.btnClose.setText( "Schlie\u00DFen" );
      this.fldLog.append(
		"\n" + String.valueOf( this.errorCount ) + " Fehler\n" );
    } else {
      doClose();
    }
  }
}

