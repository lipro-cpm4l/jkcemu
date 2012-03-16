/*
 * (c) 2010-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen fuer Festplatten
 */

package jkcemu.disk;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class HardDiskSettingsFld extends AbstractSettingsFld
{
  private static final int DISK_CNT = 2;

  private int            popupDiskIdx;
  private JPopupMenu     popupMenu;
  private JMenuItem      mnuDiskNone;
  private HardDiskInfo[] diskTypes;
  private JLabel[]       titleLabels;
  private JTextField[]   diskTypeFlds;
  private JButton[]      diskTypeBtns;
  private FileNameFld[]  fileNameFlds;
  private JButton[]      selectBtns;


  public HardDiskSettingsFld(
			SettingsFrm settingsFrm,
			String      propPrefix )
  {
    super( settingsFrm, propPrefix );
    this.popupDiskIdx  = -1;
    this.popupMenu     = null;
    this.mnuDiskNone   = null;
    this.diskTypes     = new HardDiskInfo[ DISK_CNT ];
    this.titleLabels   = new JLabel[ DISK_CNT ];
    this.diskTypeFlds  = new JTextField[ DISK_CNT ];
    this.diskTypeBtns  = new JButton[ DISK_CNT ];
    this.fileNameFlds  = new FileNameFld[ DISK_CNT ];
    this.selectBtns    = new JButton[ DISK_CNT ];


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

    for( int i = 0; i < DISK_CNT; i++ ) {
      addDiskFlds( i, gbc );
      gbc.gridy++;
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    try {
      if( props != null ) {
	for( int i = 0; i < DISK_CNT; i++ ) {
	  String       prefix = createPrefix( i );
	  HardDiskInfo info   = this.diskTypes[ i ];
	  if( info != null ) {
	    File file = this.fileNameFlds[ i ].getFile();
	    if( file != null ) {
	      props.setProperty( prefix + "file", file.getPath() );
	    } else {
	      props.setProperty( prefix + "file", "" );
	      if( selected ) {
		throw new UserInputException(
		    String.format(
			"%d. Festplatte: Abbilddatei nicht ausgew\u00E4hlt",
			i + 1 ) );
	      }
	    }
	    EmuUtil.setProperty(
			props,
			prefix + "model",
			info.getFullDiskModel() );
	    EmuUtil.setProperty(
			props,
			prefix + "cylinders",
			info.getCylinders() );
	    EmuUtil.setProperty(
			props,
			prefix + "heads",
			info.getHeads() );
	    EmuUtil.setProperty(
			props,
			prefix + "sectors_per_track",
			info.getSectorsPerTrack() );
	  } else {
	    props.setProperty( prefix + "model", "" );
	    props.setProperty( prefix + "cylinders", "" );
	    props.setProperty( prefix + "heads", "" );
	    props.setProperty( prefix + "sectors_per_track", "" );
	    props.setProperty( prefix + "file", "" );
	  }
	}
      }
    }
    catch( NumberFormatException ex ) {
      throw new UserInputException( ex.getMessage() );
    }
  }


  @Override
  public boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      int idx = indexOf( this.diskTypeBtns, src );
      if( idx >= 0 ) {
	rv = true;
	showDiskTypePopup( idx );
      }
      if( !rv ) {
	idx = indexOf( this.selectBtns, src );
	if( idx >= 0 ) {
	  rv = true;
	  doFileSelect( idx );
	}
      }
      if( !rv && (e instanceof ActionEvent) ) {
	String actionCmd = ((ActionEvent) e).getActionCommand();
	if( actionCmd != null ) {
	  if( actionCmd.startsWith( "harddisk." ) ) {
	    rv = true;
	    if( actionCmd.equals( "harddisk.select" ) ) {
	      HardDiskInfo info = HardDiskListDlg.showHardDiskListDlg(
							this.settingsFrm );
	      if( info != null ) {
		idx = this.popupDiskIdx;
		if( (idx >= 0) && (idx < this.diskTypes.length) ) {
		  this.diskTypes[ idx ] = info;
		  this.diskTypeFlds[ idx ].setText( info.toString() );
		  try {
		    this.diskTypeFlds[ idx ].setCaretPosition( 0 );
		  }
		  catch( IllegalArgumentException ex ) {}
		  updFieldsEnabled();
		  fireDataChanged();
		}
	      }
	    } else {
	      idx = this.popupDiskIdx;
	      if( (idx >= 0) && (idx < this.diskTypes.length) ) {
		if( actionCmd.equals( "harddisk.none" ) ) {
		  this.diskTypes[ idx ] = null;
		  this.diskTypeFlds[ idx ].setText( "" );
		  this.fileNameFlds[ idx ].setFile( null );
		  updFieldsEnabled();
		  fireDataChanged();
		}
	      }
	    }
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    boolean           rejected = false;
    DropTargetContext context  = e.getDropTargetContext();
    if( context != null ) {
      Component c = context.getComponent();
      for( int i = 0; i < DISK_CNT; i++ ) {
	if( c == this.fileNameFlds[ i ] ) {
	  if( !this.fileNameFlds[ i ].isEnabled() ) {
	    e.rejectDrag();
	    rejected = true;
	    break;
	  }
	}
      }
    }
    if( !rejected ) {
      super.dragEnter( e );
    }
  }


  @Override
  public boolean fileDropped( Component c, File file )
  {
    boolean rv = false;
    if( (c != null) && (file != null) ) {
      for( int i = 0; i < DISK_CNT; i++ ) {
	if( c == this.fileNameFlds[ i ] ) {
	  if( this.fileNameFlds[ i ].isEnabled() ) {
	    this.fileNameFlds[ i ].setFile( file );
	    updFieldsEnabled();
	    Main.setLastFile( file, "disk" );
	    rv = true;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    JPopupMenu menu = popupMenu;
    if( menu != null ) {
      SwingUtilities.updateComponentTreeUI( menu );
    }
  }


  @Override
  public void setEnabled( boolean state )
  {
    super.setEnabled( state );
    updFieldsEnabled();
  }


  @Override
  public void updFields( Properties props )
  {
    for( int i = 0; i < DISK_CNT; i++ ) {
      String prefix    = createPrefix( i );
      String diskModel = EmuUtil.getProperty( props, prefix + "model" );
      if( !diskModel.isEmpty() ) {
	int c = EmuUtil.getIntProperty( props, prefix + "cylinders", 0 );
	int h = EmuUtil.getIntProperty( props, prefix + "heads", 0 );
	int n = EmuUtil.getIntProperty(
				props,
				prefix + "sectors_per_track",
				0 );
	if( (c > 0) && (h > 0) && (n > 0) ) {
	  try {
	    this.diskTypes[ i ] = new HardDiskInfo( null, diskModel, c, h, n );
	    String fName = EmuUtil.getProperty( props, prefix + "file" );
	    this.fileNameFlds[ i ].setFile(
			fName.isEmpty() ? null : new File( fName ) );
	    this.diskTypeFlds[ i ].setText( this.diskTypes[ i ].toString() );
	  }
	  catch( IllegalArgumentException ex ) {}
	}
      }
    }
    updFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void addDiskFlds( int idx, GridBagConstraints gbc )
  {
    this.diskTypes[ idx ] = null;

    // Trennzeile
    if( idx > 0 ) {
      gbc.fill          = GridBagConstraints.HORIZONTAL;
      gbc.weightx       = 1.0;
      gbc.insets.top    = 5;
      gbc.insets.left   = 5;
      gbc.insets.right  = 5;
      gbc.insets.bottom = 5;
      gbc.gridwidth     = GridBagConstraints.REMAINDER;
      gbc.gridx         = 0;
      add( new JSeparator(), gbc );
      gbc.gridy++;
    }

    // 1. Zeile
    if( idx == 0 ) {
      this.titleLabels[ idx ] = new JLabel( "1. Festplatte (Master):" );
    } else {
      this.titleLabels[ idx ] = new JLabel(
		String.format( "%d. Festplatte (Slave):", idx + 1 ) );
    }
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.insets.top    = 5;
    gbc.insets.left   = 5;
    gbc.insets.right  = 5;
    gbc.insets.bottom = 0;
    gbc.gridwidth     = 1;
    gbc.gridx         = 0;
    add( this.titleLabels[ idx ], gbc );

    this.diskTypeFlds[ idx ] = new JTextField();
    this.diskTypeFlds[ idx ].setEditable( false );
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.insets.left = 0;
    gbc.gridx++;
    add( this.diskTypeFlds[ idx ], gbc );

    this.diskTypeBtns[ idx ] = createImageButton(
				"/images/disk/harddiskmodel.png",
				"Festplattenmodell ausw\u00E4hlen" );
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( this.diskTypeBtns[ idx ], gbc );

    // 2.Zeile
    this.fileNameFlds[ idx ] = new FileNameFld();
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.insets.left = 50;
    gbc.gridwidth   = 2;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.fileNameFlds[ idx ], gbc );

    this.selectBtns[ idx ] = createImageButton(
				"/images/file/open.png",
				"Festplattenabbilddatei ausw\u00E4hlen" );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.left = 0;
    gbc.gridwidth   = 1;
    gbc.gridx += 2;
    add( this.selectBtns[ idx ], gbc );

    enableFileDrop( this.fileNameFlds[ idx ] );
  }


  private JMenuItem addToPopupMenu( String text, String actionCmd )
  {
    JMenuItem item = new JMenuItem( text );
    item.setActionCommand( actionCmd );
    item.addActionListener( this );
    this.popupMenu.add( item );
    return item;
  }


  private String createPrefix( int idx )
  {
    return String.format( "%sharddisk.%d.", this.propPrefix, idx + 1 );
  }


  private void doFileSelect( int idx )
  {
    if( (idx >= 0) && (idx < DISK_CNT) ) {
      File preSelection = this.fileNameFlds[ idx ].getFile();
      if( preSelection == null ) {
	preSelection = Main.getLastPathFile( "disk" );
      }
      File file = EmuUtil.showFileOpenDlg(
				this.settingsFrm,
				"Festplattenabbilddatei ausw\u00E4hlen",
				preSelection );
      if( file != null ) {
	String msg = null;
	if( file.exists() ) {
	  if( !file.isFile() ) {
	    msg = "Datei ist keine regul\u00E4re Datei";
	  }
	} else {
	  BasicDlg.showInfoDlg(
		this,
		"Die angegebene Datei existiert nicht.\n"
			+ "Formatieren Sie bitte das betreffende Laufwerk"
			+ " vom emulierten System aus,\n"
			+ "damit die Datei angelegt wird"
			+ " und einen Inhalt erh\u00E4lt.",
		"Datei nicht gefunden" );
	}
	if( msg != null ) {
	  BasicDlg.showErrorDlg( this, file.getPath() + ": " + msg );
	  file = null;
	}
      }
      if( file != null ) {
	this.fileNameFlds[ idx ].setFile( file );
	updFieldsEnabled();
	Main.setLastFile( file, "disk" );
	fireDataChanged();
      }
    }
  }


  private int indexOf( Object[] a, Object o )
  {
    int rv = -1;
    for( int i = 0; i < a.length; i++ ) {
      if( o == a[ i ] ) {
	rv = i;
	break;
      }
    }
    return rv;
  }


  private void showDiskTypePopup( int idx )
  {
    if( this.popupMenu == null ) {
      this.popupMenu = new JPopupMenu();
      addToPopupMenu( "Festplatte ausw\u00E4hlen...", "harddisk.select" );
      this.popupMenu.addSeparator();
      this.mnuDiskNone = addToPopupMenu(
				"Festplatte nicht emulieren",
				"harddisk.none" );
    }
    if( this.mnuDiskNone != null ) {
      this.mnuDiskNone.setEnabled( this.diskTypes[ idx ] != null );
    }
    this.popupDiskIdx = idx;
    this.popupMenu.show(
		this.diskTypeBtns[ idx ],
		0,
		this.diskTypeBtns[ idx ].getHeight() );
  }


  public void updFieldsEnabled()
  {
    boolean stateGeneral = isEnabled();
    for( int i = 0; i < DISK_CNT; i++ ) {
      boolean state = false;
      if( stateGeneral ) {
	if( i > 0 ) {
	  if( this.diskTypes[ i - 1 ] != null ) {
	    state = true;
	  }
	} else {
	  state = true;
	}
      }
      this.titleLabels[ i ].setEnabled( state );
      this.diskTypeFlds[ i ].setEnabled( state );
      this.diskTypeBtns[ i ].setEnabled( state );

      if( state ) {
	if( this.diskTypes[ i ] == null ) {
	  state = false;
	}
      }
      this.fileNameFlds[ i ].setEnabled( state );
      this.selectBtns[ i ].setEnabled( state );
    }
  }
}

