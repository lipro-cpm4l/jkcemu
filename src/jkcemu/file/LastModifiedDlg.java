/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Setzen des Aenderungszeitpunktes
 */

package jkcemu.file;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.text.DateFormat;
import java.text.ParseException;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.Document;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.PopupMenuOwner;
import jkcemu.text.LogTextActionMngr;


public class LastModifiedDlg
			extends BaseDlg
			implements PopupMenuOwner, Runnable
{
  private java.util.List<Path>        paths;
  private volatile LastModifiedSetter lastModifiedSetter;
  private volatile Thread             thread;
  private LogTextActionMngr           actionMngr;
  private FileTime                    fileTime;
  private int                         numFailed;
  private int                         numTouched;
  private boolean                     cancelled;
  private boolean                     recursive;
  private boolean                     winClosing;
  private DateFormat                  dateFmtStd;
  private DateFormat                  dateFmtShort;
  private JRadioButton                rbCurrentTime;
  private JRadioButton                rbTimeInput;
  private JCheckBox                   cbRecursive;
  private JTextField                  fldTime;
  private JTextArea                   fldLog;
  private JButton                     btnApply;
  private JButton                     btnClose;
  private Map<JCheckBox,String>       cb2VfsSuffix;
  private Set<String>                 archiveFileSuffixes;


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.actionMngr.getPopupMenu();
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    String msg = null;
    try {
      for( Path path : paths ) {
	if( this.cancelled ) {
	  break;
	}
	this.lastModifiedSetter = new LastModifiedSetter(
						path,
						this.fileTime,
						this.recursive,
						this.archiveFileSuffixes );
	this.lastModifiedSetter.exec();
	this.numTouched += this.lastModifiedSetter.getNumTouched();
	this.numFailed  += this.lastModifiedSetter.getNumFailed();
      }
    }
    catch( Exception ex ) {
      msg            = ex.getMessage();
      this.cancelled = true;
    }
    finally {
      final String msg1 = msg;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    threadTerminated( msg1 );
		  }
		} );
      this.lastModifiedSetter = null;
      this.thread             = null;
    }
  }


  /*
   * Rueckgabewert:
   *   true:  Zeitstempel wurde geaendert
   *   false: Zeitstempel nicht geaendert
   */
  public static boolean open(
			Window               owner,
			java.util.List<Path> paths )
  {
    boolean rv = false;
    if( paths != null ) {
      if( !paths.isEmpty() ) {
        LastModifiedDlg dlg = new LastModifiedDlg( owner, paths );
        dlg.setVisible( true );
        if( dlg.numTouched > 0 ) {
	  rv = true;
	}
      }
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( (src == this.rbCurrentTime) || (src == this.rbTimeInput) ) {
      rv = true;
      this.fldTime.setEditable( this.rbTimeInput.isSelected() );
      this.btnApply.setEnabled( true );
    }
    else if( src == this.btnApply ) {
      rv = true;
      doApply();
    }
    else if( src == this.btnClose ) {
      rv = true;
      doClose();
    }
    else if( src instanceof JCheckBox ) {
      rv = true;
      this.btnApply.setEnabled( true );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = false;
    if( this.thread != null ) {
      if( !this.cancelled ) {
	appendToLog( "Verarbeitung wird abgebrochen..." );
	LastModifiedSetter setter = this.lastModifiedSetter;
	if( setter != null ) {
	  setter.cancel();
	}
	this.cancelled = true;
      }
    } else {
      rv = super.doClose();
      if( rv ) {
	this.rbCurrentTime.removeActionListener( this );
	this.rbTimeInput.removeActionListener( this );
	this.fldTime.removeActionListener( this );
	this.cbRecursive.removeActionListener( this );
	this.btnApply.removeActionListener( this );
	this.btnClose.removeActionListener( this );
	this.fldLog.removeMouseListener( this );
	for( JCheckBox cb : this.cb2VfsSuffix.keySet() ) {
	  cb.removeActionListener( this );
	}
      }
    }
    return rv;
  }


  @Override
  protected boolean showPopupMenu( MouseEvent e )
  {
    return this.actionMngr.showPopupMenu( e );
  }


  @Override
  public void windowClosing( WindowEvent e )
  {
    if( e.getSource() == this ) {
      this.winClosing = true;
    }
    super.windowClosing( e );
  }


	/* --- Konstruktor --- */

  private LastModifiedDlg(
		Window               owner,
		java.util.List<Path> paths )
  {
    super( owner, "\u00C4nderungszeitpunkt" );
    this.paths               = paths;
    this.cb2VfsSuffix        = new HashMap<>();
    this.archiveFileSuffixes = null;
    this.lastModifiedSetter  = null;
    this.thread              = null;
    this.fileTime            = null;
    this.numFailed           = 0;
    this.numTouched          = 0;
    this.cancelled           = false;
    this.recursive           = false;
    this.winClosing          = false;
    this.dateFmtStd          = DateFormat.getDateTimeInstance(
						DateFormat.MEDIUM,
						DateFormat.MEDIUM );
    this.dateFmtShort        = DateFormat.getDateTimeInstance(
						DateFormat.MEDIUM,
						DateFormat.SHORT );
    this.dateFmtStd.setLenient( false );
    this.dateFmtShort.setLenient( false );


    // Layout
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					2, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );


    // Fensterinhalt
    add(
	GUIFactory.createLabel( "\u00C4nderungszeitpunkt setzen auf:" ),
	gbc );

    ButtonGroup grpTime = new ButtonGroup();

    this.rbCurrentTime = GUIFactory.createRadioButton(
						"aktuellen Zeitpunkt",
						true );
    grpTime.add( this.rbCurrentTime );
    gbc.insets.left = 50;
    gbc.gridy++;
    add( this.rbCurrentTime, gbc );

    this.rbTimeInput = GUIFactory.createRadioButton( "Datum/Uhrzeit:" );
    grpTime.add( this.rbTimeInput );
    gbc.insets.top = 0;
    gbc.gridwidth  = 1;
    gbc.gridy++;
    add( this.rbTimeInput, gbc );

    this.fldTime = GUIFactory.createTextField();
    this.fldTime.setEditable( false );
    gbc.insets.left = 5;
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.gridx++;
    add( this.fldTime, gbc );

    this.cbRecursive = GUIFactory.createCheckBox(
			"In Verzeichnisse hinein wechseln",
			false );
    gbc.insets.top  = 10;
    gbc.insets.left = 5;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.gridwidth   = 2;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.cbRecursive, gbc );

    /*
     * Dateiendungen ermitteln,
     * fuer die ein virtuelles Dateisystem unterstuetzt wird.
     */
    Set<String> vfsSuffixes = LastModifiedSetter.getVfsSuffix2SchemeMap()
								.keySet();

    // Pruefen, ob Verzeichnisse oder Archivdateien zu durchsuchen sind
    Set<String> foundVfsSuffixes = new TreeSet<>();
    boolean     hasDirs          = false;
    for( Path path : this.paths ) {
      if( Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS ) ) {
	hasDirs = true;
      }
      if( Files.isRegularFile( path, LinkOption.NOFOLLOW_LINKS ) ) {
	String suffix = FileUtil.getLowerFileSuffix( path );
	if( suffix != null ) {
	  if( vfsSuffixes.contains( suffix ) ) {
	    foundVfsSuffixes.add( suffix );
	  }
	}
      }
    }
    this.cbRecursive.setEnabled( hasDirs );

    /*
     * CheckBoxen fuer die Dateiendungen anlegen,
     * fuer die ein virtuelles Dateisystem unterstuetzt wird.
     */
    int nVfsSuffixes = vfsSuffixes.size();
    if( nVfsSuffixes > 0 ) {
      String[] a = vfsSuffixes.toArray( new String[ nVfsSuffixes ] );
      if( a != null ) {
	try {
	  Arrays.sort( a );
	}
	catch( ClassCastException ex ) {}
	for( String s : a ) {
	  JCheckBox cb = GUIFactory.createCheckBox(
		"In " + s.toUpperCase() + "-Dateien hinein wechseln" );
	  cb.setEnabled( hasDirs || foundVfsSuffixes.contains( s ) );
	  gbc.insets.top = 0;
	  gbc.gridy++;
	  add( cb, gbc );
	  this.cb2VfsSuffix.put( cb, s );
	}
      }
    }


    // Ausgabe
    this.fldLog = GUIFactory.createTextArea( 4, 0 );
    this.fldLog.setEditable( false );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.fill          = GridBagConstraints.BOTH;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.weightx       = 1.0;
    gbc.weighty       = 1.0;
    gbc.gridwidth     = 2;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( GUIFactory.createScrollPane( this.fldLog ), gbc );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );

    this.btnApply = GUIFactory.createButtonApply();
    panelBtn.add( this.btnApply );

    this.btnClose = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnClose );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridy++;
    add( panelBtn, gbc );


    // Vorbelegung
    java.util.Date date = null;
    if( !this.paths.isEmpty() ) {
      try {
	FileTime t = Files.getLastModifiedTime( this.paths.get( 0 ) );
	if( t != null ) {
	  date = new java.util.Date( t.toMillis() );
	}
      }
      catch( IOException ex ) {}
    }
    if( date == null ) {
      date = new java.util.Date();
    }
    this.fldTime.setText( this.dateFmtStd.format( date ) );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );
    this.fldLog.setRows( 0 );


    // Aktionen im Popup-Menu
    this.actionMngr = new LogTextActionMngr( this.fldLog, true );


    // Listener
    this.rbCurrentTime.addActionListener( this );
    this.rbTimeInput.addActionListener( this );
    this.fldTime.addActionListener( this );
    this.cbRecursive.addActionListener( this );
    this.btnApply.addActionListener( this );
    this.btnClose.addActionListener( this );
    this.fldLog.addMouseListener( this );
    for( JCheckBox cb : this.cb2VfsSuffix.keySet() ) {
      cb.addActionListener( this );
    }
  }


	/* --- Aktionen --- */

  private void doApply()
  {
    if( this.thread == null ) {
      try {
	this.fldLog.setText( "" );
	if( this.rbCurrentTime.isSelected() ) {
	  this.fileTime = FileTime.fromMillis( System.currentTimeMillis() );
	}
	else if( this.rbTimeInput.isSelected() ) {
	  String text = this.fldTime.getText();
	  if( text != null ) {
	    java.util.Date date = null;
	    try {
	      date = this.dateFmtStd.parse( text );
	    }
	    catch( ParseException ex ) {}
	    if( date == null ) {
	      date = this.dateFmtShort.parse( text );
	    }
	    this.fileTime = FileTime.fromMillis(
				date != null ?
					date.getTime()
					: System.currentTimeMillis() );

	  }
	}
	if( this.fileTime != null ) {
	  this.archiveFileSuffixes = new TreeSet<>();
	  for( JCheckBox cb : this.cb2VfsSuffix.keySet() ) {
	    if( cb.isSelected() ) {
	      String suffix = this.cb2VfsSuffix.get( cb );
	      if( suffix != null ) {
		this.archiveFileSuffixes.add( suffix );
	      }
	    }
	  }
	  this.btnApply.setEnabled( false );
	  this.numTouched = 0;
	  this.numFailed  = 0;
	  this.cancelled  = false;
	  this.recursive  = this.cbRecursive.isSelected();
	  this.thread     = new Thread( this, "JKCEMU set last modified" );
	  this.thread.start();
	} else {
	  showErrorDlg( this, "Sie m\u00FCssen eine Zeit festlegen." );
	}
      }
      catch( ParseException ex ) {
	showErrorDlg( this, "Datum/Uhrzeit: ung\u00FCltige Eingabe" );
      }
    }
  }


	/* --- private Methoden --- */

  private void appendToLog( String msg )
  {
    if( msg != null ) {
      if( !msg.isEmpty() ) {
	Document doc = this.fldLog.getDocument();
	if( doc != null ) {
	  if( doc.getLength() > 0 ) {
	    this.fldLog.append( "\n\n" );
	  }
	}
	this.fldLog.append( msg );
      }
    }
  }


  private void threadTerminated( String msg )
  {
    StringBuilder buf = new StringBuilder( 256 );
    if( this.cancelled ) {
      buf.append( "Vorgang abgebrochen\n" );
    }
    buf.append( this.numTouched );
    if( this.numTouched == 1 ) {
      buf.append( " Datei/Verzeichnis" );
    } else {
      buf.append( " Dateien/Verzeichnissen" );
    }
    buf.append( " ge\u00E4ndert" );
    if( this.numFailed > 0 ) {
      buf.append( "\n" );
      buf.append( this.numFailed );
      if( this.numFailed == 1 ) {
	buf.append( " Datei/Verzeichnis konnte" );
      } else {
	buf.append( " Dateien/Verzeichnisse konnten" );
      }
      buf.append( " nicht ge\u00E4ndert werden." );
    }
    if( !this.cancelled ) {
      buf.append( "\nFertig" );
    }
    appendToLog( buf.toString() );
    this.btnClose.setText( EmuUtil.TEXT_CLOSE );
    if( this.winClosing ) {
      doClose();
    }
  }
}
