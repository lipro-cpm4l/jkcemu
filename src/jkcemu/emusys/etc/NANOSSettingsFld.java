/*
 * (c) 2016-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die NANOS-Einstellungen
 */

package jkcemu.emusys.etc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import jkcemu.Main;
import jkcemu.base.AutoInputCharSet;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserCancelException;
import jkcemu.base.UserInputException;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.NANOS;
import jkcemu.file.FileNameFld;
import jkcemu.file.FileUtil;
import jkcemu.file.ROMFileSettingsFld;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.AutoInputSettingsFld;
import jkcemu.settings.SettingsFrm;


public class NANOSSettingsFld extends AbstractSettingsFld
{
  private JTabbedPane          tabbedPane;
  private JPanel               tabGraphic;
  private JRadioButton         rbGraphic64x32;
  private JRadioButton         rbGraphic80x24;
  private JRadioButton         rbGraphic80x25;
  private JRadioButton         rbGraphicPoppe;
  private JCheckBox            cbFixedScreenSize;
  private JPanel               tabKeyboard;
  private JRadioButton         rbKbPio00Ahs;
  private JRadioButton         rbKbPio00Abit7;
  private JRadioButton         rbKbSio84A;
  private JCheckBox            cbKbSwapCase;
  private JPanel               tabRom;
  private JRadioButton         rbRomNanos;
  private JRadioButton         rbRomEpos;
  private JRadioButton         rbRomFile;
  private FileNameFld          fldRomFile;
  private JButton              btnRomFileSelect;
  private JButton              btnRomFileRemove;
  private GIDESettingsFld      tabGIDE;
  private JPanel               tabExt;
  private JCheckBox            cbK1520Sound;
  private JCheckBox            cbKCNet;
  private JCheckBox            cbVDIP;
  private ROMFileSettingsFld   fldAltFont8x6;
  private ROMFileSettingsFld   fldAltFont8x8;
  private AutoInputSettingsFld tabAutoInput;


  public NANOSSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );

    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Grafik
    this.tabGraphic = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Grafik", this.tabGraphic );

    GridBagConstraints gbcGraphic = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.tabGraphic.add(
		GUIFactory.createLabel( "Grafikkarte:" ),
		gbcGraphic );

    ButtonGroup grpGraphic = new ButtonGroup();

    this.rbGraphic64x32 = GUIFactory.createRadioButton(
		"Bildschirmsteuerung Video 2 mit 64x32 Zeichen" );
    grpGraphic.add( this.rbGraphic64x32 );
    gbcGraphic.insets.top  = 0;
    gbcGraphic.insets.left = 50;
    gbcGraphic.gridy++;
    this.tabGraphic.add( this.rbGraphic64x32, gbcGraphic );

    this.rbGraphic80x24 = GUIFactory.createRadioButton(
		"Bildschirmsteuerung Video 3 mit 80x24 Zeichen" );
    grpGraphic.add( this.rbGraphic80x24 );
    gbcGraphic.gridy++;
    this.tabGraphic.add( this.rbGraphic80x24, gbcGraphic );

    this.rbGraphic80x25 = GUIFactory.createRadioButton(
		"Bildschirmsteuerung Video 3 mit 80x25 Zeichen" );
    grpGraphic.add( this.rbGraphic80x25 );
    gbcGraphic.gridy++;
    this.tabGraphic.add( this.rbGraphic80x25, gbcGraphic );

    this.rbGraphicPoppe = GUIFactory.createRadioButton(
		"Farbgrafikkarte mit 64x32 und 80x24 Zeichen" );
    grpGraphic.add( this.rbGraphicPoppe );
    gbcGraphic.gridy++;
    this.tabGraphic.add( this.rbGraphicPoppe, gbcGraphic );

    this.cbFixedScreenSize = GUIFactory.createCheckBox(
	"Gleiche Fenstergr\u00F6\u00DFe bei 64x32 und 80x24 Zeichen" );
    gbcGraphic.insets.left = 100;
    gbcGraphic.gridy++;
    this.tabGraphic.add( this.cbFixedScreenSize, gbcGraphic );

    gbcGraphic.fill        = GridBagConstraints.HORIZONTAL;
    gbcGraphic.weightx     = 1.0;
    gbcGraphic.insets.top  = 10;
    gbcGraphic.insets.left = 5;
    gbcGraphic.gridy++;
    this.tabGraphic.add( GUIFactory.createSeparator(), gbcGraphic );

    this.fldAltFont8x8 = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + NANOS.PROP_FONT_8X8_PREFIX,
				"Alternativer Zeichensatz (8x8):" );
    gbcGraphic.insets.top = 10;
    gbcGraphic.gridy++;
    this.tabGraphic.add( this.fldAltFont8x8, gbcGraphic );

    this.fldAltFont8x6 = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + NANOS.PROP_FONT_8X6_PREFIX,
		"Alternativer 8x6-Zeichensatz"
			+ " f\u00FCr 64x32-Modus der Farbgrafikkarte:" );
    gbcGraphic.insets.bottom = 5;
    gbcGraphic.gridy++;
    this.tabGraphic.add( this.fldAltFont8x6, gbcGraphic );


    // Tab Tastatur
    this.tabKeyboard = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Tastatur", this.tabKeyboard );

    GridBagConstraints gbcKeyboard = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.tabKeyboard.add(
		GUIFactory.createLabel( "Tastatur ist angeschlossen an:" ),
		gbcKeyboard );

    ButtonGroup grpKeyboard = new ButtonGroup();

    this.rbKbPio00Ahs = GUIFactory.createRadioButton(
		"ZRE-PIO Port A mit Ready/Strobe-Handshake (NANOS 2.2)",
		true );
    grpKeyboard.add( this.rbKbPio00Ahs );
    gbcKeyboard.insets.top  = 0;
    gbcKeyboard.insets.left = 50;
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.rbKbPio00Ahs, gbcKeyboard );

    this.rbKbPio00Abit7 = GUIFactory.createRadioButton(
		"ZRE-PIO Port A mit Strobe an Bit 7 (EPOS 2.1)" );
    grpKeyboard.add( this.rbKbPio00Abit7 );
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.rbKbPio00Abit7, gbcKeyboard );

    this.rbKbSio84A = GUIFactory.createRadioButton( "IO-Karte SIO Port A" );
    grpKeyboard.add( this.rbKbSio84A );
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.rbKbSio84A, gbcKeyboard );

    this.cbKbSwapCase = GUIFactory.createCheckBox(
		"Gro\u00DF-/Kleinschreibung umkehren" );
    gbcKeyboard.insets.top    = 10;
    gbcKeyboard.insets.left   = 5;
    gbcKeyboard.insets.bottom = 5;
    gbcKeyboard.gridy++;
    this.tabKeyboard.add( this.cbKbSwapCase, gbcKeyboard );


    // Tab ROM
    this.tabRom = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "ROM", this.tabRom );

    GridBagConstraints gbcRom = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpRom = new ButtonGroup();

    this.rbRomNanos = GUIFactory.createRadioButton(
				"Boot-ROM f\u00FCr NANOS 2.2",
				true );
    grpRom.add( this.rbRomNanos );
    this.tabRom.add( this.rbRomNanos, gbcRom );

    this.rbRomEpos = GUIFactory.createRadioButton(
				"Boot-ROM f\u00FCr EPOS 2.1" );
    grpRom.add( this.rbRomEpos );
    gbcRom.insets.top = 0;
    gbcRom.gridy++;
    this.tabRom.add( this.rbRomEpos, gbcRom );

    this.rbRomFile = GUIFactory.createRadioButton( "ROM-Datei:" );
    grpRom.add( this.rbRomFile );
    gbcRom.gridy++;
    this.tabRom.add( this.rbRomFile, gbcRom );

    this.fldRomFile      = new FileNameFld();
    gbcRom.insets.bottom = 5;
    gbcRom.insets.left   = 50;
    gbcRom.gridwidth     = 1;
    gbcRom.gridy++;
    this.tabRom.add( this.fldRomFile, gbcRom );

    this.btnRomFileSelect = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					EmuUtil.TEXT_SELECT_ROM_FILE );
    gbcRom.fill        = GridBagConstraints.NONE;
    gbcRom.weightx     = 0.0;
    gbcRom.insets.left = 0;
    gbcRom.gridx++;
    this.tabRom.add( this.btnRomFileSelect, gbcRom );

    this.btnRomFileRemove = GUIFactory.createRelImageResourceButton(
					this,
					"file/delete.png",
					EmuUtil.TEXT_REMOVE_ROM_FILE );
    gbcRom.gridx++;
    this.tabRom.add( this.btnRomFileRemove, gbcRom );


    // Tab GIDE
    this.tabGIDE = new GIDESettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "GIDE", this.tabGIDE );


    // Tab Erweiterungen
    this.tabExt = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Erweiterungen", this.tabExt );

    GridBagConstraints gbcExt = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbK1520Sound = GUIFactory.createCheckBox( "K1520-Sound-Karte" );
    this.tabExt.add( this.cbK1520Sound, gbcExt );

    this.cbKCNet = GUIFactory.createCheckBox(
				"KCNet-kompatible Netzwerkkarte" );
    gbcExt.insets.top = 0;
    gbcExt.gridy++;
    this.tabExt.add( this.cbKCNet, gbcExt );

    this.cbVDIP = GUIFactory.createCheckBox(
				"USB-Anschluss (Vinculum VDIP Modul)" );
    gbcExt.insets.bottom = 5;
    gbcExt.gridy++;
    this.tabExt.add( this.cbVDIP, gbcExt );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
				settingsFrm,
				propPrefix,
				AutoInputCharSet.getCPMCharSet(),
				NANOS.DEFAULT_SWAP_KEY_CHAR_CASE,
				NANOS.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Listener
    this.rbGraphic64x32.addActionListener( this );
    this.rbGraphic80x24.addActionListener( this );
    this.rbGraphic80x25.addActionListener( this );
    this.rbGraphicPoppe.addActionListener( this );
    this.cbFixedScreenSize.addActionListener( this );
    this.rbKbPio00Ahs.addActionListener( this );
    this.rbKbPio00Abit7.addActionListener( this );
    this.rbKbSio84A.addActionListener( this );
    this.cbKbSwapCase.addActionListener( this );
    this.rbRomNanos.addActionListener( this );
    this.rbRomEpos.addActionListener( this );
    this.rbRomFile.addActionListener( this );
    this.btnRomFileSelect.addActionListener( this );
    this.btnRomFileRemove.addActionListener( this );
    this.cbK1520Sound.addActionListener( this );
    this.cbKCNet.addActionListener( this );
    this.cbVDIP.addActionListener( this );


    // Sonstiges
    updGraphicDependFields();
    updRomDependFields();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws
					UserCancelException,
					UserInputException
  {
    Component tab  = null;
    String    text = null;
    try {

      // Tab Grafik
      tab  = this.tabGraphic;
      text = NANOS.VALUE_GRAPHIC_80X25;
      if( this.rbGraphic64x32.isSelected() ) {
	text = NANOS.VALUE_GRAPHIC_64X32;
      } else if( this.rbGraphic80x24.isSelected() ) {
	text = NANOS.VALUE_GRAPHIC_80X24;
      } else if( this.rbGraphicPoppe.isSelected() ) {
	text = NANOS.VALUE_GRAPHIC_POPPE;
      }
      EmuUtil.setProperty(
		props,
		this.propPrefix + NANOS.PROP_GRAPHIC,
		text );
      EmuUtil.setProperty(
		props,
		this.propPrefix + NANOS.PROP_FIXED_SCREEN_SIZE,
		this.cbFixedScreenSize.isSelected() );
      this.fldAltFont8x8.applyInput( props, selected );
      this.fldAltFont8x6.applyInput( props, selected );

      // Tab Tastatur
      tab  = this.tabKeyboard;
      text = NANOS.VALUE_KEYBOARD_PIO00A_HS;
      if( this.rbKbPio00Abit7.isSelected() ) {
	text = NANOS.VALUE_KEYBOARD_PIO00A_BIT7;
      } else if( this.rbKbSio84A.isSelected() ) {
	text = NANOS.VALUE_KEYBOARD_SIO84A;
      }
      EmuUtil.setProperty(
		props,
		this.propPrefix + NANOS.PROP_KEYBOARD,
		text );
      EmuUtil.setProperty(
		props,
		this.propPrefix + NANOS.PROP_KEYBOARD_SWAP_CASE,
		this.cbKbSwapCase.isSelected() );

      // Tab ROM
      tab  = this.tabRom;
      text = NANOS.VALUE_NANOS;
      if( this.rbRomEpos.isSelected() ) {
	text = NANOS.VALUE_EPOS;
      } else if( this.rbRomFile.isSelected() ) {
	File file = this.fldRomFile.getFile();
	if( file == null ) {
	  if( selected ) {
	    this.tabbedPane.setSelectedComponent( this.tabRom );
	    throw new UserInputException(
		"ROM: Bitte w\u00E4hlen Sie eine ROM-Datei aus\n"
			+ "oder stellen Sie einen anderen Boot-ROM ein." );
	  }
	}
	if( file != null ) {
	  text = NANOS.VALUE_PREFIX_FILE + file.getPath();
	}
      }
      EmuUtil.setProperty( props, this.propPrefix + NANOS.PROP_ROM, text );

      // Tab GIDE
      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

      // Tab Erweiterungen
      tab = this.tabExt;
      EmuUtil.setProperty(
		props,
		this.propPrefix + NANOS.PROP_K1520SOUND_ENABLED,
		this.cbK1520Sound.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + NANOS.PROP_KCNET_ENABLED,
		this.cbKCNet.isSelected() );
      EmuUtil.setProperty(
		props,
		this.propPrefix + NANOS.PROP_VDIP_ENABLED,
		this.cbVDIP.isSelected() );

      // Tab AutoInput
      tab = this.tabAutoInput;
      this.tabAutoInput.applyInput( props, selected );
    }
    catch( UserInputException ex ) {
      if( tab != null ) {
	this.tabbedPane.setSelectedComponent( tab );
      }
      throw ex;
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    this.settingsFrm.setWaitCursor( true );

    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      rv = this.tabGIDE.doAction( e );
      if( !rv ) {
	rv = this.tabAutoInput.doAction( e );
      }
      if( !rv ) {
	if( (src == this.rbGraphic64x32)
	    || (src == this.rbGraphic80x24)
	    || (src == this.rbGraphic80x25)
	    || (src == this.rbGraphicPoppe) )
	{
	  updGraphicDependFields();
	  fireDataChanged();
	  rv = true;
	}
	else if( (src == this.rbRomNanos)
		 || (src == this.rbRomEpos)
		 || (src == this.rbRomFile) )
	{
	  updRomDependFields();
	  fireDataChanged();
	  rv = true;
	}
	else if( src == this.btnRomFileSelect ) {
	  File file = selectFile(
				EmuUtil.TEXT_SELECT_ROM_FILE,
				Main.FILE_GROUP_ROM,
				this.fldRomFile.getFile(),
				FileUtil.getROMFileFilter() );
	  if( file != null ) {
	    if( this.fldRomFile.setFile( file ) ) {
	      this.btnRomFileRemove.setEnabled( true );
	      fireDataChanged();
	    }
	  }
	  rv = true;
	}
	else if( src == this.btnRomFileRemove ) {
	  if( this.fldRomFile.setFile( null ) ) {
	    this.btnRomFileRemove.setEnabled( false );
	    fireDataChanged();
	  }
	  rv = true;
	}
	else if( src instanceof AbstractButton ) {
	  fireDataChanged();
	  rv = true;
	}
      }
    }
    this.settingsFrm.setWaitCursor( false );
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    switch( EmuUtil.getProperty(
		props,
		this.propPrefix + NANOS.PROP_GRAPHIC ) )
    {
      case NANOS.VALUE_GRAPHIC_64X32:
	this.rbGraphic64x32.setSelected( true );
	break;
      case NANOS.VALUE_GRAPHIC_80X24:
	this.rbGraphic80x24.setSelected( true );
	break;
      case NANOS.VALUE_GRAPHIC_POPPE:
	this.rbGraphicPoppe.setSelected( true );
	break;
      default:
	this.rbGraphic80x25.setSelected( true );
    }
    this.cbFixedScreenSize.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + NANOS.PROP_FIXED_SCREEN_SIZE,
			false ) );
    this.fldAltFont8x8.updFields( props );
    this.fldAltFont8x6.updFields( props );
    switch( EmuUtil.getProperty(
		props,
		this.propPrefix + NANOS.PROP_KEYBOARD ) )
    {
      case NANOS.VALUE_KEYBOARD_PIO00A_BIT7:
	this.rbKbPio00Abit7.setSelected( true );
	break;
      case NANOS.VALUE_KEYBOARD_SIO84A:
	this.rbKbSio84A.setSelected( true );
	break;
      default:
	this.rbKbPio00Ahs.setSelected( true );
    }
    this.cbKbSwapCase.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + NANOS.PROP_KEYBOARD_SWAP_CASE,
			false ) );
    String valueText = EmuUtil.getProperty(
				props,
				this.propPrefix + NANOS.PROP_ROM );
    String lowerText = valueText.toLowerCase();
    if( (lowerText.length() > NANOS.VALUE_PREFIX_FILE.length())
	 && lowerText.startsWith( NANOS.VALUE_PREFIX_FILE ) )
    {
      this.rbRomFile.setSelected( true );
      this.fldRomFile.setFileName( valueText.substring( 5 ) );
    } else if( lowerText.equalsIgnoreCase( NANOS.VALUE_EPOS ) ) {
      this.rbRomEpos.setSelected( true );
    } else {
      this.rbRomNanos.setSelected( true );
    }
    this.cbK1520Sound.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + NANOS.PROP_K1520SOUND_ENABLED,
			false ) );
    this.cbKCNet.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + NANOS.PROP_KCNET_ENABLED,
			false ) );
    this.cbVDIP.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + NANOS.PROP_VDIP_ENABLED,
			false ) );
    this.tabGIDE.updFields( props );
    this.tabAutoInput.updFields( props );
    updGraphicDependFields();
    updRomDependFields();
  }


  private void updGraphicDependFields()
  {
    boolean state = this.rbGraphicPoppe.isSelected();
    this.cbFixedScreenSize.setEnabled( state );
    this.fldAltFont8x6.setEnabled( state );
  }


  private void updRomDependFields()
  {
    boolean stateFile = this.rbRomFile.isSelected();
    this.fldRomFile.setEnabled( stateFile );
    this.btnRomFileSelect.setEnabled( stateFile );
    this.btnRomFileRemove.setEnabled(
		stateFile && (this.fldRomFile.getFile() != null) );
  }
}
