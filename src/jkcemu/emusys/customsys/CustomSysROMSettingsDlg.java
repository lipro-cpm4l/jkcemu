/*
 * (c) 2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer die Einstellungen eines ROM-Bereichs
 */

package jkcemu.emusys.customsys;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.Window;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileNameFld;
import jkcemu.base.HexDocument;
import jkcemu.base.UserInputException;


public class CustomSysROMSettingsDlg
			extends BaseDlg
			implements DropTargetListener
{
  private static final String LABEL_BEG_ADDR = "Anfangsadresse (hex):";
  private static final String LABEL_SIZE     = "Gr\u00F6\u00DFe (hex):";
  private static final String LABEL_IO_ADDR  = "E/A-Adresse (hex):";

  private Window            owner;
  private CustomSysROM      approvedROM;
  private HexDocument       docBegAddr;
  private HexDocument       docSize;
  private HexDocument       docSwitchIOAddr;
  private FileNameFld       fldFile;
  private JTextField        fldBegAddr;
  private JTextField        fldSize;
  private JTextField        fldSwitchIOAddr;
  private JLabel            labelSwitchIOAddr;
  private JLabel            labelSwitchIOBit;
  private JComboBox<String> comboSwitchIOBit;
  private JCheckBox         btnEnableOnReset;
  private JCheckBox         btnBoot;
  private JRadioButton      btnAlwaysSwitchedOn;
  private JRadioButton      btnSwitchoffable;
  private JRadioButton      btnSwitchableByBit;
  private JButton           btnSelect;
  private JButton           btnOK;
  private JButton           btnCancel;


  public static CustomSysROM showDlg(
				Window       owner,
				CustomSysROM rom )
  {
    CustomSysROMSettingsDlg dlg = new CustomSysROMSettingsDlg(
							owner,
							rom,
							rom.isBootROM() );
    dlg.setVisible( true );
    return dlg.approvedROM;
  }


  public static CustomSysROM showNewROMDlg(
				Window  owner,
				boolean presetBootROM )
  {
    CustomSysROMSettingsDlg dlg = new CustomSysROMSettingsDlg(
							owner,
							null,
							presetBootROM );
    dlg.setVisible( true );
    return dlg.approvedROM;
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
    boolean           done    = false;
    DropTargetContext context = e.getDropTargetContext();
    if( context != null ) {
      if( context.getComponent() == this.fldFile ) {
	File file = EmuUtil.fileDrop( this, e );
	if( file != null ) {
	  setFile( file );
	  done = true;
	}
      }
    }
    if( done ) {
      e.dropComplete( true );
    } else {
      e.rejectDrop();
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.fldBegAddr ) {
      rv = true;
      this.fldSize.requestFocus();
    }
    else if( src == this.fldSize ) {
      rv = true;
      this.btnSelect.requestFocus();
    }
    if( src == this.btnSelect ) {
      rv = true;
      doFileSelect();
    }
    else if( (src == this.btnAlwaysSwitchedOn)
	     || (src == this.btnSwitchoffable)
	     || (src == this.btnSwitchableByBit) )
    {
      rv = true;
      updSwitchableFieldsEnabled();
    }
    else if( src == this.fldSwitchIOAddr ) {
      rv = true;
      if( this.comboSwitchIOBit.isEnabled() ) {
	this.comboSwitchIOBit.requestFocus();
      } else {
	this.btnBoot.requestFocus();
      }
    }
    else if( src == this.btnOK ) {
      rv = true;
      doApply();
    }
    else if( src == this.btnCancel ) {
      rv = true;
      doClose();
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void doApply()
  {
    try {
      updRomSizeIfMissing();

      int switchIOAddr = -1;
      int begAddr      = this.docBegAddr.intValue();
      int romSize      = this.docSize.intValue();
      if( romSize < 1 ) {
	throw new UserInputException( "Ung\u00FCltige ROM-Gr\u00F6\u00DFe" );
      }
      if( (begAddr + romSize) > 0x10000 ) {
	throw new UserInputException(
		String.format(
			"Der ROM-Bereich ragt \u00FCber die Adresse"
				+ " FFFFh hinaus.\n"
				+ "Bei der Anfangsadresse %04Xh kann der"
				+ " ROM-Bereich maximal %04Xh Bytes"
				+ " gro\u00DF sein.",
			begAddr,
			0x10000 - begAddr ) );
      }
      boolean status = true;
      File    file   = this.fldFile.getFile();
      if( file != null ) {
	long fileSize = file.length();
	if( fileSize > romSize ) {
	  if( !showConfirmDlg(
		this,
		"Die Datei ist gr\u00F6\u00DFer als der ROM-Bereich.\n"
			+ "Es sind somit nicht alle Bytes der Datei"
			+ " im Arbeitsspeicher sichtbar." ) )
	  {
	    status = false;
	  }
	}
      } else {
	if( !showConfirmDlg(
		this,
		"Sie haben keine Datei ausgew\u00E4hlt.\n"
			+ "Der ROM-Bereich enth\u00E4lt somit nur"
			+ " FFh-Bytes." ) )
	{
	  status = false;
	}
      }
      if( status ) {
	boolean switchableByBit = this.btnSwitchableByBit.isSelected();
	boolean enableOnReset   = this.btnEnableOnReset.isSelected();
	boolean bootROM         = this.btnBoot.isSelected();
	if( this.btnSwitchoffable.isSelected() || switchableByBit ) {
	  Integer tmpIOAddr = this.docSwitchIOAddr.getInteger();
	  if( tmpIOAddr != null ) {
	    switchIOAddr = tmpIOAddr;
	  }
	  if( switchIOAddr < 0 ) {
	    throw new UserInputException(
			"Wenn der ROM ein- oder ausblendbar ist, muss auch\n"
				+ "eine E/A-Adresse angegeben werden,"
				+ " \u00FCBer die das geschieht." );
	  }
	}
	if( switchableByBit && !enableOnReset && bootROM ) {
	  throw new UserInputException(
			"Wenn der ROM ein schaltbarer Boot-ROM ist,\n"
				+ "muss er nach RESET eingeblendet sein." );
	}
	int switchIOMask  = 0;
	int switchIOValue = 0;
	if( switchableByBit ) {
	  int idx      = this.comboSwitchIOBit.getSelectedIndex();
	  switchIOMask = (0x01 << (idx % 8));
	  if( idx < 8 ) {
	    switchIOValue = switchIOMask;
	  }
	}
	this.approvedROM = new CustomSysROM(
				begAddr,
				romSize,
				file != null ? file.getPath() : null,
				switchIOAddr,
				switchIOMask,
				switchIOValue,
				enableOnReset,
				bootROM );
	doClose();
      }
    }
    catch( NumberFormatException | UserInputException ex ) {
      showErrorDlg( this, ex );
    }
  }


  private void doFileSelect()
  {
    File file = this.fldFile.getFile();
    if( file == null ) {
      file = Main.getLastDirFile( Main.FILE_GROUP_ROM );
    }
    file = EmuUtil.showFileOpenDlg(
			this.owner,
			"ROM-Datei ausw\u00E4hlen",
			file,
			EmuUtil.getROMFileFilter() );
    if( file != null ) {
      setFile( file );
      Main.setLastFile( file, Main.FILE_GROUP_ROM );
    }
  }


  private void setFile( File file )
  {
    this.fldFile.setFile( file );
    updRomSizeIfMissing();
  }


  private void updRomSizeIfMissing()
  {
    File file = this.fldFile.getFile();
    if( (file != null) && (this.docSize.getLength() == 0) ) {
      long fileLen = file.length();
      if( (fileLen > 0) && (fileLen < 0x10000) ) {
	this.docSize.setValue( (int) fileLen, 4 );
      }
    }
  }


  private void updSwitchableFieldsEnabled()
  {
    boolean switchableByBit = this.btnSwitchableByBit.isSelected();
    boolean switchable      = this.btnSwitchoffable.isSelected()
					|| switchableByBit;
    this.labelSwitchIOAddr.setEnabled( switchable );
    this.fldSwitchIOAddr.setEnabled( switchable );
    this.labelSwitchIOBit.setEnabled( switchableByBit );
    this.comboSwitchIOBit.setEnabled( switchableByBit );
    this.btnEnableOnReset.setEnabled( switchableByBit );
  }


	/* --- Konstruktor --- */

  private CustomSysROMSettingsDlg(
			Window       owner,
			CustomSysROM rom,
			boolean      presetBootROM )
  {
    super(
	owner,
	rom != null ? "ROM-Bereich bearbeiten" : "Neuer ROM-Bereich" );
    this.owner       = owner;
    this.approvedROM = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    // Adressbereich
    JPanel panelAddr = new JPanel( new GridBagLayout() );
    add( panelAddr, gbc );

    GridBagConstraints gbcAddr = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 0, 0, 0, 0 ),
						0, 0 );

    panelAddr.add( new JLabel( LABEL_BEG_ADDR ), gbcAddr );

    this.docBegAddr = new HexDocument( 4, LABEL_BEG_ADDR );
    this.fldBegAddr = new JTextField( this.docBegAddr, "", 5 );
    gbcAddr.insets.left = 5;
    gbcAddr.gridx++;
    panelAddr.add( this.fldBegAddr, gbcAddr );

    gbcAddr.gridx++;
    panelAddr.add( new JLabel( LABEL_SIZE ), gbcAddr );

    this.docSize = new HexDocument( 4, LABEL_SIZE );
    this.fldSize = new JTextField( this.docSize, "", 5 );
    gbcAddr.gridx++;
    panelAddr.add( this.fldSize, gbcAddr );

    // Datei
    gbc.insets.top = 10;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( new JLabel( "ROM-Inhalt:" ), gbc );

    this.fldFile   = new FileNameFld();
    gbc.insets.top = 0;
    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.weightx    = 1.0;
    gbc.gridwidth  = 2;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( this.fldFile, gbc );

    this.btnSelect = createImageButton(
				"/images/file/open.png",
				"ROM-Datei ausw\u00E4hlen" );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.left = 0;
    gbc.gridx += 2;
    add( this.btnSelect, gbc );

    // Schaltmodi
    ButtonGroup grpSwitch = new ButtonGroup();

    this.btnAlwaysSwitchedOn = new JRadioButton(
	"ROM ist nicht schaltbar, d.h. immer eingeblendet",
	true );
    grpSwitch.add( this.btnAlwaysSwitchedOn );
    gbc.insets.top = 10;
    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( this.btnAlwaysSwitchedOn, gbc );

    this.btnSwitchoffable = new JRadioButton(
	"ROM ist mit einem Ausgabebefehl ausblendbar" );
    grpSwitch.add( this.btnSwitchoffable );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.btnSwitchoffable, gbc );

    this.btnSwitchableByBit = new JRadioButton(
	"ROM ist mit einem Ausgabebefehl \u00FCber ein Bit"
		+ " ein- und ausblendbar" );
    grpSwitch.add( this.btnSwitchableByBit );
    gbc.gridy++;
    add( this.btnSwitchableByBit, gbc );

    this.labelSwitchIOAddr = new JLabel( LABEL_IO_ADDR );
    gbc.insets.top         = 5;
    gbc.insets.left        = 50;
    gbc.gridwidth          = 1;
    gbc.gridx              = 0;
    gbc.gridy++;
    add( this.labelSwitchIOAddr, gbc );

    this.docSwitchIOAddr = new HexDocument( 2, LABEL_IO_ADDR );
    this.fldSwitchIOAddr = new JTextField(
					this.docSwitchIOAddr,
					"",
					2 );
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.fldSwitchIOAddr, gbc );

    this.labelSwitchIOBit = new JLabel( "ROM wird eingeblendet bei:" );
    gbc.insets.left       = 50;
    gbc.gridx             = 0;
    gbc.gridy++;
    add( this.labelSwitchIOBit, gbc );

    this.comboSwitchIOBit = new JComboBox<>();
    this.comboSwitchIOBit.setEditable( false );
    for( int i = 0; i < 8; i++ ) {
      this.comboSwitchIOBit.addItem( String.format( "Bit %d = H", i ) );
    }
    for( int i = 0; i < 8; i++ ) {
      this.comboSwitchIOBit.addItem( String.format( "Bit %d = L", i ) );
    }
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboSwitchIOBit, gbc );

    this.btnEnableOnReset = new JCheckBox(
				"ROM ist nach RESET eingeblendet" );
    gbc.insets.left = 50;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.btnEnableOnReset, gbc );

    // Boot-ROM
    this.btnBoot = new JCheckBox(
	"Nach RESET beginnt bei dem ROM die Programmausf\u00FChrung"
		+ " (Boot-ROM)" );
    gbc.insets.top  = 10;
    gbc.insets.left = 5;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.btnBoot, gbc );


    // Knoepfe
    JPanel panelBtn   = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.insets.left   = 5;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );


    // Vorbelegungen
    if( rom != null ) {
      this.docBegAddr.setValue( rom.getBegAddr(), 4 );
      this.docSize.setValue( rom.getSize(), 4 );
      this.fldFile.setFileName( rom.getFileName() );
      int switchIOAddr  = rom.getSwitchIOAddr();
      int switchIOMask  = rom.getSwitchIOMask() & 0xFF;
      int switchIOValue = rom.getSwitchIOValue() & switchIOMask;
      if( switchIOAddr >= 0 ) {
	this.docSwitchIOAddr.setValue( switchIOAddr, 2 );
	if( switchIOMask != 0 ) {
	  int idx = 0;
	  while( idx < 8 ) {
	    if( (switchIOMask & 0x01) != 0 ) {
	      break;
	    }
	    switchIOMask >>= 1;
	    idx++;
	  }
	  if( switchIOValue == 0 ) {
	    idx += 8;			// L-aktiv
	  }
	  try {
	    this.comboSwitchIOBit.setSelectedIndex( idx );
	  }
	  catch( IllegalArgumentException ex ) {}
	  this.btnSwitchableByBit.setSelected( true );
	} else {
	  this.btnSwitchoffable.setSelected( true );
	}
      } else {
	this.btnAlwaysSwitchedOn.setSelected( true );
      }
      this.btnEnableOnReset.setSelected( rom.getEnableOnReset() );
      this.btnBoot.setSelected( rom.isBootROM() );
    } else {
      this.btnEnableOnReset.setSelected( presetBootROM );
      this.btnBoot.setSelected( presetBootROM );
    }
    updSwitchableFieldsEnabled();


    // Fenstergroesse
    pack();
    setParentCentered();


    // Listener
    this.fldBegAddr.addActionListener( this );
    this.fldSize.addActionListener( this );
    this.btnSelect.addKeyListener( this );
    this.btnAlwaysSwitchedOn.addActionListener( this );
    this.btnSwitchoffable.addActionListener( this );
    this.btnSwitchableByBit.addActionListener( this );
    this.fldSwitchIOAddr.addActionListener( this );
    this.comboSwitchIOBit.addActionListener( this );
    this.btnOK.addActionListener( this );
    this.btnOK.addKeyListener( this );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );


    // Drag&Drop ermoeglichen
    (new DropTarget( this.fldFile, this )).setActive( true );
  }
}
