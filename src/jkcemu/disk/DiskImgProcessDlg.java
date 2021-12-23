/*
 * (c) 2009-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Anzeige waehrend einer Aktion mir einer Abbilddatei
 */

package jkcemu.disk;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.EventObject;
import java.util.zip.GZIPInputStream;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.DeviceIO;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.file.FileUtil;
import jkcemu.text.TextUtil;


public class DiskImgProcessDlg extends BaseDlg implements Runnable
{
  private enum Direction { FILE_TO_DISK, DISK_TO_FILE };

  private Direction         direction;
  private String            drvFileName;
  private File              imgFile;
  private StringBuilder     statusBuf;
  private char[]            runningBuf;
  private int               runningIdx;
  private JLabel            labelMsg;
  private JLabel            labelStatus;
  private JLabel            labelRunning;
  private JButton           btnCancel;
  private javax.swing.Timer timer;
  private volatile Thread   thread;
  private volatile long     nBytesProcessed;
  private long              lastBytesProcessed;


  public static void createDiskImageFromDrive( Frame owner )
  {
    Object drive = DriveSelectDlg.selectDrive(
				owner,
				DeviceIO.MediaType.ANY_DISK_READ_ONLY );
    if( drive != null ) {
      String  drvFileName = null;
      Boolean cdrom       = null;
      long    diskSize    = -1;
      boolean specialPriv = false;
      if( drive instanceof DeviceIO.Drive ) {
	drvFileName = ((DeviceIO.Drive) drive).getFileName();
	cdrom       = ((DeviceIO.Drive) drive).getCDRom();
	diskSize    = ((DeviceIO.Drive) drive).getDiskSize();
	specialPriv = ((DeviceIO.Drive) drive).needsSpecialPrivileges();
      } else {
	drvFileName = drive.toString();
      }
      FileFilter[] fileFilters = null;
      if( cdrom != null ) {
	fileFilters      = new FileFilter[ 1 ];
	fileFilters = new FileFilter[] {
			cdrom.booleanValue() ?
				FileUtil.getISOFileFilter()
				: FileUtil.getPlainDiskFileFilter() };
      } else {
	fileFilters = new FileFilter[] {
				FileUtil.getPlainDiskFileFilter(),
				FileUtil.getISOFileFilter() };
      }
      if( drvFileName != null ) {
	File imgFile = FileUtil.showFileSaveDlg(
				owner,
				"Einfache Abbilddatei speichern",
				Main.getLastDirFile( Main.FILE_GROUP_DISK ),
				fileFilters );
	if( imgFile != null ) {
	  if( DiskUtil.checkFileExt(
				owner,
				imgFile,
				DiskUtil.plainDiskFileExt,
				DiskUtil.gzPlainDiskFileExt,
				DiskUtil.isoFileExt,
				DiskUtil.gzISOFileExt ) )
	  {
	    StringBuilder buf = new StringBuilder( 256 );
	    buf.append( "Erzeuge Abbilddatei" );
	    String displyDriveName = getDisplayDriveName( drvFileName );
	    if( displyDriveName != null ) {
	      buf.append( " von " );
	      buf.append( displyDriveName );
	    }
	    buf.append( "..." );
	    (new DiskImgProcessDlg(
				owner,
				buf.toString(),
				Direction.DISK_TO_FILE,
				drvFileName,
				imgFile )).setVisible( true );
	  }
	}
      }
    }
  }


  public static void writeDiskImageToDrive( Frame owner )
  {
    File imgFile = FileUtil.showFileOpenDlg(
				owner,
				"Einfache Abbilddatei \u00F6ffnen",
				Main.getLastDirFile( Main.FILE_GROUP_DISK ),
				FileUtil.getPlainDiskFileFilter(),
				FileUtil.getISOFileFilter() );
    if( imgFile != null ) {
      if( imgFile.exists() && (imgFile.length() == 0) ) {
	showErrorDlg( owner, "Die Datei ist leer." );
      } else {
	boolean state    = false;
	String  fileName = imgFile.getName();
	if( fileName != null ) {
	  String lowerFileName = fileName.toLowerCase();
	  state = (TextUtil.endsWith(
				lowerFileName,
				DiskUtil.plainDiskFileExt )
			|| TextUtil.endsWith(
				lowerFileName,
				DiskUtil.gzPlainDiskFileExt )
			|| TextUtil.endsWith(
				lowerFileName,
				DiskUtil.isoFileExt )
			|| TextUtil.endsWith(
				lowerFileName,
				DiskUtil.gzISOFileExt ));
	}
	if( !state ) {
	  state = showYesNoWarningDlg(
			owner,
			"JKCEMU kann nur einfache Abbilddateien"
				+ " auf einen Datentr\u00E4ger schreiben.\n"
				+ "Laut Dateiendung scheint die Datei jedoch"
				+ " keine einfache Abbilddatei zu sein.\n"
				+ "M\u00F6chten Sie trotzdem fortsetzen?",
			"Dateityp" );
	}
	if( state ) {
	  Object drive = DriveSelectDlg.selectDrive(
				owner,
				DeviceIO.MediaType.ANY_DISK_READ_ONLY );
	  if( drive != null ) {
	    String drvFileName  = null;
	    boolean specialPriv = false;
	    if( drive instanceof DeviceIO.Drive ) {
	      drvFileName = ((DeviceIO.Drive) drive).getFileName();
	      specialPriv = ((DeviceIO.Drive) drive).needsSpecialPrivileges();
	    } else {
	      drvFileName = drive.toString();
	    }
	    if( drvFileName != null ) {
	      StringBuilder buf = new StringBuilder( 0x100 );
	      buf.append( "Die Abbilddatei wird nun"
				+ " auf den Datentr\u00E4ger" );
	      String displayDriveName = getDisplayDriveName( drvFileName );
	      if( displayDriveName != null ) {
		buf.append( " im Laufwerk " );
		buf.append( displayDriveName );
	      }
	      buf.append( " geschrieben.\n"
			+ "Dabei werden alle bisherigen Daten"
			+ " auf dem Datentr\u00E4ger gel\u00F6scht!"
			+ "\n\nIst der Datentr\u00E4ger im"
			+ " Dateisystem eingeh\u00E4ngt,"
			+ " wird er nun ausgeh\u00E4ngt." );
	      if( JOptionPane.showConfirmDialog(
			owner,
			buf.toString(),
			"Achtung",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE )
					== JOptionPane.OK_OPTION )
	      {
		buf.setLength( 0 );
		buf.append( "Schreibe Abbilddatei" );
		if( displayDriveName != null ) {
		  buf.append( " auf " );
		  buf.append( displayDriveName );
		}
		buf.append( "..." );
		(new DiskImgProcessDlg(
				owner,
				buf.toString(),
				Direction.FILE_TO_DISK,
				drvFileName,
				imgFile )).setVisible( true );
	      }
	    }
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
	    in  = DeviceIO.openDeviceForSequentialRead( this.drvFileName );
	    out = FileUtil.createOptionalGZipOutputStream( this.imgFile );
	    Main.setLastDriveFileName( this.drvFileName );
	    Main.setLastFile( this.imgFile, Main.FILE_GROUP_DISK );

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
	    EmuUtil.closeSilently( out );
	    EmuUtil.closeSilently( in );
	  }
	  if( this.thread == null ) {
	    this.imgFile.delete();
	  }
	}
	catch( EOFException ex ) {}
	catch( Exception ex ) {
	  retEx = ex;
	}
	break;

      case FILE_TO_DISK:
	try {
	  InputStream  in  = null;
	  OutputStream out = null;
	  try {
	    long len = this.imgFile.length();
	    in       = new FileInputStream( this.imgFile );
	    if( FileUtil.isGZipFile( this.imgFile ) ) {
	      in = new GZIPInputStream( in );
	    }
	    if( (len > (2880 * 1024)) && ((len % 512L) >= 0x100) ) {
	      // kompatible Festplattenabbilddatei -> Dateikopf ueberspringen
	      for( int i = 0; i < 0x100; i++ ) {
		if( in.read() < 0 ) {
		  break;
		}
	      }
	    }
	    out = DeviceIO.openDeviceForSequentialWrite( this.drvFileName );
	    Main.setLastDriveFileName( this.drvFileName );
	    Main.setLastFile( this.imgFile, Main.FILE_GROUP_DISK );

	    byte[] buf = new byte[ 0x4000 ];
	    int    n   = EmuUtil.read( in, buf );
	    while( n > 0 ) {
	      if( n < buf.length ) {
		/*
		 * auf max. Sektorgroesse (2048 Bytes)
		 * mit Null-Bytes auffuellen
		 */
		while( (n < buf.length) && ((n & 0x7FF) != 0) ) {
		  buf[ n++ ] = (byte) 0;
		}
		out.write( buf, 0, n );
		break;
	      }
	      out.write( buf, 0, n );
	      this.nBytesProcessed += n;
	      if( this.thread == null ) {
		break;
	      }
	      n = EmuUtil.read( in, buf );
	    }
	    out.close();
	    out = null;
	  }
	  finally {
	    EmuUtil.closeSilently( out );
	    EmuUtil.closeSilently( in );
	  }
	}
	catch( Exception ex ) {
	  retEx = ex;
	}
	break;
    }
    fireThreadFinished( retEx );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.btnCancel ) {
      doCancel();
      rv = true;
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
      this.btnCancel.removeActionListener( this );
      this.timer.stop();
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private DiskImgProcessDlg(
			Window    owner,
			String    msg,
			Direction direction,
			String    drvFileName,
			File      imgFile )
  {
    super( owner, "Abbilddatei" );
    this.direction          = direction;
    this.drvFileName        = drvFileName;
    this.imgFile            = imgFile;
    this.statusBuf          = new StringBuilder( 80 );
    this.runningBuf         = new char[ 10 ];
    this.runningIdx         = 0;
    this.nBytesProcessed    = 0;
    this.lastBytesProcessed = -1;
    Arrays.fill( this.runningBuf, '*' );

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.labelMsg = GUIFactory.createLabel( msg );
    add( this.labelMsg, gbc );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Bitte warten!" ), gbc );

    this.labelStatus = GUIFactory.createLabel();
    gbc.insets.top   = 10;
    gbc.gridy++;
    add( this.labelStatus, gbc );

    this.labelRunning = GUIFactory.createLabel();
    this.labelRunning.setFont( new Font( Font.MONOSPACED, Font.BOLD, 12 ) );
    gbc.gridy++;
    add( this.labelRunning, gbc );

    this.btnCancel = GUIFactory.createButtonCancel();
    gbc.gridy++;
    add( this.btnCancel, gbc );

    updStatusText();
    pack();
    setParentCentered();

    this.timer = new javax.swing.Timer(
			200,
			new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    updStatusText();
			  }
			} );
    this.timer.start();
 
    this.thread = new Thread(
			Main.getThreadGroup(),
			this,
			direction == Direction.FILE_TO_DISK ?
				"JKCEMU disk image writer"
				: "JKCEMU disk image reader" );
    this.thread.start();


    // Listener
    this.btnCancel.addActionListener( this );
  }


	/* --- private Methoden --- */

  private void doCancel()
  {
    this.thread = null;
    this.labelMsg.setText( "Vorgang wird abgebrochen..." );
    this.btnCancel.setEnabled( false );
  }


  private static String getDisplayDriveName( String drvFileName )
  {
    String rv = null;
    if( drvFileName.startsWith( "\\\\.\\" ) ) {
      if( (drvFileName.length() == 6)
	  && drvFileName.endsWith( ":" ) )
      {
	rv = drvFileName.substring( 4 );
      }
    } else {
      rv = drvFileName;
    }
   return rv;
  }


  private void fireThreadFinished( final Exception ex )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
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
    } else {
      if( this.thread != null ) {
	String msg = "Fertig!";
	if( (this.direction == Direction.DISK_TO_FILE)
	    && (this.imgFile != null) )
	{
	  if( this.imgFile.exists() ) {
	    msg = "Die Abbilddatei wurde erfolgreich erzeugt:\n"
					+ this.imgFile.getPath();
	  }
	}
	showInfoDlg( this, msg );
      }
    }
    this.thread = null;
    doClose();
  }


  private void updStatusText()
  {
    long nBytes = this.nBytesProcessed;
    if( (this.lastBytesProcessed < 0)
	|| (nBytes != this.lastBytesProcessed) )
    {
      // Anzahl gelesene bzw. geschriebene Bytes
      this.statusBuf.setLength( 0 );
      EmuUtil.appendSizeText( this.statusBuf, nBytes, true, false );
      switch( this.direction ) {
	case DISK_TO_FILE:
	  this.statusBuf.append( " gelesen" );
	  break;

	case FILE_TO_DISK:
	  this.statusBuf.append( " geschrieben" );
	  break;
      }
      this.labelStatus.setText( this.statusBuf.toString() );
      this.lastBytesProcessed = nBytes;

      // Bewegungsanzeige
      if( this.runningBuf[ this.runningIdx ] == '*' ) {
	this.runningBuf[ this.runningIdx ] = '\u0020';
	if( this.runningIdx == (this.runningBuf.length - 1) ) {
	  this.runningIdx = 0;
	  this.runningBuf[ this.runningIdx ] = '*';
	}
      } else {
	this.runningBuf[ this.runningIdx ] = '*';
      }
      this.runningIdx++;
      if( this.runningIdx >= this.runningBuf.length ) {
	this.runningIdx = 0;
      }
      this.labelRunning.setText( new String( this.runningBuf ) );
    }
  }
}
