/*
 * (c) 2016-2021 Jens Mueller
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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
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
import jkcemu.base.GUIFactory;
import jkcemu.base.HelpFrm;
import jkcemu.base.HexCharFld;
import jkcemu.base.ReplyBytesDlg;
import jkcemu.file.FileUtil;
import jkcemu.text.TextFinder;


public class DiskImgViewFrm extends BaseFrm
			implements
				ByteDataSource,
				CaretListener,
				DropTargetListener,
				HyperlinkListener
{
  public static final String TITLE = Main.APPNAME
					+ "Diskettenabbilddatei-Inspektor";

  private static final String HELP_PAGE     = "/help/disk/diskimgviewer.htm";
  private static final String MARK_BEG      = "<font color=\"red\">";
  private static final String MARK_END      = "</font>";
  private static final String MARK_BOGUS_ID = "r?";
  private static final String MARK_NO_DATA  = MARK_BEG + "no_data" + MARK_END;
  private static final String MARK_DELETED  = MARK_BEG + "del" + MARK_END;
  private static final String MARK_ERROR    = MARK_BEG + "err" + MARK_END;

  private static final String PROP_SPLIT_POS = "split.position";


  private static class NotFoundException extends Exception
  {
    private NotFoundException() {}
  };


  private static final int PREF_SECTORLIST_W = 300;
  private static final int PREF_SECTORDATA_W = 300;
  private static final int PREF_SECTORDATA_H = 200;

  private static final String HTML_ACTION_KEY_PREFIX  = "sector:";
  private static final String HTML_ACTION_HREF_PREFIX
				= "href=" + HTML_ACTION_KEY_PREFIX;

  private static final String NOT_RECOGNIZED = "nicht erkannt";

  private static DiskImgViewFrm instance = null;

  private AbstractFloppyDisk        disk;
  private File                      file;
  private String                    exportPrefix;
  private SectorData                selectedSector;
  private boolean                   lastFound;
  private boolean                   lastBigEndian;
  private ReplyBytesDlg.InputFormat lastInputFmt;
  private String                    lastFindText;
  private byte[]                    findBytes;
  private int                       findCyl;
  private int                       findDataPos;
  private int                       findHead;
  private int                       findWraps;
  private int                       findSectorIdx;
  private SectorData                findSectorData;
  private JMenuItem                 mnuOpen;
  private JMenuItem                 mnuExportTracks;
  private JMenuItem                 mnuClose;
  private JMenuItem                 mnuBytesCopyAscii;
  private JMenuItem                 mnuBytesCopyHex;
  private JMenuItem                 mnuBytesCopyDump;
  private JMenuItem                 mnuFind;
  private JMenuItem                 mnuFindPrev;
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


  public static DiskImgViewFrm open()
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
    return instance;
  }


  public static DiskImgViewFrm open( File file )
  {
    open();
    if( file != null ) {
      instance.openFile( file );
    }
    return instance;
  }


	/* --- ByteDataSource --- */

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


  @Override
  public boolean getDataReadOnly()
  {
    return true;
  }


  @Override
  public boolean setDataByte( int addr, int value )
  {
    return false;
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
    if( !FileUtil.isFileDrop( e ) )
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
    final File file = FileUtil.fileDrop( this, e );
    if( file != null ) {
      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    openFile( file );
			  }
			} );
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
  public boolean applySettings( Properties props )
  {
    boolean rv   = super.applySettings( props );
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
      } else if( src == this.mnuExportTracks ) {
	rv = true;
	TrackExportDlg.exportTracks( this, this.disk, this.exportPrefix );
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
      } else if( src == this.mnuFindPrev ) {
        rv = true;
        doFindNext( true );
      } else if( src == this.mnuFindNext ) {
        rv = true;
        doFindNext( false );
      } else if( src == this.mnuHelpContent ) {
	rv = true;
	HelpFrm.openPage( HELP_PAGE );
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
    boolean rv = false;
    if( Main.isTopFrm( this ) ) {
      rv = EmuUtil.closeOtherFrames( this );
      if( rv ) {
	rv = super.doClose();
      }
      if( rv ) {
	Main.exitSuccess();
      }
    } else {
      rv = super.doClose();
    }
    if( rv ) {
      // damit beim erneuten Oeffnen das Eingabefeld leer ist
      if( this.disk != null ) {
	this.disk.closeSilently();
      }
      this.disk = null;
      this.file = null;
      this.mnuExportTracks.setEnabled( false );
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
    this.disk           = null;
    this.file           = null;
    this.exportPrefix   = null;
    this.selectedSector = null;


    // Menu Datei
    JMenu mnuFile = createMenuFile();

    this.mnuOpen = createMenuItemWithStandardAccelerator(
						EmuUtil.TEXT_OPEN_OPEN,
						KeyEvent.VK_O );
    mnuFile.add( this.mnuOpen );
    mnuFile.addSeparator();

    this.mnuExportTracks = createMenuItem( "Spuren exportieren..." );
    this.mnuExportTracks.setEnabled( false );
    mnuFile.add( this.mnuExportTracks );
    mnuFile.addSeparator();

    this.mnuClose = createMenuItemClose();
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = createMenuEdit();

    this.mnuBytesCopyHex = createMenuItem(
		"Ausgw\u00E4hlte Bytes als Hexadezimalzahlen kopieren" );
    this.mnuBytesCopyHex.setEnabled( false );
    mnuEdit.add( this.mnuBytesCopyHex );

    this.mnuBytesCopyAscii = createMenuItem(
		"Ausgw\u00E4hlte Bytes als ASCII-Text kopieren" );
    this.mnuBytesCopyAscii.setEnabled( false );
    mnuEdit.add( this.mnuBytesCopyAscii );

    this.mnuBytesCopyDump = createMenuItem(
		"Ausgw\u00E4hlte Bytes als Hex-ASCII-Dump kopieren" );
    this.mnuBytesCopyDump.setEnabled( false );
    mnuEdit.add( this.mnuBytesCopyDump );
    mnuEdit.addSeparator();

    this.mnuFind = createMenuItemOpenFind( true );
    this.mnuFind.setEnabled( false );
    mnuEdit.add( this.mnuFind );

    this.mnuFindNext = createMenuItemFindNext( true );
    this.mnuFindNext.setEnabled( false );
    mnuEdit.add( this.mnuFindNext );

    this.mnuFindPrev = createMenuItemFindPrev( true );
    this.mnuFindPrev.setEnabled( false );
    mnuEdit.add( this.mnuFindPrev );


    // Menu Hilfe
    JMenu mnuHelp       = createMenuHelp(); 
    this.mnuHelpContent = createMenuItem(
			"Hilfe zum Diskettenabbilddatei-Inspektor..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu
    setJMenuBar( GUIFactory.createMenuBar( mnuFile, mnuEdit, mnuHelp ) );


    // Fensterinhalt
    setLayout( new BorderLayout() );

    this.fldFileContent = GUIFactory.createEditorPane();
    this.fldFileContent.setEditable( false );
    this.fldFileContent.setPreferredSize(
				new Dimension( PREF_SECTORDATA_W, 1 ) );

    JPanel panelDetails = GUIFactory.createPanel( new GridBagLayout() );

    this.splitPane = GUIFactory.createSplitPane(
			JSplitPane.HORIZONTAL_SPLIT,
			false,
			GUIFactory.createScrollPane( this.fldFileContent ),
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
    JPanel panelFile = GUIFactory.createPanel( new GridBagLayout() );
    panelFile.setBorder( GUIFactory.createTitledBorder( "Datei" ) );
    panelDetails.add( panelFile, gbcDetails );

    GridBagConstraints gbcFile = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelFile.add( GUIFactory.createLabel( "Dateiname:" ), gbcFile );
    gbcFile.gridy++;
    panelFile.add( GUIFactory.createLabel( "Format:" ), gbcFile );
    gbcFile.gridy++;
    panelFile.add( GUIFactory.createLabel( "Bemerkung:" ), gbcFile );
    gbcFile.insets.bottom = 5;
    gbcFile.gridy++;
    panelFile.add( GUIFactory.createLabel( "Zeitstempel:" ), gbcFile );

    this.fldFileName = GUIFactory.createTextField();
    this.fldFileName.setEditable( false );
    gbcFile.fill          = GridBagConstraints.HORIZONTAL;
    gbcFile.weightx       = 1.0;
    gbcFile.insets.bottom = 0;
    gbcFile.gridy         = 0;
    gbcFile.gridx++;
    panelFile.add( this.fldFileName, gbcFile );

    this.fldPhysFormat = GUIFactory.createTextField();
    this.fldPhysFormat.setEditable( false );
    gbcFile.gridy++;
    panelFile.add( this.fldPhysFormat, gbcFile );

    this.fldRemark = GUIFactory.createTextField();
    this.fldRemark.setEditable( false );
    gbcFile.gridy++;
    panelFile.add( this.fldRemark, gbcFile );

    this.fldTimestamp = GUIFactory.createTextField();
    this.fldTimestamp.setEditable( false );
    gbcFile.insets.bottom = 5;
    gbcFile.gridy++;
    panelFile.add( this.fldTimestamp, gbcFile );

    // Bereich automatische CP/M-Diskettenformaterkannung
    JPanel panelRecognizedFmt = GUIFactory.createPanel(
						new GridBagLayout() );
    panelRecognizedFmt.setBorder(
	GUIFactory.createTitledBorder(
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
		GUIFactory.createLabel( "Systemspuren / Directory:" ),
		gbcRecognizedFmt );
    gbcRecognizedFmt.gridy++;
    panelRecognizedFmt.add(
		GUIFactory.createLabel( "Blockgr\u00F6\u00DFe:" ),
		gbcRecognizedFmt );
    gbcRecognizedFmt.insets.bottom = 5;
    gbcRecognizedFmt.gridy++;
    panelRecognizedFmt.add(
		GUIFactory.createLabel( "Blocknummernformat:" ),
		gbcRecognizedFmt );

    this.fldLogFormat = GUIFactory.createTextField();
    this.fldLogFormat.setEditable( false );
    gbcRecognizedFmt.fill          = GridBagConstraints.HORIZONTAL;
    gbcRecognizedFmt.weightx       = 1.0;
    gbcRecognizedFmt.insets.bottom = 0;
    gbcRecognizedFmt.gridy         = 0;
    gbcRecognizedFmt.gridx++;
    panelRecognizedFmt.add( this.fldLogFormat, gbcRecognizedFmt );

    this.fldBlockSize = GUIFactory.createTextField();
    this.fldBlockSize.setEditable( false );
    gbcRecognizedFmt.gridy++;
    panelRecognizedFmt.add( this.fldBlockSize, gbcRecognizedFmt );

    this.fldBlockNumFmt = GUIFactory.createTextField();
    this.fldBlockNumFmt.setEditable( false );
    gbcRecognizedFmt.insets.bottom = 5;
    gbcRecognizedFmt.gridy++;
    panelRecognizedFmt.add( this.fldBlockNumFmt, gbcRecognizedFmt );

    // Bereich Sektor
    JPanel panelSector = GUIFactory.createPanel( new GridBagLayout() );
    panelSector.setBorder( GUIFactory.createTitledBorder( "Sektor" ) );
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

    panelSector.add(
		GUIFactory.createLabel( "Sektorposition:" ),
		gbcSector );
    gbcSector.gridy++;
    panelSector.add( GUIFactory.createLabel( "Sektor-ID:" ), gbcSector );
    gbcSector.insets.bottom = 5;
    gbcSector.gridy++;
    panelSector.add( GUIFactory.createLabel( "Sonstiges:" ), gbcSector );

    this.fldSectorPos = GUIFactory.createTextField();
    this.fldSectorPos.setEditable( false );
    gbcSector.fill          = GridBagConstraints.HORIZONTAL;
    gbcSector.weightx       = 1.0;
    gbcSector.insets.bottom = 0;
    gbcSector.gridy         = 0;
    gbcSector.gridx++;
    panelSector.add( this.fldSectorPos, gbcSector );

    this.fldSectorID = GUIFactory.createTextField();
    this.fldSectorID.setEditable( false );
    gbcSector.gridy++;
    panelSector.add( this.fldSectorID, gbcSector );

    this.fldSectorEtc = GUIFactory.createTextField();
    this.fldSectorEtc.setEditable( false );
    gbcSector.insets.bottom = 5;
    gbcSector.gridy++;
    panelSector.add( this.fldSectorEtc, gbcSector );

    this.sectorDataInfo = GUIFactory.createLabel(
		"Bitte in der linken Ansicht einen Sektor anklicken" );
    this.sectorDataInfo.setHorizontalAlignment( SwingConstants.CENTER );
    this.sectorDataInfo.setVerticalAlignment( SwingConstants.CENTER );
    this.fldSectorData = new HexCharFld( this );
    GUIFactory.initFont( this.fldSectorData );

    JPanel sectorDataPlaceholder = GUIFactory.createPanel();
    sectorDataPlaceholder.setPreferredSize(
		new Dimension( PREF_SECTORDATA_W, PREF_SECTORDATA_H ) );
    this.spSectorData   = GUIFactory.createScrollPane(
					sectorDataPlaceholder );
    gbcSector.fill      = GridBagConstraints.BOTH;
    gbcSector.weighty   = 1.0;
    gbcSector.gridwidth = GridBagConstraints.REMAINDER;
    gbcSector.gridx     = 0;
    gbcSector.gridy++;
    panelSector.add( this.spSectorData, gbcSector );

    JPanel panelSectorBtns = GUIFactory.createPanel(
					new GridLayout( 1, 2, 5, 5 ) );
    gbcSector.anchor       = GridBagConstraints.CENTER;
    gbcSector.fill         = GridBagConstraints.NONE;
    gbcSector.weightx      = 0.0;
    gbcSector.weighty      = 0.0;
    gbcSector.gridy++;
    panelSector.add( panelSectorBtns, gbcSector );

    this.btnSectorCopy = GUIFactory.createButton( "Sektordaten kopieren" );
    this.btnSectorCopy.setEnabled( false );
    panelSectorBtns.add( this.btnSectorCopy );

    this.btnSectorExport = GUIFactory.createButton(
					"Sektordaten exportieren..." );
    this.btnSectorExport.setEnabled( false );
    panelSectorBtns.add( this.btnSectorExport );


    // Listener
    this.fldFileContent.addHyperlinkListener( this );
    this.fldSectorData.addCaretListener( this );
    this.btnSectorCopy.addActionListener( this );
    this.btnSectorExport.addActionListener( this );


    // Drag&Drop aktivieren
    (new DropTarget( this.fldFileContent, this )).setActive( true );


    // sonstiges
    resetFind();
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      pack();
      setLocationByPlatform( true );
    }
    this.fldFileContent.setPreferredSize( null );
  }


	/* --- Aktionen --- */

  private void doFileOpen()
  {
    openFile( FileUtil.showFileOpenDlg(
			this,
			"Diskettenabbilddatei \u00F6ffnen",
			Main.getLastDirFile( Main.FILE_GROUP_DISK ),
			FileUtil.getPlainDiskFileFilter(),
			FileUtil.getAnaDiskFileFilter(),
			FileUtil.getCopyQMFileFilter(),
			FileUtil.getDskFileFilter(),
			FileUtil.getImageDiskFileFilter(),
			FileUtil.getTeleDiskFileFilter() ) );
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
	  doFindNext( false );
	  this.mnuFindNext.setEnabled( true );
	  this.mnuFindPrev.setEnabled( true );
	}
      }
    }
  }


  private void doFindNext( boolean backwards )
  {
    if( (this.disk != null)
	&& (this.findBytes != null)
	&& (this.findBytes.length > 0) )
    {
      try {
	if( this.lastFound ) {
	  this.findWraps = 2;
	  if( backwards ) {
	    decFindDataPos();
	  } else {
	    incFindDataPos();
	  }
	}
	this.findWraps = 2;
	for(;;) {
	  SectorData sector = getFindSector();
	  if( sector != null ) {
	    if( backwards ) {
	      for( int i = this.findDataPos; i >= 0; --i ) {
		if( equalsFindBytesAt( sector, i ) ) {
		  this.findDataPos = i;
		  showFoundBytes();
		  return;		// Suche beenden
		}
	      }
	      decFindDataPos();		// weitersuchen
	    } else {
	      int dataLen = sector.getDataLength();
	      for( int i = this.findDataPos; i < dataLen; i++ ) {
		if( equalsFindBytesAt( sector, i ) ) {
		  this.findDataPos = i;
		  showFoundBytes();
		  return;		// Suche beenden
		}
	      }
	      incFindDataPos();		// weitersuchen
	    }
	  } else {
	    // ab dem naechsten Sektor weitersuchen
	    if( backwards ) {
	      decFindSectorIdx();
	    } else {
	      incFindSectorIdx();
	    }
	  }
	}
      }
      catch( NotFoundException ex ) {
	if( this.disk != null ) {
	  BaseDlg.showInfoDlg( this, "Byte-Folge nicht gefunden" );
	}
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
	    buf.append( (i % 16) == 0 ? '\n' : '\u0020' );
	  }
	  buf.append( String.format( "%02X", sector.getDataByte( i ) ) );
	}
	buf.append( '\n' );
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
	String fName = String.format(
				"%ssector_%d_%d_%d_%d.bin",
				this.exportPrefix != null ?
					this.exportPrefix
					: "",
				sector.getCylinder(),
				sector.getHead(),
				sector.getSectorNum(),
				sector.getSizeCode() );
	File file = FileUtil.showFileSaveDlg(
				this,
				"Sektordaten exportieren",
				dirFile != null ? 
					new File( dirFile, fName )
					: new File( fName ),
				FileUtil.getBinaryFileFilter() );
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
	      EmuUtil.closeSilently( out );
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


  private void decFindDataPos() throws NotFoundException
  {
    SectorData sector = getFindSector();
    if( sector != null ) {
      --this.findDataPos;
      if( this.findDataPos < 0 ) {
	decFindSectorIdx();
      }
    } else {
      decFindSectorIdx();
    }
  }


  private void decFindSectorIdx() throws NotFoundException
  {
    if( this.disk == null ) {
      throw new NotFoundException();
    }
    this.findDataPos    = 0;
    this.findSectorData = null;
    if( this.findSectorIdx > 0 ) {
      --this.findSectorIdx;
    } else {
      for(;;) {
	if( this.findHead > 0 ) {
	  --this.findHead;
	} else {
	  --this.findCyl;
	  if( this.findCyl < 0 ) {
	    this.findCyl = getFindCyls() - 1;
	    --this.findWraps;
	    if( (this.findCyl < 0) || (this.findWraps <= 0) ) {
	      this.findCyl = 0;
	      throw new NotFoundException();
	    }
	  }
	  int sides = getFindSides();
	  if( sides < 1 ) {
	    throw new NotFoundException();
	  }
	  this.findHead = sides - 1;
	}
	int n = this.disk.getSectorsOfTrack( this.findCyl, this.findHead );
	if( n > 0 ) {
	  this.findSectorIdx = n - 1;
	  break;
	}
      }
    }
    SectorData sector = getFindSector();
    if( sector != null ) {
      int dataLen = sector.getDataLength();
      if( dataLen > 0 ) {
	this.findDataPos = dataLen - 1;
      }
    }
  }


  private boolean equalsFindBytesAt( SectorData sector, int pos )
  {
    int dataLen = sector.getDataLength();
    for( int i = 0; i < this.findBytes.length; i++ ) {
      int p = pos + i;
      if( p >= dataLen ) {
	return false;
      }
      if( (byte) sector.getDataByte( p ) != this.findBytes[ i ] ) {
	return false;
      }
    }
    return true;
  }


  private void incFindDataPos() throws NotFoundException
  {
    SectorData sector = getFindSector();
    if( sector != null ) {
      this.findDataPos++;
      if( this.findDataPos >= sector.getDataLength() ) {
	incFindSectorIdx();
      }
    } else {
      incFindSectorIdx();
    }
  }


  private void incFindSectorIdx() throws NotFoundException
  {
    this.findDataPos    = 0;
    this.findSectorData = null;
    this.findSectorIdx++;
    if( this.findSectorIdx >= getFindSectorsOfTrack() ) {
      this.findSectorIdx = 0;
      this.findHead++;
      if( this.findHead >= getFindSides() ) {
	this.findHead = 0;
	this.findCyl++;
	if( this.findCyl >= getFindCyls() ) {
	  this.findCyl = 0;
	  --this.findWraps;
	  if( this.findWraps <= 0 ) {
	    throw new NotFoundException();
	  }
	}
      }
    }
  }


  private int getFindCyls() throws NotFoundException
  {
    if( this.disk == null ) {
      throw new NotFoundException();
    }
    return this.disk.getCylinders();
  }


  private SectorData getFindSector() throws NotFoundException
  {
    if( this.disk == null ) {
      throw new NotFoundException();
    }
    if( this.findSectorData == null ) {
      this.findSectorData = this.disk.getSectorByIndex(
						this.findCyl,
						this.findHead,
						this.findSectorIdx );
    }
    return this.findSectorData;
  }


  private int getFindSectorsOfTrack() throws NotFoundException
  {
    if( this.disk == null ) {
      throw new NotFoundException();
    }
    return this.disk.getSectorsOfTrack( this.findCyl, this.findHead );
  }


  private int getFindSides() throws NotFoundException
  {
    if( this.disk == null ) {
      throw new NotFoundException();
    }
    return this.disk.getSides();
  }


  private void openFile( File file )
  {
    if( file != null ) {
      try {
	AbstractFloppyDisk disk = DiskUtil.readDiskFile( this, file, true );
	if( disk != null ) {
	  AbstractFloppyDisk disk2 = null;
	  if( disk.isRepaired() ) {
	    String msg = disk.getWarningText();
	    if( msg == null ) {
	      msg = "Die Datei enth\u00E4lt mysteri\u00F6se Daten, die "
			+ Main.APPNAME + " beim Laden repariert hat.";
	    }
	    msg = msg + "\nDie Datei selbst wurde durch die Reparatur"
			+ " nicht ge\u00E4ndert.\n"
			+ "M\u00F6chten Sie den reparierten oder"
			+ " den originalen Dateiinhalt sehen?\n";
	    switch( BaseDlg.showOptionDlg(
				this,
				msg,
				"Dateireparatur",
				"Reparierter Inhalt",
				"Originaler Inhalt",
				EmuUtil.TEXT_CANCEL ) )
	    {
	      case 0:
		// leer
		break;
	      case 1:
		disk = DiskUtil.readDiskFile( this, file, false );
		break;
	      default:
		disk = null;
	    }
	  }
	}
	if( disk != null ) {

	  // alte Abbilddatei schliessen und neue uebernehmen
	  if( this.disk != null ) {
	    this.disk.closeSilently();
	  }
	  this.disk = disk;
	  this.file = file;
	  Main.setLastFile( file, Main.FILE_GROUP_DISK );
	  setTitle( TITLE + ": " + file.getPath() );

	  // Allgemeine Infos
	  int cyls            = disk.getCylinders();
	  int sides           = disk.getSides();
	  int sectorsPerTrack = disk.getSectorsPerTrack();
	  int sectorSize      = disk.getSectorSize();

	  setText( this.fldFileName, file.getName() );
	  setText(
		this.fldPhysFormat,
		String.format(
			"%d KByte brutto (%d x %d x %d x %d)",
			cyls * sides * sectorSize * sectorsPerTrack / 1024,
			cyls,
			sides,
			sectorsPerTrack,
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
	  StringBuilder buf            = new StringBuilder( 0x2000 );
	  float         sysTracks      = 0F;
	  int           blockSize      = DiskUtil.DEFAULT_BLOCK_SIZE;
	  Boolean       blkNum16Bit    = null;
	  AtomicInteger rvSysBytes     = new AtomicInteger( -1 );
	  AtomicInteger rvBlockNumSize = new AtomicInteger( -1 );
	  AtomicInteger rvBlockSize    = new AtomicInteger( -1 );
	  byte[]        dirBytes       = DiskUtil.findAndReadDirBytes(
								null,
								null,
								null,
								disk,
								rvSysBytes );
	  if( rvSysBytes.get() > 0 ) {
	    int bytesPerTrack = disk.getSides()
					* disk.getSectorsPerTrack()
					* disk.getSectorSize();
	    if( bytesPerTrack > 0 ) {
	      sysTracks = rvSysBytes.floatValue() / (float) bytesPerTrack;
	    }
	  }
	  if( DiskUtil.recognizeBlockNumFmt(
				dirBytes,
				disk.getDiskSize(),
				rvBlockNumSize,
				rvBlockSize ) )
	  {
	    blockSize   = rvBlockSize.get();
	    blkNum16Bit = Boolean.valueOf( rvBlockNumSize.get() == 16 );
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
	    boolean hasBogusIdSectors     = false;
	    boolean hasNoDataSectors      = false;
	    boolean hasDeletedDataSectors = false;
	    boolean hasErrorSectors       = false;
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
		int n = disk.getSectorsOfTrack( physCyl, physHead );
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
		    buf.append( ':' );
		    buf.append( physHead );
		    buf.append( ':' );
		    buf.append( i );
		    buf.append( "\">[" );
		    if( sector.hasBogusID() ) {
		      hasBogusIdSectors = true;
		      buf.append( MARK_BEG );
		      buf.append( sector.getSectorNum() );
		      buf.append( MARK_BOGUS_ID );
		      buf.append( MARK_END );
		    } else {
		      buf.append( sector.getSectorNum() );
		    }
		    int c = sector.getCylinder();
		    if( c != physCyl ) {
		      buf.append( ',' );
		      buf.append( MARK_BEG );
		      buf.append( "c=" );
		      buf.append( c );
		      buf.append( MARK_END );
		    }
		    int h = sector.getHead();
		    if( h != physHead ) {
		      buf.append( ',' );
		      buf.append( MARK_BEG );
		      buf.append( "h=" );
		      buf.append( h );
		      buf.append( MARK_END );
		    }
		    int dataLen = sector.getDataLength();
		    if( dataLen != sectorSize ) {
		      buf.append( ',' );
		      buf.append( MARK_BEG );
		      buf.append( "n=" );
		      buf.append( sector.getSizeCode() );
		      buf.append( MARK_END );
		    }
		    if( dataLen == 0 ) {
		      hasNoDataSectors = true;
		      buf.append( ',' );
		      buf.append( MARK_NO_DATA );
		    }
		    if( sector.getDataDeleted() ) {
		      hasDeletedDataSectors = true;
		      buf.append( ',' );
		      buf.append( MARK_DELETED );
		    }
		    if( sector.checkError() ) {
		      hasErrorSectors = true;
		      buf.append( ',' );
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
			|| hasDeletedDataSectors
			|| hasErrorSectors )
	    {
	      buf.append( "<br/>\n"
			+ "Agenda:<br/>\n"
			+ "<table border=\"0\">\n" );
	      if( hasBogusIdSectors ) {
		buf.append( "<tr><td valign=\"top\">" );
		buf.append( MARK_BEG );
		buf.append( MARK_BOGUS_ID );
		buf.append( MARK_END );
		buf.append( ":</td><td valign=\"top\">Sektor-ID generiert,"
			+ " da Sektorkopf nicht gelesen werden konnte"
			+ "</td></tr>\n" );
	      }
	      if( hasNoDataSectors ) {
		buf.append( "<tr><td valign=\"top\">" );
		buf.append( MARK_NO_DATA );
		buf.append( ":</td><td valign=\"top\">"
			+ "Sektor ohne Datenbereich</td></tr>\n" );
	      }
	      if( hasDeletedDataSectors ) {
		buf.append( "<tr><td valign=\"top\">" );
		buf.append( MARK_DELETED );
		buf.append( ":</td><td valign=\"top\">"
			+ "Sektor mit <em>Deleted Data Address Mark</em>"
			+ "</td></tr>\n" );
	      }
	      if( hasDeletedDataSectors ) {
		buf.append( "<tr><td>" );
		buf.append( MARK_ERROR );
		buf.append( ":</td><td valign=\"top\">"
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
	  if( (cyls > 0) && (sides > 0)
	      && (sectorsPerTrack > 0) && (sectorSize > 0) )
	  {
	    this.mnuExportTracks.setEnabled( true );
	  }
	  this.mnuFind.setEnabled( true );

	  this.exportPrefix = file.getName();
	  if( this.exportPrefix != null ) {
	    int pos = this.exportPrefix.indexOf( '.' );
	    if( pos > 0 ) {
	      this.exportPrefix = this.exportPrefix.substring( 0, pos )
							+ "_";
	    }
	  }
	}
      }
      catch( IOException ex ) {
	BaseDlg.showErrorDlg( this, ex );
      }
    }
  }


  private void resetFind()
  {
    this.lastFound      = false;
    this.lastBigEndian  = false;
    this.lastInputFmt   = null;
    this.lastFindText   = null;
    this.findBytes      = null;
    this.findCyl        = 0;
    this.findDataPos    = 0;
    this.findHead       = 0;
    this.findWraps      = 0;
    this.findSectorIdx  = 0;
    this.findSectorData = null;
    this.mnuFindNext.setEnabled( false );
    this.mnuFindPrev.setEnabled( false );
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


  private void showFoundBytes()
  {
    this.lastFound = true;

    // Sektor anzeigen
    showSector( this.findCyl, this.findHead, this.findSectorIdx );

    /*
     * gefundene Bytes rueckwaerts selektieren,
     * damit der Cursor auf der ersten,
     * d.h. der gefundenen Position steht
     */
    final int        p1  = this.findDataPos + this.findBytes.length - 1;
    final int        p2  = this.findDataPos;
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
      if( sector.getDataDeleted()
	  || sector.checkError()
	  || sector.hasBogusID() )
      {
	StringBuilder buf = new StringBuilder( 128 );
	if( sector.getDataDeleted() ) {
	  buf.append( "Daten als gel\u00F6scht markiert" );
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
