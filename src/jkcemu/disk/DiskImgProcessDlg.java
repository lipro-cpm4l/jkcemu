/*
 * (c) 2009-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Anzeige waehrend einer Aktion mir einer Abbilddatei
 */

package jkcemu.disk;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.EventObject;
import java.util.zip.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class DiskImgProcessDlg extends BasicDlg implements Runnable
{
  private enum Direction { FILE_TO_DISK, DISK_TO_FILE };

  private Direction         direction;
  private String            drvFileName;
  private File              imgFile;
  private StringBuilder     statusBuf;
  private JLabel            labelMsg;
  private JLabel            labelStatus;
  private JButton           btnCancel;
  private javax.swing.Timer timer;
  private volatile Thread   thread;
  private volatile long     nBytesProcessed;


  public static void createDiskImageFromDrive( Frame owner )
  {
    String drvFileName = DriveSelectDlg.selectDriveFileName( owner );
    if( drvFileName != null ) {
      File imgFile = EmuUtil.showFileSaveDlg(
				owner,
				"Einfache Diskettenabbilddatei speichern",
				Main.getLastPathFile( "disk" ),
				EmuUtil.getPlainDiskFileFilter() );
      if( imgFile != null ) {
	if( DiskUtil.checkFileExt(
				owner,
				imgFile,
				DiskUtil.plainDiskFileExt,
				DiskUtil.gzPlainDiskFileExt ) )
	{
	  (new DiskImgProcessDlg(
			owner,
			String.format(
				"Erzeuge Abbilddatei von %s...",
				getDriveName( drvFileName ) ),
			Direction.DISK_TO_FILE,
			drvFileName,
			imgFile )).setVisible( true );
	}
      }
    }
  }


  public static void writeDiskImageToDrive( Frame owner )
  {
    File imgFile = EmuUtil.showFileOpenDlg(
				owner,
				"Einfach Diskettenabbilddatei \u00F6ffnen",
				Main.getLastPathFile( "disk" ),
				EmuUtil.getPlainDiskFileFilter() );
    if( imgFile != null ) {
      boolean state    = false;
      String  fileName = imgFile.getName();
      if( fileName != null ) {
	state = EmuUtil.endsWith(
				fileName.toLowerCase(),
				DiskUtil.plainDiskFileExt,
				DiskUtil.gzPlainDiskFileExt );
      }
      if( !state ) {
	state = BasicDlg.showYesNoWarningDlg(
			owner,
			"JKCEMU kann nur einfache Abbilddateien"
				+ " auf Diskette schreiben.\n"
				+ "Laut Dateiendung scheint die Datei jedoch"
				+ " keine einfache Abbilddatei zu sein.\n"
				+ "M\u00F6chten Sie trotzdem fortsetzen?",
			"Dateityp" );
      }
      if( state ) {
	String drvFileName = DriveSelectDlg.selectDriveFileName( owner );
	if( drvFileName != null ) {
	  String drvName = getDriveName( drvFileName );
	  if( JOptionPane.showConfirmDialog(
		owner,
		String.format(
			"Die Abbilddatei wird nun auf das Laufwerk %s"
				+ " geschrieben.\n"
				+ "Dabei werden alle bisherigen Daten"
				+ " auf dem Datentr\u00E4ger gel\u00F6scht!",
			drvName ),
		"Achtung",
		JOptionPane.OK_CANCEL_OPTION,
		JOptionPane.WARNING_MESSAGE ) == JOptionPane.OK_OPTION )
	  {
	    (new DiskImgProcessDlg(
			owner,
			String.format(
				"Schreibe Abbilddatei auf %s...",
				drvName ),
			Direction.FILE_TO_DISK,
			drvFileName,
			imgFile )).setVisible( true );
	  }
	}
      }
    }
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    Exception retEx = null;
    switch( this.direction ) {
      case DISK_TO_FILE:
	try {
	  InputStream  in  = null;
	  OutputStream out = null;
	  try {
	    in  = new FileInputStream( this.drvFileName );
	    out = new FileOutputStream( this.imgFile );
	    if( EmuUtil.isGZipFile( this.imgFile ) ) {
	      out = new GZIPOutputStream( out );
	    }
	    Main.setLastDriveFileName( this.drvFileName );

	    byte[] buf = new byte[ 2048 ];	// max. Sektorgroesse
	    int    n   = in.read( buf );
	    while( n > 0 ) {
	      out.write( buf, 0, n );
	      this.nBytesProcessed += n;
	      if( this.thread == null ) {
		break;
	      }
	      n = in.read( buf );
	    }
	    out.close();
	    out = null;
	  }
	  finally {
	    EmuUtil.doClose( out );
	    EmuUtil.doClose( in );
	  }
	  if( this.thread == null ) {
	    this.imgFile.delete();
	  }
	}
	catch( IOException ex ) {
	  /* Windows meldet beim Lesen hinter dem Diskettenende
	   * einen Fehler.
	   * Aus diesem Grund wird einfach geprueft,
	   * ob die Anzahl der gelesenen Bytes einer ueblichen
	   * Diskettengroesse entspricht, und wenn ja,
	   * wird der Fehler ignoriert.
	   */
	  if( (File.separatorChar == '/')
	      || ((this.nBytesProcessed != (720 * 1024))
		  && (this.nBytesProcessed != (800 * 1024))
		  && (this.nBytesProcessed != (1200 * 1024))
		  && (this.nBytesProcessed != (1440 * 1024))
		  && (this.nBytesProcessed != (2880 * 1024))) )
	  {
	    retEx = ex;
	  }
	}
	break;

      case FILE_TO_DISK:
	try {
	  InputStream  in  = null;
	  OutputStream out = null;
	  try {
	    in  = new FileInputStream( this.imgFile );
	    if( EmuUtil.isGZipFile( this.imgFile ) ) {
	      in = new GZIPInputStream( in );
	    }
	    out = DeviceIO.openDeviceForSequentialWrite( this.drvFileName );
	    Main.setLastDriveFileName( this.drvFileName );

	    byte[] buf = new byte[ 2048 ];	// max. Sektorgroesse
	    int    n   = in.read( buf );
	    while( n > 0 ) {
	      out.write( buf, 0, n );
	      this.nBytesProcessed += n;
	      if( this.thread == null ) {
		break;
	      }
	      n = in.read( buf );
	    }
	    out.close();
	    out = null;
	  }
	  finally {
	    EmuUtil.doClose( out );
	    EmuUtil.doClose( in );
	  }
	}
	catch( Exception ex ) {
	  retEx = ex;
	}
	break;
    }
    this.thread = null;
    fireThreadFinished( retEx );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.btnCancel ) {
	doCancel();
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = false;
    if( this.thread != null ) {
      if( showYesNoDlg(
		this,
		"M\u00F6chten Sie den Vorgang abbrechen?" ) )
      {
	doCancel();
      }
    }
    if( this.thread == null ) {
      rv = super.doClose();
    }
    if( rv ) {
      this.timer.stop();
    }
    return rv;
  }


	/* --- private Konstruktoren und Methoden --- */

  private DiskImgProcessDlg(
			Window    owner,
			String    msg,
			Direction direction,
			String    drvFileName,
			File      imgFile )
  {
    super( owner, "Abbilddatei" );
    this.direction       = direction;
    this.drvFileName     = drvFileName;
    this.imgFile         = imgFile;
    this.statusBuf       = new StringBuilder( 80 );
    this.nBytesProcessed = 0;

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.labelMsg = new JLabel( msg );
    add( this.labelMsg, gbc );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( new JLabel( "Bitte warten!" ), gbc );

    this.labelStatus = new JLabel();
    gbc.insets.top   = 20;
    gbc.gridy++;
    add( this.labelStatus, gbc );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    gbc.gridy++;
    add( this.btnCancel, gbc );

    updStatusText();
    pack();
    setParentCentered();

    this.timer = new javax.swing.Timer(
			500,
			new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    updStatusText();
			  }
			} );
    this.timer.start();
 
    this.thread = new Thread(
			this,
			direction == Direction.FILE_TO_DISK ?
				"JKCEMU disk image writer"
				: "JKCEMU disk image reader" );
    this.thread.start();
  }


  private void doCancel()
  {
    this.thread = null;
    this.labelMsg.setText( "Vorgang wird abgebrochen..." );
    this.btnCancel.setEnabled( false );
  }


  private static String getDriveName( String drvFileName )
  {
    return drvFileName.startsWith( "\\\\.\\" ) && (drvFileName.length() > 4) ?
						drvFileName.substring( 4 )
						: drvFileName;
  }


  private void fireThreadFinished( final Exception ex )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    threadFinished( ex );
		  }
		} );
  }


  private void threadFinished( final Exception ex )
  {
    if( ex != null ) {
      showErrorDlg( this, ex );
    }
    doClose();
  }


  private void updStatusText()
  {
    this.statusBuf.setLength( 0 );
    EmuUtil.appendSizeText(
		this.statusBuf,
		this.nBytesProcessed,
		true,
		false );
    switch( this.direction ) {
      case DISK_TO_FILE:
	this.statusBuf.append( " gelesen" );
	break;

      case FILE_TO_DISK:
	this.statusBuf.append( " geschrieben" );
	break;
    }
    this.labelStatus.setText( this.statusBuf.toString() );
  }
}

