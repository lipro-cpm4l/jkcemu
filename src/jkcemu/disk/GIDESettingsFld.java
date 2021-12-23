/*
 * (c) 2010-2021 Jens Mueller
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
import java.util.Arrays;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.OptionDlg;
import jkcemu.base.PopupMenuOwner;
import jkcemu.base.UserInputException;
import jkcemu.file.FileNameFld;
import jkcemu.file.FileUtil;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.SettingsFrm;


public class GIDESettingsFld
			extends AbstractSettingsFld
			implements PopupMenuOwner
{
  private static final String ACTION_HD_NONE
				= GIDE.PROP_HARDDISK_PREFIX + "none";
  private static final String ACTION_HD_SELECT
				= GIDE.PROP_HARDDISK_PREFIX + "select";

  private static final int DISK_CNT = 2;

  private int               appliedIOBaseAddr;
  private int               defaultIOBaseAddr;
  private int               popupDiskIdx;
  private JPopupMenu        popupMenu;
  private JMenuItem         mnuDiskNone;
  private JCheckBox         cbEnabled;
  private JComboBox<String> comboIOBaseAddr;
  private HardDiskInfo[]    diskTypes;
  private JLabel[]          titleLabels;
  private JTextField[]      diskTypeFlds;
  private JButton[]         diskTypeBtns;
  private FileNameFld[]     fileNameFlds;
  private JButton[]         selectBtns;
  private int[]             offsets;


  public GIDESettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    this( settingsFrm, propPrefix, -1 );
  }


  public GIDESettingsFld(
			SettingsFrm settingsFrm,
			String      propPrefix,
			int         defaultIOBaseAddr,
			int...      ioBaseAddrs )
  {
    super( settingsFrm, propPrefix );
    this.defaultIOBaseAddr = defaultIOBaseAddr;
    this.appliedIOBaseAddr = -1;
    this.popupDiskIdx      = -1;
    this.popupMenu         = null;
    this.mnuDiskNone       = null;
    this.diskTypes         = new HardDiskInfo[ DISK_CNT ];
    this.titleLabels       = new JLabel[ DISK_CNT ];
    this.diskTypeFlds      = new JTextField[ DISK_CNT ];
    this.diskTypeBtns      = new JButton[ DISK_CNT ];
    this.fileNameFlds      = new FileNameFld[ DISK_CNT ];
    this.selectBtns        = new JButton[ DISK_CNT ];
    this.offsets           = new int[ DISK_CNT ];
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

    int nIOBaseAddrs = (ioBaseAddrs != null ? ioBaseAddrs.length : 0);
    if( nIOBaseAddrs > 1 ) {
      this.cbEnabled = GUIFactory.createCheckBox(
			"GIDE emulieren an E/A-Basisadresse:" );
      this.comboIOBaseAddr = GUIFactory.createComboBox();
      this.comboIOBaseAddr.setEditable( false );
      for( int i = 0; i < nIOBaseAddrs; i++ ) {
	this.comboIOBaseAddr.addItem(
		String.format( "%02Xh", ioBaseAddrs[ i ] ) );
      }
      JPanel panelGeneral = GUIFactory.createPanel( new GridBagLayout() );
      GridBagConstraints gbcGeneral = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 0, 0, 0, 0 ),
					0, 0 );
      panelGeneral.add( this.cbEnabled, gbcGeneral );
      gbcGeneral.insets.left = 5;
      gbcGeneral.gridx++;
      panelGeneral.add( this.comboIOBaseAddr, gbcGeneral );
      add( panelGeneral, gbc );
    } else {
      this.cbEnabled       = GUIFactory.createCheckBox( "GIDE emulieren" );
      this.comboIOBaseAddr = null;
      add( this.cbEnabled, gbc );
    }
    gbc.gridy++;
    for( int i = 0; i < DISK_CNT; i++ ) {
      addDiskFlds( i, gbc );
      gbc.gridy++;
    }
    this.cbEnabled.addActionListener( this );
    if( this.comboIOBaseAddr != null ) {
      this.comboIOBaseAddr.addActionListener( this );
    }
  }


  /*
   * Rueckgabewert:
   *   IO-Basisadresse oder -1, falls keine Auswahl angeboten wurde
   */
  public int getAppliedIOBaseAddr()
  {
    return this.appliedIOBaseAddr;
  }


  public void setEnabledEx( boolean stateAll, boolean switchable )
  {
    super.setEnabled( stateAll );
    this.cbEnabled.setEnabled( stateAll && switchable );
    updFieldsEnabled();
  }


	/* --- PopupMenuOwner --- */

  @Override
  public JPopupMenu getPopupMenu()
  {
    return this.popupMenu;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    int ioBaseAddr = -1;
    try {
      if( props != null ) {
        boolean enabled = this.cbEnabled.isSelected();
	EmuUtil.setProperty(
			props,
			this.propPrefix + GIDE.PROP_ENABLED,
			enabled );
	if( this.comboIOBaseAddr != null ) {
	  Object o = this.comboIOBaseAddr.getSelectedItem();
	  if( o != null ) {
	    String s = o.toString();
	    if( s != null ) {
	      s = s.trim().toUpperCase();
	      if( s.endsWith( "H" ) ) {
		s = s.substring( 0, s.length() - 1 );
	      }
	      try {
		int tmpAddr = Integer.parseInt( s, 16 );
		EmuUtil.setProperty(
			props,
			this.propPrefix + GIDE.PROP_IOBASEADDR,
			tmpAddr );
		if( selected && enabled ) {
		  ioBaseAddr = tmpAddr;
		}
	      }
	      catch( NumberFormatException ex ) {
		if( selected && enabled ) {
		  throw new UserInputException(
			"E/A-Basisadresse: Ung\u00FCltige Eingabe" );
		}
	      }
	    }
	  }
	}
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
    finally {
      this.appliedIOBaseAddr = ioBaseAddr;
    }
  }


  @Override
  public boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.cbEnabled ) {
	rv = true;
	updFieldsEnabled();
	fireDataChanged();
      } else if( src == this.comboIOBaseAddr ) {
	rv = true;
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
  public void fileDropped( Component c, File file )
  {
    if( (c != null) && (file != null) ) {
      for( int i = 0; i < DISK_CNT; i++ ) {
	if( c == this.fileNameFlds[ i ] ) {
	  if( this.fileNameFlds[ i ].isEnabled() ) {
	    setFile( i, file, 0 );
	    updFieldsEnabled();
	    break;
	  }
	}
      }
    }
  }


  @Override
  public void setEnabled( boolean state )
  {
    super.setEnabled( state );
    this.cbEnabled.setEnabled( state );
    updFieldsEnabled();
  }


  @Override
  public void updFields( Properties props )
  {
    this.cbEnabled.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + GIDE.PROP_ENABLED,
				false ) );
    if( this.comboIOBaseAddr != null ) {
      this.comboIOBaseAddr.setSelectedItem(
		String.format(
			"%02Xh",
			EmuUtil.getIntProperty(
				props,
				this.propPrefix + GIDE.PROP_IOBASEADDR,
				this.defaultIOBaseAddr ) ) );
    }
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
    add( GUIFactory.createSeparator(), gbc );
    gbc.gridy++;

    // 1. Zeile
    if( idx == 0 ) {
      this.titleLabels[ idx ] = GUIFactory.createLabel(
					"1. Festplatte (Master):" );
    } else {
      this.titleLabels[ idx ] = GUIFactory.createLabel(
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

    this.diskTypeFlds[ idx ] = GUIFactory.createTextField();
    this.diskTypeFlds[ idx ].setEditable( false );
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.insets.left = 0;
    gbc.gridx++;
    add( this.diskTypeFlds[ idx ], gbc );

    this.diskTypeBtns[ idx ] = GUIFactory.createRelImageResourceButton(
				this,
				"disk/harddiskmodel.png",
				"Festplattenmodell ausw\u00E4hlen" );
    this.diskTypeBtns[ idx ].addActionListener( this );
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( this.diskTypeBtns[ idx ], gbc );

    // 2.Zeile
    this.fileNameFlds[ idx ] = new FileNameFld();
    gbc.fill                 = GridBagConstraints.HORIZONTAL;
    gbc.weightx              = 1.0;
    gbc.insets.left          = 50;
    gbc.gridwidth            = 2;
    gbc.gridx                = 0;
    gbc.gridy++;
    add( this.fileNameFlds[ idx ], gbc );

    this.selectBtns[ idx ] = GUIFactory.createRelImageResourceButton(
				this,
				"file/open.png",
				"Festplattenabbilddatei ausw\u00E4hlen" );
    this.selectBtns[ idx ].addActionListener( this );
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
    JMenuItem item = GUIFactory.createMenuItem( text );
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
      File file   = FileUtil.showFileOpenDlg(
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
      this.popupMenu = GUIFactory.createPopupMenu();
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
    boolean stateGeneral = isEnabled();
    if( stateGeneral && this.cbEnabled.isEnabled() ) {
      stateGeneral = this.cbEnabled.isSelected();
    }
    if( this.comboIOBaseAddr != null ) {
      this.comboIOBaseAddr.setEnabled( stateGeneral );
    }
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
