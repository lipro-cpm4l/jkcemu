/*
 * (c) 2010-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die GIDE-Einstellungen
 */

package jkcemu.disk;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import jkcemu.Main;
import jkcemu.base.AbstractSettingsFld;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileNameFld;
import jkcemu.base.OptionDlg;
import jkcemu.base.SettingsFrm;
import jkcemu.base.UserInputException;


public class GIDESettingsFld extends AbstractSettingsFld
{
  private static final String ACTION_HD_NONE   = "hd.none";
  private static final String ACTION_HD_SELECT = "hd.select";

  private static final int DISK_CNT = 2;

  private int            popupDiskIdx;
  private JPopupMenu     popupMenu;
  private JMenuItem      mnuDiskNone;
  private JCheckBox      btnEnabled;
  private HardDiskInfo[] diskTypes;
  private JLabel[]       titleLabels;
  private JTextField[]   diskTypeFlds;
  private JButton[]      diskTypeBtns;
  private FileNameFld[]  fileNameFlds;
  private JButton[]      selectBtns;
  private int[]          offsets;


  public GIDESettingsFld(
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
    this.offsets       = new int[ DISK_CNT ];
    Arrays.fill( this.offsets, 0 );


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

    this.btnEnabled = new JCheckBox( "GIDE emulieren" );
    add( this.btnEnabled, gbc );
    gbc.gridy++;
    for( int i = 0; i < DISK_CNT; i++ ) {
      addDiskFlds( i, gbc );
      gbc.gridy++;
    }
    this.btnEnabled.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    try {
      if( props != null ) {
	EmuUtil.setProperty(
			props,
			this.propPrefix + GIDE.PROP_ENABLED,
			this.btnEnabled.isSelected() );
	for( int i = 0; i < DISK_CNT; i++ ) {
	  String       prefix = createPrefix( i );
	  HardDiskInfo info   = this.diskTypes[ i ];
	  if( info != null ) {
	    File file = this.fileNameFlds[ i ].getFile();
	    if( file != null ) {
	      props.setProperty( prefix + GIDE.PROP_FILE, file.getPath() );
	    } else {
	      props.setProperty( prefix + GIDE.PROP_FILE, "" );
	      if( selected ) {
		throw new UserInputException(
		    String.format(
			"%d. Festplatte: Abbilddatei nicht ausgew\u00E4hlt",
			i + 1 ) );
	      }
	    }
	    EmuUtil.setProperty(
			props,
			prefix + GIDE.PROP_OFFSET,
			this.offsets[ i ] );
	    EmuUtil.setProperty(
			props,
			prefix + GIDE.PROP_MODEL,
			info.getFullDiskModel() );
	    EmuUtil.setProperty(
			props,
			prefix + GIDE.PROP_CYLINDERS,
			info.getCylinders() );
	    EmuUtil.setProperty(
			props,
			prefix + GIDE.PROP_HEADS,
			info.getHeads() );
	    EmuUtil.setProperty(
			props,
			prefix + GIDE.PROP_SECTORS_PER_TRACK,
			info.getSectorsPerTrack() );
	  } else {
	    props.setProperty( prefix + GIDE.PROP_MODEL, "" );
	    props.setProperty( prefix + GIDE.PROP_CYLINDERS, "" );
	    props.setProperty( prefix + GIDE.PROP_HEADS, "" );
	    props.setProperty( prefix + GIDE.PROP_SECTORS_PER_TRACK, "" );
	    props.setProperty( prefix + GIDE.PROP_FILE, "" );
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
      if( src == this.btnEnabled ) {
	rv = true;
	updFieldsEnabled();
	fireDataChanged();
      } else {
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
	    if( actionCmd.startsWith( GIDE.PROP_HARDDISK_PREFIX ) ) {
	      rv = true;
	      if( actionCmd.equals( ACTION_HD_SELECT ) ) {
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
		  if( actionCmd.equals( ACTION_HD_NONE ) ) {
		    this.diskTypes[ idx ] = null;
		    this.diskTypeFlds[ idx ].setText( "" );
		    setFile( idx, null, 0 );
		    updFieldsEnabled();
		    fireDataChanged();
		  }
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
	    setFile( i, file, 0 );
	    updFieldsEnabled();
	    Main.setLastFile( file, Main.FILE_GROUP_DISK );
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
    this.btnEnabled.setEnabled( state );
    updFieldsEnabled();
  }


  @Override
  public void updFields( Properties props )
  {
    this.btnEnabled.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + GIDE.PROP_ENABLED,
				false ) );
    for( int i = 0; i < DISK_CNT; i++ ) {
      String prefix    = createPrefix( i );
      String diskModel = EmuUtil.getProperty(
				props,
				prefix + GIDE.PROP_MODEL );
      if( !diskModel.isEmpty() ) {
	int c = EmuUtil.getIntProperty(
				props,
				prefix + GIDE.PROP_CYLINDERS,
				0 );
	int h = EmuUtil.getIntProperty(
				props,
				prefix + GIDE.PROP_HEADS,
				0 );
	int n = EmuUtil.getIntProperty(
				props,
				prefix + GIDE.PROP_SECTORS_PER_TRACK,
				0 );
	if( (c > 0) && (h > 0) && (n > 0) ) {
	  try {
	    this.diskTypes[ i ] = new HardDiskInfo( null, diskModel, c, h, n );
	    String fName = EmuUtil.getProperty(
					props,
					prefix + GIDE.PROP_FILE );
	    setFile( i, fName.isEmpty() ? null : new File( fName ), 0 );
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
    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.weightx       = 1.0;
    gbc.insets.top    = 10;
    gbc.insets.left   = 5;
    gbc.insets.right  = 5;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    add( new JSeparator(), gbc );
    gbc.gridy++;

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
    return String.format(
		"%s%s%d.",
		this.propPrefix,
		GIDE.PROP_HARDDISK_PREFIX,
		idx + 1 );
  }


  private void doFileSelect( int idx )
  {
    if( (idx >= 0) && (idx < DISK_CNT) ) {
      File preSelection = this.fileNameFlds[ idx ].getFile();
      if( preSelection == null ) {
	preSelection = Main.getLastDirFile( Main.FILE_GROUP_DISK );
      }
      int  offset = -1;
      File file   = EmuUtil.showFileOpenDlg(
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
	  switch( OptionDlg.showOptionDlg(
		this.settingsFrm,
		"Die angegebene Datei existiert nicht.\n"
			+ "Formatieren Sie bitte das betreffende Laufwerk"
			+ " vom emulierten System aus,\n"
			+ "damit die Datei angelegt wird"
			+ " und einen Inhalt erh\u00E4lt.\n\n"
			+ "Welchen Typ soll die Datei haben?\n"
			+ "Eine RAW-Datei enth\u00E4lt das reine Abbild"
			+ " und kann mit geeigneten Programmen\n"
			+ "direkt auf eine Festplatte oder DOM"
			+ " geschrieben werden.\n"
			+ "Eine kompatible Abbilddatei entspricht dem Format"
			+ " \u00E4lterer JKCEMU-Versionen\n"
			+ "und kann auch in manch anderem Emulator verwendet"
			+ " werden.\n"
			+ "Allerdings lassen sich kompatible Abbilddateien"
			+ " nicht mit \u00FCblichen Werkzeugen\n"
			+ "direkt auf eine Festplatte oder DOM schreiben.",
		"Neue Abbilddatei",
		0,
		"RAW-Datei",
		"Kompatible Abbilddatei" ) )
	  {
	    case 0:
	      offset = 0;
	      break;
	    case 1:
	      offset = 0x100;
	      break;
	    default:
	      file = null;
	  }
	}
	if( msg != null ) {
	  BaseDlg.showErrorDlg( this, file.getPath() + ": " + msg );
	  file = null;
	}
      }
      if( file != null ) {
	setFile( idx, file, offset );
	updFieldsEnabled();
	Main.setLastFile( file, Main.FILE_GROUP_DISK );
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


  private void setFile( int idx, File file, int defaultOffset )
  {
    this.fileNameFlds[ idx ].setFile( file );
    if( file != null ) {
      if( file.exists() ) {
	int remainBytes = (int) (file.length() % 512L);
	this.offsets[ idx ] = (remainBytes >= 0x100 ? 0x100 : 0);
      } else {
	this.offsets[ idx ] = defaultOffset;
      }
    } else {
      this.offsets[ idx ] = 0;
    }
  }


  private void showDiskTypePopup( int idx )
  {
    if( this.popupMenu == null ) {
      this.popupMenu = new JPopupMenu();
      addToPopupMenu( "Festplatte ausw\u00E4hlen...", ACTION_HD_SELECT );
      this.popupMenu.addSeparator();
      this.mnuDiskNone = addToPopupMenu(
				"Festplatte nicht emulieren",
				ACTION_HD_NONE );
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
    boolean stateGeneral = (isEnabled() && this.btnEnabled.isSelected());
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
