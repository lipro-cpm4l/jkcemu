/*
 * (c) 2018-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Exportieren von Spuren einer Diskettenabbilddatei
 */

package jkcemu.disk;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.PopupMenuOwner;
import jkcemu.file.FileUtil;
import jkcemu.text.LogTextActionMngr;


public class TrackExportDlg
			extends BaseDlg
			implements ChangeListener, PopupMenuOwner
{
  private static String lastFileName   = null;
  private static String lastTrackRange = null;

  private AbstractFloppyDisk disk;
  private String             exportPrefix;
  private LogTextActionMngr  actionMngr;
  private JTextArea          fldLog;
  private JSpinner           spinnerCylFrom;
  private JSpinner           spinnerCylTo;
  private JRadioButton       rbSide0;
  private JRadioButton       rbSide1;
  private JRadioButton       rbSidesAll;
  private JButton            btnExport;
  private JButton            btnClose;


  public static void exportTracks(
			Window             owner,
			AbstractFloppyDisk disk,
			String             exportPrefix )
  {
    if( disk != null )
      (new TrackExportDlg(
			owner,
			disk,
			exportPrefix )).setVisible( true );
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    Object src = e.getSource();
    if( src == this.spinnerCylFrom ) {
      int cylFrom = EmuUtil.getInt( this.spinnerCylFrom );
      int cylTo   = EmuUtil.getInt( this.spinnerCylTo );
      if( cylTo < cylFrom ) {
	try {
	  this.spinnerCylTo.setValue( cylFrom );
	  cylTo = cylFrom;
	}
	catch( IllegalArgumentException ex ) {}
      }
    } else if( src == this.spinnerCylTo ) {
      int cylFrom = EmuUtil.getInt( this.spinnerCylFrom );
      int cylTo   = EmuUtil.getInt( this.spinnerCylTo );
      if( cylTo < cylFrom ) {
	try {
	  this.spinnerCylFrom.setValue( cylTo );
	  cylFrom = cylTo;
	}
	catch( IllegalArgumentException ex ) {}
      }
    }
  }


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.actionMngr.getPopupMenu();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.btnExport ) {
      rv = true;
      doExport();
    } else if( src == this.btnClose ) {
      rv = true;
      doClose();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.spinnerCylFrom.removeChangeListener( this );
      this.spinnerCylTo.removeChangeListener( this );
      this.btnExport.removeActionListener( this );
      this.btnClose.removeActionListener( this );
      this.fldLog.removeMouseListener( this );
    }
    return rv;
  }


  @Override
  protected boolean showPopupMenu( MouseEvent e )
  {
    return this.actionMngr.showPopupMenu( e );
  }


	/* --- Konstruktor --- */

  private TrackExportDlg(
		Window             owner,
		AbstractFloppyDisk disk,
		String             exportPrefix )
  {
    super( owner, "Spuren exportieren" );
    this.disk         = disk;
    this.exportPrefix = (exportPrefix != null ? exportPrefix : "");


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    add( GUIFactory.createLabel( "Von Spur:" ), gbc );

    int cylTo = disk.getCylinders();
    if( cylTo > 0 ) {
      --cylTo;
    }
    this.spinnerCylFrom = GUIFactory.createSpinner(
		new SpinnerNumberModel( 0, 0, cylTo, 1 ) );
    gbc.gridx++;
    add( this.spinnerCylFrom, gbc );

    gbc.gridx++;
    add( GUIFactory.createLabel( "bis  Spur:" ), gbc );

    this.spinnerCylTo = GUIFactory.createSpinner(
		new SpinnerNumberModel( cylTo, 0, cylTo, 1 ) );
    gbc.gridx++;
    add( this.spinnerCylTo, gbc );

    gbc.gridx = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Seiten:" ), gbc );

    JPanel panelSides = GUIFactory.createPanel();
    panelSides.setLayout( new BoxLayout( panelSides, BoxLayout.X_AXIS ) );
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    add( panelSides, gbc );

    ButtonGroup grpSides = new ButtonGroup();

    this.rbSidesAll = GUIFactory.createRadioButton( "Alle", true );
    grpSides.add( this.rbSidesAll );
    panelSides.add( this.rbSidesAll );
    panelSides.add( Box.createHorizontalStrut( 5 ) );

    this.rbSide0 = GUIFactory.createRadioButton( "1" );
    grpSides.add( this.rbSide0 );
    panelSides.add( this.rbSide0 );
    panelSides.add( Box.createHorizontalStrut( 5 ) );

    this.rbSide1 = GUIFactory.createRadioButton( "2" );
    grpSides.add( this.rbSide1 );
    panelSides.add( this.rbSide1 );

    gbc.insets.bottom = 0;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Ergebnis:" ), gbc );

    this.fldLog = GUIFactory.createTextArea( 8, 0 );
    this.fldLog.setEditable( false );
    gbc.fill          = GridBagConstraints.BOTH;
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.weightx       = 1.0;
    gbc.weighty       = 1.0;
    gbc.gridy++;
    add( GUIFactory.createScrollPane( this.fldLog ), gbc );

    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.insets.top  = 10;
    gbc.weightx     = 0.0;
    gbc.weighty     = 0.0;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnExport = GUIFactory.createButton( "Exportieren" );
    panelBtn.add( this.btnExport );

    this.btnClose = GUIFactory.createButtonClose();
    panelBtn.add( this.btnClose );


    // Fenstergroesse und -position
    pack();
    this.fldLog.setRows( 0 );
    setResizable( true );
    setParentCentered();


    // Aktionen im Popup-Menu
    this.actionMngr = new LogTextActionMngr( this.fldLog, true );


    // Listener
    this.spinnerCylFrom.addChangeListener( this );
    this.spinnerCylTo.addChangeListener( this );
    this.btnExport.addActionListener( this );
    this.btnClose.addActionListener( this );
    this.fldLog.addMouseListener( this );
  }


	/* --- private Methoden --- */

  private void doExport()
  {
    int cylFrom = EmuUtil.getInt( this.spinnerCylFrom );
    int cylTo   = EmuUtil.getInt( this.spinnerCylTo );
    if( cylFrom <= cylTo )  {
      String trackRange = String.format( "%d_%d", cylFrom, cylTo );
      String fileName   = String.format(
				"%stracks_%s.bin",
				this.exportPrefix,
				trackRange );
      if( (lastFileName != null) && (lastTrackRange != null) ) {
	fileName = lastFileName.replace( lastTrackRange, trackRange );
      }
      File dirFile = Main.getLastDirFile( Main.FILE_GROUP_SECTOR );
      File file    = FileUtil.showFileSaveDlg(
				this,
				"Spuren exportieren",
				dirFile != null ? 
					new File( dirFile, fileName )
					: new File( fileName ),
				FileUtil.getBinaryFileFilter() );
      if( file != null ) {
	this.fldLog.setText( "" );
	boolean      success   = false;
	int          totalSize = 0;
	OutputStream out       = null;
	try {
	  out = new BufferedOutputStream( new FileOutputStream( file ) );

	  int headFrom  = 0;
	  int headTo    = 0;
	  if( this.disk.getSides() > 1 ) {
	    if( this.rbSide1.isSelected() ) {
	      headFrom = 1;
	      headTo   = 1;
	    } else if( this.rbSidesAll.isSelected() ) {
	      headTo = 1;
	    }
	  }
	  boolean       legendCyl     = false;
	  boolean       legendHead    = false;
	  boolean       legendSize    = false;
	  boolean       legendDel     = false;
	  boolean       legendErr     = false;
	  boolean       legendBogusID = false;
	  StringBuilder buf           = new StringBuilder( 0x0800 );
	  for( int cyl = cylFrom; cyl <= cylTo; cyl++ ) {
	    int trackSize = 0;
	    buf.setLength( 0 );
	    for( int head = headFrom; head <= headTo; head++ ) {
	      if( buf.length() > 0 ) {
		buf.append( " |" );
	      }
	      Map<Integer,SectorData> sectors = getBestSectors( cyl, head );
	      SortedSet<Integer>      secNums = new TreeSet<>();
	      secNums.addAll( sectors.keySet() );
	      for( Integer secNum : secNums ) {
		SectorData sector = sectors.get( secNum );
		if( sector != null ) {
		  int nWritten = sector.writeTo( out, -1 );
		  trackSize += nWritten;
		  buf.append(
			String.format(
				" %d(%d",
				sector.getSectorNum(),
				nWritten ) );
		  if( sector.getCylinder() != cyl ) {
		    buf.append( ",c?" );
		    legendCyl = true;
		  }
		  if( sector.getHead() != head ) {
		    buf.append( ",h?" );
		    legendHead = true;
		  }
		  if( sector.hasBogusID() ) {
		    buf.append( ",r?" );
		    legendBogusID = true;
		  }
		  if( nWritten != SectorData.getSizeBySizeCode(
						sector.getSizeCode() ) )
		  {
		    buf.append( ",n?" );
		    legendSize = true;
		  }
		  if( sector.getDataDeleted() ) {
		    buf.append( ",del" );
		    legendDel = true;
		  }
		  if( sector.checkError() ) {
		    buf.append( ",err" );
		    legendErr = true;
		  }
		  buf.append( ')' );
		}
	      }
	    }
	    totalSize += trackSize;
	    this.fldLog.append(
		String.format( "Spur %d: %d Bytes", cyl, trackSize ) );
	    if( buf.length() > 2 ) {
	      this.fldLog.append( ", Sektoren:" );
	      this.fldLog.append( buf.toString() );
	    }
	    this.fldLog.append( "\n" );
	  }
	  if( legendCyl || legendHead || legendSize
	      || legendDel || legendErr || legendBogusID )
	  {
	    this.fldLog.append( "\nLegende:\n" );
	    if( legendCyl ) {
	      this.fldLog.append( "  c?:  Spur-Nr. in der Sektor-ID stimmt"
			+ " nicht mit der physischen Spur \u00FCberein\n" );
	    }
	    if( legendHead ) {
	      this.fldLog.append( "  h?:  Kopf-Nr. in der Sektor-ID stimmt"
			+ " nicht mit der physischen Seite \u00FCberein\n" );
	    }
	    if( legendBogusID ) {
	      this.fldLog.append( "  r?:  Sektor-ID konnte nicht gelesen"
			+ " werden und wurde deshalb generiert"
			+ " (erfunden)\n" );
	    }
	    if( legendSize ) {
	      this.fldLog.append( "  n?:  Gr\u00F6\u00DFe in der Sektor-ID"
			+ " stimmt nicht mit der realen Sektorgr\u00F6\u00DFe"
			+ " (Anzahl Bytes) \u00FCberein\n" );
	    }
	    if( legendDel ) {
	      this.fldLog.append( "  del: Sektor hat L\u00F6schmarkierung"
			+ " (Deleted Data Address Mark)\n" );
	    }
	    if( legendErr ) {
	      this.fldLog.append( "  err: Sektor wurde mit CRC-Fehler"
			+ " gelesen\n" );
	    }
	  }
	  success        = true;
	  lastTrackRange = null;
	  lastFileName   = file.getName();
	  if( lastFileName != null ) {
	    if( lastFileName.indexOf( trackRange ) >= 0 ) {
	      lastTrackRange = trackRange;
	    }
	  }
	  Main.setLastFile( file, Main.FILE_GROUP_SECTOR );
	}
	catch( IOException ex ) {
	  this.fldLog.append( "\n\nExport fehlgeschlagen" );
	  String msg = ex.getMessage();
	  if( msg != null ) {
	    if( !msg.isEmpty() ) {
	      this.fldLog.append( ":\n" );
	      this.fldLog.append( msg );
	    }
	  }
	  this.fldLog.append( "\n" );
	  success = false;
	}
	finally {
	  EmuUtil.closeSilently( out );
	}
	if( success ) {
	  this.fldLog.append(
		String.format(
			"\n\n%d Byte%s exportiert\n",
			totalSize,
			totalSize == 1 ? "" : "s" ) );
	}
      }
    }
  }


  private Map<Integer,SectorData> getBestSectors( int cyl, int head )
  {
    Map<Integer,SectorData> sectorMap = new HashMap<>();

    int n = this.disk.getSectorsOfTrack( cyl, head );
    for( int i = 0; i < n; i++ ) {
      SectorData sector = this.disk.getSectorByIndex( cyl, head, i );
      if( sector != null ) {
	int        sectorNum = sector.getSectorNum();
	SectorData oldSector = sectorMap.get( sectorNum );
	if( oldSector != null ) {
	  if( getSectorRating( sector, cyl, head )
			> getSectorRating( sector, cyl, head ) )
	  {
	    sectorMap.put( sectorNum, sector );
	  }
	} else {
	  sectorMap.put( sectorNum, sector );
	}
      }
    }
    return sectorMap;
  }


  private int getSectorRating( SectorData sector, int cyl, int head )
  {
    int rv = 0;
    if( sector.getCylinder() == cyl ) {
      rv += 3;
    }
    if( sector.getHead() == head ) {
      rv += 3;
    }
    if( !sector.checkError() ) {
      rv += 2;
    }
    if( !sector.getDataDeleted() ) {
      rv++;
    }
    return rv;
  }
}
