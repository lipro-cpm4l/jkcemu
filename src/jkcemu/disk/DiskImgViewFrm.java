/*
 * (c) 2016-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Inspektor fuer Diskettenabbilddateien
 */

package jkcemu.disk;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.EventObject;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.PatternSyntaxException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.JTextComponent;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.ByteDataSource;
import jkcemu.base.EmuUtil;
import jkcemu.base.HelpFrm;
import jkcemu.base.HexCharFld;
import jkcemu.base.ReplyBytesDlg;


public class DiskImgViewFrm extends BaseFrm
			implements
				ByteDataSource,
				CaretListener,
				DropTargetListener,
				HyperlinkListener
{
  private static final String HELP_PAGE      = "/help/disk/diskimgviewer.htm";
  private static final String MARK_BOGUS_ID  = "*";
  private static final String MARK_NO_DATA   = "no_data";
  private static final String MARK_DELETED   = "del";
  private static final String MARK_ERROR     = "err";
  private static final String PROP_SPLIT_POS = "split.position";


  private static class DataFoundException extends Exception
  {
    private DataFoundException() {}
  };


  private static final int PREF_SECTORLIST_W = 300;
  private static final int PREF_SECTORDATA_W = 300;
  private static final int PREF_SECTORDATA_H = 200;

  private static final String HTML_ACTION_KEY_PREFIX  = "sector:";
  private static final String HTML_ACTION_HREF_PREFIX
				= "href=" + HTML_ACTION_KEY_PREFIX;

  private static final String TITLE = "JKCEMU Diskettenabbilddatei-Inspektor";

  private static final String NOT_RECOGNIZED = "nicht erkannt";

  private static DiskImgViewFrm instance = null;

  private AbstractFloppyDisk        disk;
  private File                      file;
  private SectorData                selectedSector;
  private boolean                   lastBigEndian;
  private ReplyBytesDlg.InputFormat lastInputFmt;
  private String                    lastFindText;
  private byte[]                    findBytes;
  private int                       findCyl;
  private int                       findHead;
  private int                       findSectorIdx;
  private int                       findDataPos;
  private JMenuItem                 mnuOpen;
  private JMenuItem                 mnuClose;
  private JMenuItem                 mnuBytesCopyAscii;
  private JMenuItem                 mnuBytesCopyHex;
  private JMenuItem                 mnuBytesCopyDump;
  private JMenuItem                 mnuFind;
  private JMenuItem                 mnuFindNext;
  private JMenuItem                 mnuHelpContent;
  private JSplitPane                splitPane;
  private JEditorPane               fldFileContent;
  private JTextField                fldFileName;
  private JTextField                fldPhysFormat;
  private JTextField                fldRemark;
  private JTextField                fldTimestamp;
  private JTextField                fldLogFormat;
  private JTextField                fldBlockSize;
  private JTextField                fldBlockNumFmt;
  private JTextField                fldSectorPos;
  private JTextField                fldSectorID;
  private JTextField                fldSectorEtc;
  private HexCharFld                fldSectorData;
  private JButton                   btnSectorCopy;
  private JButton                   btnSectorExport;
  private JScrollPane               spSectorData;
  private JLabel                    sectorDataInfo;


  public static void open()
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
        instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new DiskImgViewFrm();
    }
    instance.toFront();
    instance.setVisible( true );
  }


  public static void open( File file )
  {
    open();
    if( file != null ) {
      instance.openFile( file );
    }
  }


	/* --- ByteSourceData --- */

  @Override
  public int getAddrOffset()
  {
    return 0;
  }


  @Override
  public int getDataByte( int addr )
  {
    SectorData sector = this.selectedSector;
    return sector != null ? sector.getDataByte( addr ) : 0;
  }


  @Override
  public int getDataLength()
  {
    SectorData sector = this.selectedSector;
    return sector != null ? sector.getDataLength() : 0;
  }


	/* --- CaretListener --- */

  @Override
  public void caretUpdate( CaretEvent e )
  {
    int     pos   = this.fldSectorData.getCaretPosition();
    boolean state = ((pos >= 0) && (pos < getDataLength()));
    this.mnuBytesCopyAscii.setEnabled( state );
    this.mnuBytesCopyHex.setEnabled( state );
    this.mnuBytesCopyDump.setEnabled( state );
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // leer
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    File file = EmuUtil.fileDrop( this, e );
    if( file != null ) {
      openFile( file );
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    // leer
  }


	/* --- HyperlinkListener --- */

  @Override
  public void hyperlinkUpdate( HyperlinkEvent e )
  {
    if( (this.disk != null)
	&& (e.getSource() == this.fldFileContent)
	&& e.getEventType().equals( HyperlinkEvent.EventType.ACTIVATED ) )
    {
      javax.swing.text.Element elem = e.getSourceElement();
      if( elem != null ) {
	javax.swing.text.AttributeSet atts = elem.getAttributes();
	if( atts != null ) {
	  Object a = atts.getAttribute( javax.swing.text.html.HTML.Tag.A );
	  if( a != null ) {
	    String text = a.toString();
	    if( text != null ) {
	      if( text.startsWith( HTML_ACTION_HREF_PREFIX ) ) {
		text = text.substring( HTML_ACTION_HREF_PREFIX.length() );
		try {
		  String[] items = text.split( ":" );
		  if( items != null ) {
		    if( items.length == 3 ) {
		      showSector(
				Integer.parseInt( items[ 0 ].trim() ),
				Integer.parseInt( items[ 1 ].trim() ),
				Integer.parseInt( items[ 2 ].trim() ) );
		    }
		  }
		}
		catch( NumberFormatException ex ) {}
		catch( PatternSyntaxException ex ) {}
	      }
	    }
	  }
	}
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props, boolean resizable )
  {
    boolean rv   = super.applySettings( props, resizable );
    int splitPos = EmuUtil.getIntProperty(
				props,
				getSettingsPrefix() + PROP_SPLIT_POS,
				-1 );
    if( splitPos >= 0 ) {
      this.splitPane.setDividerLocation( splitPos );
    }
    return rv;
  }


  @Override
  public boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.mnuOpen ) {
	rv = true;
	doFileOpen();
      } else if( src == this.mnuClose ) {
	rv = true;
	doClose();
      } else if( src == this.mnuBytesCopyAscii ) {
        rv = true;
        this.fldSectorData.copySelectedBytesAsAscii();
      } else if( src == this.mnuBytesCopyHex ) {
        rv = true;
        this.fldSectorData.copySelectedBytesAsHex();
      } else if( src == this.mnuBytesCopyDump ) {
        rv = true;
        this.fldSectorData.copySelectedBytesAsDump();
      } else if( src == this.mnuFind ) {
        rv = true;
        doFind();
      } else if( src == this.mnuFindNext ) {
        rv = true;
        doFindNext();
      } else if( src == this.mnuHelpContent ) {
	rv = true;
	HelpFrm.open( HELP_PAGE );
      } else if( src == this.btnSectorCopy ) {
	rv = true;
	doSectorCopy();
      } else if( src == this.btnSectorExport ) {
	rv = true;
	doSectorExport();
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      if( !Main.checkQuit( this ) ) {
	// damit beim erneuten Oeffnen das Eingabefeld leer ist
	if( this.disk != null ) {
	  this.disk.closeSilent();
	}
	this.disk = null;
	this.file = null;
	this.mnuFind.setEnabled( false );
	this.fldFileName.setText( "" );
	this.fldPhysFormat.setText( "" );
	this.fldRemark.setText( "" );
	this.fldTimestamp.setText( "" );
	this.fldLogFormat.setText( "" );
	this.fldBlockSize.setText( "" );
	this.fldBlockNumFmt.setText( "" );
	this.fldFileContent.setContentType( "text/plain" );
	this.fldFileContent.setText( "" );
	this.fldFileContent.setCaretPosition( 0 );
	clearSectorDetails();
      }
    }
    return rv;
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      super.putSettingsTo( props );
      props.setProperty(
		getSettingsPrefix() + PROP_SPLIT_POS,
		String.valueOf( this.splitPane.getDividerLocation() ) );
    }
  }


	/* --- Konstruktor --- */

  private DiskImgViewFrm()
  {
    setTitle( TITLE );
    Main.updIcon( this );
    this.disk              = null;
    this.file              = null;
    this.selectedSector    = null;


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuOpen = createJMenuItem(
			"\u00D6ffnen...",
			KeyStroke.getKeyStroke(
					KeyEvent.VK_O,
					InputEvent.CTRL_MASK ) );
    mnuFile.add( this.mnuOpen );
    mnuFile.addSeparator();

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );
    mnuBar.add( mnuEdit );

    this.mnuBytesCopyHex = createJMenuItem(
		"Ausgw\u00E4hlte Bytes als Hexadezimalzahlen kopieren" );
    this.mnuBytesCopyHex.setEnabled( false );
    mnuEdit.add( this.mnuBytesCopyHex );

    this.mnuBytesCopyAscii = createJMenuItem(
		"Ausgw\u00E4hlte Bytes als ASCII-Text kopieren" );
    this.mnuBytesCopyAscii.setEnabled( false );
    mnuEdit.add( this.mnuBytesCopyAscii );

    this.mnuBytesCopyDump = createJMenuItem(
		"Ausgw\u00E4hlte Bytes als Hex-ASCII-Dump kopieren" );
    this.mnuBytesCopyDump.setEnabled( false );
    mnuEdit.add( this.mnuBytesCopyDump );
    mnuEdit.addSeparator();

    this.mnuFind = createJMenuItem(
		"Suchen...",
		KeyStroke.getKeyStroke( KeyEvent.VK_F, Event.CTRL_MASK ) );
    this.mnuFind.setEnabled( false );
    mnuEdit.add( this.mnuFind );

    this.mnuFindNext = createJMenuItem(
		"Weitersuchen",
		KeyStroke.getKeyStroke( KeyEvent.VK_F3, 0 ) );
    this.mnuFindNext.setEnabled( false );
    mnuEdit.add( this.mnuFindNext );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Fensterinhalt
    setLayout( new BorderLayout() );

    this.fldFileContent = new JEditorPane();
    this.fldFileContent.setEditable( false );
    this.fldFileContent.setPreferredSize(
				new Dimension( PREF_SECTORDATA_W, 1 ) );

    JPanel panelDetails = new JPanel( new GridBagLayout() );

    this.splitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT,
				false,
				new JScrollPane( this.fldFileContent ),
				panelDetails );
    add( this.splitPane, BorderLayout.CENTER );


    // linke Seite Detailansicht
    GridBagConstraints gbcDetails = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.HORIZONTAL,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    // Bereich Datei
    JPanel panelFile = new JPanel( new GridBagLayout() );
    panelFile.setBorder( BorderFactory.createTitledBorder( "Datei" ) );
    panelDetails.add( panelFile, gbcDetails );

    GridBagConstraints gbcFile = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelFile.add( new JLabel( "Dateiname:" ), gbcFile );
    gbcFile.gridy++;
    panelFile.add( new JLabel( "Format:" ), gbcFile );
    gbcFile.gridy++;
    panelFile.add( new JLabel( "Bemerkung:" ), gbcFile );
    gbcFile.insets.bottom = 5;
    gbcFile.gridy++;
    panelFile.add( new JLabel( "Zeitstempel:" ), gbcFile );

    this.fldFileName = new JTextField();
    this.fldFileName.setEditable( false );
    gbcFile.fill          = GridBagConstraints.HORIZONTAL;
    gbcFile.weightx       = 1.0;
    gbcFile.insets.bottom = 0;
    gbcFile.gridy         = 0;
    gbcFile.gridx++;
    panelFile.add( this.fldFileName, gbcFile );

    this.fldPhysFormat = new JTextField();
    this.fldPhysFormat.setEditable( false );
    gbcFile.gridy++;
    panelFile.add( this.fldPhysFormat, gbcFile );

    this.fldRemark = new JTextField();
    this.fldRemark.setEditable( false );
    gbcFile.gridy++;
    panelFile.add( this.fldRemark, gbcFile );

    this.fldTimestamp = new JTextField();
    this.fldTimestamp.setEditable( false );
    gbcFile.insets.bottom = 5;
    gbcFile.gridy++;
    panelFile.add( this.fldTimestamp, gbcFile );

    // Bereich automatische CP/M-Diskettenformaterkannung
    JPanel panelRecognizedFmt = new JPanel( new GridBagLayout() );
    panelRecognizedFmt.setBorder(
	BorderFactory.createTitledBorder(
			"Automatische CP/M-Diskettenformaterkennung" ) );
    gbcDetails.gridy++;
    panelDetails.add( panelRecognizedFmt, gbcDetails );

    GridBagConstraints gbcRecognizedFmt = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelRecognizedFmt.add(
		new JLabel( "Systemspuren / Directory:" ),
		gbcRecognizedFmt );
    gbcRecognizedFmt.gridy++;
    panelRecognizedFmt.add(
		new JLabel( "Blockgr\u00F6\u00DFe:" ),
		gbcRecognizedFmt );
    gbcRecognizedFmt.insets.bottom = 5;
    gbcRecognizedFmt.gridy++;
    panelRecognizedFmt.add(
		new JLabel( "Blocknummernformat:" ),
		gbcRecognizedFmt );

    this.fldLogFormat = new JTextField();
    this.fldLogFormat.setEditable( false );
    gbcRecognizedFmt.fill          = GridBagConstraints.HORIZONTAL;
    gbcRecognizedFmt.weightx       = 1.0;
    gbcRecognizedFmt.insets.bottom = 0;
    gbcRecognizedFmt.gridy         = 0;
    gbcRecognizedFmt.gridx++;
    panelRecognizedFmt.add( this.fldLogFormat, gbcRecognizedFmt );

    this.fldBlockSize = new JTextField();
    this.fldBlockSize.setEditable( false );
    gbcRecognizedFmt.gridy++;
    panelRecognizedFmt.add( this.fldBlockSize, gbcRecognizedFmt );

    this.fldBlockNumFmt = new JTextField();
    this.fldBlockNumFmt.setEditable( false );
    gbcRecognizedFmt.insets.bottom = 5;
    gbcRecognizedFmt.gridy++;
    panelRecognizedFmt.add( this.fldBlockNumFmt, gbcRecognizedFmt );

    // Bereich Sektor
    JPanel panelSector = new JPanel( new GridBagLayout() );
    panelSector.setBorder( BorderFactory.createTitledBorder( "Sektor" ) );
    gbcDetails.fill    = GridBagConstraints.BOTH;
    gbcDetails.weighty = 1.0;
    gbcDetails.gridy++;
    panelDetails.add( panelSector, gbcDetails );

    GridBagConstraints gbcSector = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelSector.add( new JLabel( "Sektorposition:" ), gbcSector );
    gbcSector.gridy++;
    panelSector.add( new JLabel( "Sektor-ID:" ), gbcSector );
    gbcSector.insets.bottom = 5;
    gbcSector.gridy++;
    panelSector.add( new JLabel( "Sonstiges:" ), gbcSector );

    this.fldSectorPos = new JTextField();
    this.fldSectorPos.setEditable( false );
    gbcSector.fill          = GridBagConstraints.HORIZONTAL;
    gbcSector.weightx       = 1.0;
    gbcSector.insets.bottom = 0;
    gbcSector.gridy         = 0;
    gbcSector.gridx++;
    panelSector.add( this.fldSectorPos, gbcSector );

    this.fldSectorID = new JTextField();
    this.fldSectorID.setEditable( false );
    gbcSector.gridy++;
    panelSector.add( this.fldSectorID, gbcSector );

    this.fldSectorEtc = new JTextField();
    this.fldSectorEtc.setEditable( false );
    gbcSector.insets.bottom = 5;
    gbcSector.gridy++;
    panelSector.add( this.fldSectorEtc, gbcSector );

    this.sectorDataInfo = new JLabel(
		"Bitte in der linken Ansicht einen Sektor anklicken" );
    this.sectorDataInfo.setHorizontalAlignment( SwingConstants.CENTER );
    this.sectorDataInfo.setVerticalAlignment( SwingConstants.CENTER );
    this.fldSectorData = new HexCharFld( this );

    JPanel sectorDataPlaceholder = new JPanel();
    sectorDataPlaceholder.setPreferredSize(
		new Dimension( PREF_SECTORDATA_W, PREF_SECTORDATA_H ) );
    this.spSectorData   = new JScrollPane( sectorDataPlaceholder );
    gbcSector.fill      = GridBagConstraints.BOTH;
    gbcSector.weighty   = 1.0;
    gbcSector.gridwidth = GridBagConstraints.REMAINDER;
    gbcSector.gridx     = 0;
    gbcSector.gridy++;
    panelSector.add( this.spSectorData, gbcSector );

    JPanel panelSectorBtns = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbcSector.anchor       = GridBagConstraints.CENTER;
    gbcSector.fill         = GridBagConstraints.NONE;
    gbcSector.weightx      = 0.0;
    gbcSector.weighty      = 0.0;
    gbcSector.gridy++;
    panelSector.add( panelSectorBtns, gbcSector );

    this.btnSectorCopy = new JButton( "Sektordaten kopieren" );
    this.btnSectorCopy.setEnabled( false );
    panelSectorBtns.add( this.btnSectorCopy );

    this.btnSectorExport = new JButton( "Sektordaten exportieren..." );
    this.btnSectorExport.setEnabled( false );
    panelSectorBtns.add( this.btnSectorExport );


    // Drag&Drop aktivieren
    (new DropTarget( this.fldFileContent, this )).setActive( true );


    // Listener
    this.fldFileContent.addHyperlinkListener( this );
    this.fldSectorData.addCaretListener( this );
    this.btnSectorCopy.addActionListener( this );
    this.btnSectorExport.addActionListener( this );

    // sonstiges
    resetFind();
    setResizable( true );
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setLocationByPlatform( true );
    }
    this.fldFileContent.setPreferredSize( null );
  }


	/* --- Aktionen --- */

  private void doFileOpen()
  {
    openFile( EmuUtil.showFileOpenDlg(
			this,
			"Diskettenabbilddatei \u00F6ffnen",
			Main.getLastDirFile( Main.FILE_GROUP_DISK ),
			EmuUtil.getPlainDiskFileFilter(),
			EmuUtil.getAnaDiskFileFilter(),
			EmuUtil.getCopyQMFileFilter(),
			EmuUtil.getDskFileFilter(),
			EmuUtil.getImageDiskFileFilter(),
			EmuUtil.getTeleDiskFileFilter() ) );
  }


  private void doFind()
  {
    if( this.disk != null ) {
      ReplyBytesDlg dlg = new ReplyBytesDlg(
					this,
					"Bytes suchen",
					this.lastInputFmt,
					this.lastBigEndian,
					this.lastFindText );
      dlg.setVisible( true );
      byte[] findBytes = dlg.getApprovedBytes();
      if( findBytes != null ) {
	if( findBytes.length > 0 ) {
	  resetFind();
	  this.findBytes     = findBytes;
	  this.lastInputFmt  = dlg.getApprovedInputFormat();
	  this.lastBigEndian = dlg.getApprovedBigEndian();
	  this.lastFindText  = dlg.getApprovedText();
	  doFindNext();
	  this.mnuFindNext.setEnabled( true );
	}
      }
    }
  }


  private void doFindNext()
  {
    if( (this.disk != null) && (this.findBytes != null) ) {
      if( this.findBytes.length > 0 ) {
	if( this.findCyl < 0 ) {
	  this.findCyl = 0;
	}
	if( this.findHead < 0 ) {
	  this.findHead = 0;
	}
	int nCyls = this.disk.getCylinders();
	int sides = this.disk.getSides();
	for( int cyl = this.findCyl; cyl < nCyls; cyl++ ) {
	  for( int head = this.findHead; head < sides; head++ ) {
	    if( this.findSectorIdx < 0 ) {
	      this.findSectorIdx = 0;
	    }
	    int nSectors = this.disk.getSectorsOfCylinder( cyl, head );
	    for( int idx = this.findSectorIdx; idx < nSectors; idx++ ) {
	      SectorData sector = this.disk.getSectorByIndex( cyl, head, idx );
	      if( sector != null ) {
		if( this.findDataPos < 0 ) {
		  this.findDataPos = 0;
		}
		int dataLen = sector.getDataLength();
		for( int i = this.findDataPos; i < dataLen; i++ ) {
		  boolean found = true;
		  for( int k = 0; k < this.findBytes.length; k++ ) {
		    int p = i + k;
		    if( p < dataLen ) {
		      if( (byte) sector.getDataByte( p )
						!= this.findBytes[ k ] )
		      {
			found = false;
			break;
		      }
		    } else {
		      found = false;
		      break;
		    }
		  }
		  if( found ) {

		    // Sektor anzeigen
		    showSector( cyl, head, idx );

		    /*
		     * gefundene Bytes rueckwaerts selektieren,
		     * damit der Cursor auf der ersten,
		     * d.h. der gefundenen Position steht
		     */
		    final int        p1  = i + this.findBytes.length - 1;
		    final int        p2  = i;
		    final HexCharFld fld = this.fldSectorData;
		    EventQueue.invokeLater(
				new Runnable()
				{
				  @Override
				  public void run()
				  {
				    fld.setSelection( p1, p2 );
				  }
				} );

		    // gefundene Position inkrementieren
		    this.findDataPos = i + 1;
		    if( this.findDataPos >= dataLen ) {
		      this.findDataPos = 0;
		      this.findSectorIdx++;
		      if( this.findSectorIdx >= nSectors ) {
			this.findSectorIdx = 0;
			head++;
			if( head >= sides ) {
			  this.findCyl++;
			}
		      }
		    }

		    // Suche verlassen
		    return;
		  }
		}
	      }
	    }
	  }
	}
	BaseDlg.showInfoDlg( this, "Byte-Folge nicht gefunden" );
      }
    }
  }


  private void doSectorCopy()
  {
    SectorData sector = this.selectedSector;
    if( sector != null ) {
      int len = sector.getDataLength();
      if( len > 0 ) {
	StringBuilder buf = new StringBuilder( len * 3 );
	for( int i = 0; i < len; i++ ) {
	  if( i > 0 ) {
	    buf.append( (char) ((i % 16) == 0 ? '\n' : '\u0020') );
	  }
	  buf.append( String.format( "%02X", sector.getDataByte( i ) ) );
	}
	buf.append( (char) '\n' );
	EmuUtil.copyToClipboard( this, buf.toString() );
      }
    }
  }


  private void doSectorExport()
  {
    SectorData sector = this.selectedSector;
    if( sector != null ) {
      int len = sector.getDataLength();
      if( len > 0 ) {
	File dirFile = Main.getLastDirFile( Main.FILE_GROUP_SECTOR );
	if( (dirFile == null) && (this.file != null) ) {
	  dirFile = this.file.getParentFile();
	}
	String preName = "";
	if( this.file != null ) {
	  String s = this.file.getName();
	  if( s != null ) {
	    int pos = s.indexOf( '.' );
	    if( pos > 0 ) {
	      preName = s.substring( 0, pos ) + "_";
	    }
	  }
	}
	String fName = String.format(
				"%ssector_%d_%d_%d_%d.bin",
				preName,
				sector.getCylinder(),
				sector.getHead(),
				sector.getSectorNum(),
				sector.getSizeCode() );
	File file = EmuUtil.showFileSaveDlg(
				this,
				"Sektordaten exportieren",
				dirFile != null ? 
					new File( dirFile, fName )
					: new File( fName ),
				EmuUtil.getBinaryFileFilter() );
	if( file != null ) {
	  try {
	    OutputStream out = null;
	    try {
	      out = new FileOutputStream( file );
	      sector.writeTo( out, -1 );
	      out.close();
	      out = null;
	      Main.setLastFile( file, Main.FILE_GROUP_SECTOR );
	    }
	    finally {
	      EmuUtil.closeSilent( out );
	    }
	  }
	  catch( IOException ex ) {
	    BaseDlg.showErrorDlg( this, ex );
	  }
	}
      }
    }
  }


	/* --- private Methoden --- */

  private void clearSectorDetails()
  {
    this.selectedSector = null;
    this.fldSectorPos.setText( "" );
    this.fldSectorID.setText( "" );
    this.fldSectorEtc.setText( "" );
    this.btnSectorCopy.setEnabled( false );
    this.btnSectorExport.setEnabled( false );
    if( this.disk != null ) {
      setSectorDataView( this.sectorDataInfo );
    } else {
      setSectorDataView( null );
    }
  }


  private void openFile( File file )
  {
    if( file != null ) {
      try {
	AbstractFloppyDisk disk = DiskUtil.readDiskFile( this, file, false );
	if( disk != null ) {

	  // alte Abbilddatei schliessen und neue uebernehmen
	  if( this.disk != null ) {
	    this.disk.closeSilent();
	  }
	  this.disk = disk;
	  this.file = file;
	  Main.setLastFile( file, Main.FILE_GROUP_DISK );
	  setTitle( TITLE + ": " + file.getPath() );

	  // Allgemeine Infos
	  int cyls          = disk.getCylinders();
	  int sides         = disk.getSides();
	  int sectorsPerCyl = disk.getSectorsPerCylinder();
	  int sectorSize    = disk.getSectorSize();

	  setText( this.fldFileName, file.getName() );
	  setText(
		this.fldPhysFormat,
		String.format(
			"%d KByte brutto (%d x %d x %d x %d)",
			cyls * sides * sectorSize * sectorsPerCyl / 1024,
			cyls,
			sides,
			sectorsPerCyl,
			sectorSize ) );
	  setText( this.fldRemark, disk.getRemark() );
	  java.util.Date diskDate = disk.getDiskDate();
	  if( diskDate != null ) {
	    setText(
		this.fldTimestamp,
		DateFormat.getDateTimeInstance(
				DateFormat.MEDIUM,
				DateFormat.MEDIUM ).format( diskDate ) );
	  }

	  // Automatische Formaterkennung
	  StringBuilder buf         = new StringBuilder( 0x2000 );
	  int           blockSize   = DiskUtil.DEFAULT_BLOCK_SIZE;
	  float         sysTracks   = 0F;
	  AtomicInteger rvSysBytes  = new AtomicInteger( -1 );
	  AtomicInteger rvBlockSize = new AtomicInteger( -1 );
	  byte[]        dirBytes    = DiskUtil.findAndReadDirBytes(
								null,
								null,
								null,
								disk,
								rvSysBytes );
	  if( rvSysBytes.get() > 0 ) {
	    int bytesPerTrack = disk.getSides()
					* disk.getSectorsPerCylinder()
					* disk.getSectorSize();
	    if( bytesPerTrack > 0 ) {
	      sysTracks = rvSysBytes.floatValue() / (float) bytesPerTrack;
	    }
	  }
	  Boolean blkNum16Bit = DiskUtil.checkBlockNum16Bit(
						dirBytes,
						disk.getDiskSize(),
						rvBlockSize );
	  if( rvBlockSize.get() > 0 ) {
	    blockSize = rvBlockSize.get();
	  }
	  if( dirBytes.length > 0 ) {
	    int sysTracks100 = Math.round( sysTracks * 100F );
	    if( sysTracks100 == 0 ) {
	      buf.append( "Keine Systemspur" );
	    } else if( sysTracks100 == 100 ) {
	      buf.append( "1 Systemspur" );
	    } else {
	      NumberFormat numFmt = NumberFormat.getNumberInstance();
	      if( numFmt instanceof DecimalFormat ) {
		((DecimalFormat) numFmt).applyPattern( "#0.##" );
	      }
	      buf.append( numFmt.format( sysTracks ) );
	      buf.append( " Systemspuren" );
	    }
	    buf.append( ", " );
	    boolean dirFilled = false;
	    if( DiskUtil.isFilledDir( dirBytes ) ) {
	      if( (dirBytes.length % 1024) == 0 ) {
		buf.append( String.format( 
				"%d KByte",
				dirBytes.length / 1024 ) );
	      } else {
		buf.append( String.format( "%d Byte", dirBytes.length ) );
	      }
	    } else {
	      buf.append( "leeres" );
	    }
	    buf.append( " Directory" );
	  } else {
	    buf.append( NOT_RECOGNIZED );
	  }
	  setText( this.fldLogFormat, buf.toString() );
	  if( rvBlockSize.get() > 0 ) {
	    buf.setLength( 0 );
	    if( (blockSize % 1024) == 0 ) {
	      buf.append( String.format( "%d KByte", blockSize / 1024 ) );
	    } else {
	      buf.append( String.format( "%d Byte", blockSize ) );
	    }
	    buf.append( ", nicht zu 100% sicher erkennbar!" );
	    setText( this.fldBlockSize, buf.toString() );
	  } else {
	    setText( this.fldBlockSize, NOT_RECOGNIZED );
	  }
	  if( blkNum16Bit != null ) {
	    setText(
		this.fldBlockNumFmt,
		blkNum16Bit.booleanValue() ? "16 Bit" : "8 Bit" );
	  } else {
	    setText( this.fldBlockNumFmt, NOT_RECOGNIZED );
	  }

	  // Sektortabelle
	  buf.setLength( 0 );
	  buf.append( "<html>\n" );

	  if( (cyls < 1) || (sides < 1) ) {
	    buf.append( "Diskettenabbilddatei ist leer!" );
	  } else {
	    boolean hasBogusIdSectors = false;
	    boolean hasNoDataSectors  = false;
	    boolean hasDeletedSectors = false;
	    boolean hasErrorSectors   = false;
	    buf.append( "<table border=\"1\">\n"
			+ "<tr><th>Spur</th>" );
	    for( int side = 0; side < sides; side++ ) {
	      buf.append( "<th>Sektoren Seite&nbsp;" );
	      buf.append( side + 1 );
	      buf.append( " (Kopf&nbsp;" );
	      buf.append( side );
	      buf.append( ")</th>" );
	    }
	    buf.append( "</tr>\n" );
	    for( int physCyl = 0; physCyl < cyls; physCyl++ ) {
	      buf.append( "<tr><td align=\"right\">" );
	      buf.append( physCyl );
	      buf.append( "</td>" );
	      for( int physHead = 0; physHead < sides; physHead++ ) {
		buf.append( "<td align=\"left\" nowrap=\"nowrap\">" );
		int n = disk.getSectorsOfCylinder( physCyl, physHead );
		for( int i = 0; i < n; i++ ) {
		  SectorData sector = disk.getSectorByIndex(
							physCyl,
							physHead,
							i );
		  if( sector != null ) {
		    if( i > 0 ) {
		      buf.append( "&nbsp;" );
		    }
		    buf.append( "<a href=\"" );
		    buf.append( HTML_ACTION_KEY_PREFIX );
		    buf.append( physCyl );
		    buf.append( (char) ':' );
		    buf.append( physHead );
		    buf.append( (char) ':' );
		    buf.append( i );
		    buf.append( "\">[" );
		    buf.append( sector.getSectorNum() );
		    if( sector.hasBogusID() ) {
		      hasBogusIdSectors = true;
		      buf.append( MARK_BOGUS_ID );
		    }
		    int c = sector.getCylinder();
		    if( c != physCyl ) {
		      buf.append( ",c=" );
		      buf.append( c );
		    }
		    int h = sector.getHead();
		    if( h != physHead ) {
		      buf.append( ",h=" );
		      buf.append( h );
		    }
		    int dataLen = sector.getDataLength();
		    if( dataLen != sectorSize ) {
		      buf.append( ",n=" );
		      buf.append( sector.getSizeCode() );
		    }
		    if( dataLen == 0 ) {
		      hasNoDataSectors = true;
		      buf.append( (char) ',' );
		      buf.append( MARK_NO_DATA );
		    }
		    if( sector.isDeleted() ) {
		      hasDeletedSectors = true;
		      buf.append( (char) ',' );
		      buf.append( MARK_DELETED );
		    }
		    if( sector.checkError() ) {
		      hasErrorSectors = true;
		      buf.append( (char) ',' );
		      buf.append( MARK_ERROR );
		    }
		    buf.append( "]</a>" );
		  }
		}
		buf.append( "</td>" );
	      }
	      buf.append( "</tr>\n" );
	    }
	    buf.append( "</table>\n" );
	    if( hasBogusIdSectors
			|| hasNoDataSectors
			|| hasDeletedSectors
			|| hasErrorSectors )
	    {
	      buf.append( "<br/>\n"
			+ "Agenda:<br/>\n"
			+ "<table border=\"0\">\n" );
	      if( hasBogusIdSectors ) {
		buf.append( "<tr><td valign=\"top\">" );
		buf.append( MARK_BOGUS_ID );
		buf.append( "</td><td valign=\"top\">Sektor-ID generiert,"
			+ " da Sektorkopf nicht gelesen werden konnte"
			+ "</td></tr>\n" );
	      }
	      if( hasNoDataSectors ) {
		buf.append( "<tr><td valign=\"top\">" );
		buf.append( MARK_NO_DATA );
		buf.append( "</td><td valign=\"top\">"
			+ "Sektor ohne Datenbereich</td></tr>\n" );
	      }
	      if( hasDeletedSectors ) {
		buf.append( "<tr><td valign=\"top\">" );
		buf.append( MARK_DELETED );
		buf.append( "</td><td valign=\"top\">"
			+ "Sektor mit L&ouml;schmarkierung</td></tr>\n" );
	      }
	      if( hasDeletedSectors ) {
		buf.append( "<tr><td>" );
		buf.append( MARK_ERROR );
		buf.append( "</td><td valign=\"top\">"
			+ "Sektordaten mit CRC-Fehler gelesen</td></tr>\n" );
	      }
	      buf.append( "</table>\n" );
	    }
	  }
	  buf.append( "</html>" );
	  this.fldFileContent.setContentType( "text/html" );
	  setText( this.fldFileContent, buf.toString() );

	  // Sonstiges
	  clearSectorDetails();
	  resetFind();
	  this.mnuFind.setEnabled( true );
	}
      }
      catch( IOException ex ) {
	BaseDlg.showErrorDlg( this, ex );
      }
    }
  }


  private void resetFind()
  {
    this.lastBigEndian = false;
    this.lastInputFmt  = null;
    this.lastFindText  = null;
    this.findBytes     = null;
    this.findCyl       = 0;
    this.findHead      = 0;
    this.findSectorIdx = 0;
    this.findDataPos   = 0;
    this.mnuFindNext.setEnabled( false );
  }


  private void setSectorDataView( Component view )
  {
    JViewport vp = this.spSectorData.getViewport();
    if( vp != null ) {
      vp.setView( view );
    }
  }


  private static void setText( JTextComponent c, String text )
  {
    c.setText( text != null ? text : "" );
    c.setCaretPosition( 0 );
  }


  private void showSector( int cyl, int head, int idx )
  {
    SectorData sector = this.disk.getSectorByIndex( cyl, head, idx );
    if( sector != null ) {
      setText(
	this.fldSectorPos,
	String.format(
		"Spur %d, Seite %d, Index %d\n",
		cyl,
		head + 1,
		idx ) );
      setText(
	this.fldSectorID,
	String.format(
		"C=%d, H=%d, R=%d, N=%d\n",
		sector.getCylinder(),
		sector.getHead(),
		sector.getSectorNum(),
		sector.getSizeCode() ) );
      String etcText = "";
      if( sector.isDeleted() || sector.checkError() || sector.hasBogusID() ) {
	StringBuilder buf = new StringBuilder( 128 );
	if( sector.isDeleted() ) {
	  buf.append( "Sektor gel\u00F6scht" );
	}
	if( sector.checkError() ) {
	  if( buf.length() > 0 ) {
	    buf.append( ", " );
	  }
	  buf.append( "Lesefehler" );
	}
	if( sector.hasBogusID() ) {
	  if( buf.length() > 0 ) {
	    buf.append( ", " );
	  }
	  buf.append( "Sektor-ID generiert (Sektorkopf war nicht lesbar)" );
	}
	etcText = buf.toString();
      }
      setText( this.fldSectorEtc, etcText );
      this.selectedSector = sector;
      setSectorDataView( this.fldSectorData );
      this.fldSectorData.refresh();
      this.btnSectorCopy.setEnabled( true );
      this.btnSectorExport.setEnabled( true );
    } else {
      clearSectorDetails();
    }
  }
}

