/*
 * (c) 2017-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen des benutzerdefinierten Computers
 */

package jkcemu.emusys.customsys;

import java.awt.Component;
import java.awt.BorderLayout;
import java.util.EventObject;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import jkcemu.base.AutoInputCharSet;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HexDocument;
import jkcemu.base.UserCancelException;
import jkcemu.base.UserInputException;
import jkcemu.disk.GIDESettingsFld;
import jkcemu.emusys.CustomSys;
import jkcemu.file.ROMFileSettingsFld;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.AutoInputSettingsFld;
import jkcemu.settings.AutoLoadSettingsFld;
import jkcemu.settings.SettingsFrm;


public class CustomSysSettingsFld extends AbstractSettingsFld
{
  protected static final String TEXT_INVALID_VALUE = " Ung\u00FCltiger Wert";
  protected static final String TEXT_INVALID_ADDR = " Ung\u00FCltige Adresse";

  private Map<Integer,String>  ioAddrMap;
  private JTabbedPane          tabbedPane;
  private FDCSettingsFld       tabFDC;
  private GeneralSettingslFld  tabGeneral;
  private IOSettingsFld        tabIO;
  private KeyboardSettingsFld  tabKeyboard;
  private ROMSettingsFld       tabROM;
  private ScreenSettingsFld    tabScreen;
  private GIDESettingsFld      tabGIDE;
  private AutoLoadSettingsFld  tabAutoLoad;
  private AutoInputSettingsFld tabAutoInput;


  public CustomSysSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );
    this.ioAddrMap = new HashMap<>();

    setLayout( new BorderLayout() );

    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, BorderLayout.CENTER );

    this.tabGeneral = new GeneralSettingslFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "Allgemein", this.tabGeneral );

    this.tabROM = new ROMSettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "ROM", this.tabROM );

    this.tabScreen = new ScreenSettingsFld( settingsFrm, propPrefix );
    this.tabbedPane.addTab( "Bildschirmausgabe", this.tabScreen );

    this.tabKeyboard = new KeyboardSettingsFld( this, propPrefix );
    this.tabbedPane.addTab( "Tastatur", this.tabKeyboard );

    this.tabIO = new IOSettingsFld( this, propPrefix );
    this.tabbedPane.addTab( "E/A-Bausteine", this.tabIO );

    this.tabFDC = new FDCSettingsFld( this, propPrefix );
    this.tabbedPane.addTab( "FDC", this.tabFDC );

    this.tabGIDE = new GIDESettingsFld(
				settingsFrm,
				propPrefix,
				CustomSys.DEFAULT_GIDE_IOBASEADDR,
				0x00, 0x10, 0x20, 0x30,
				0x40, 0x50, 0x60, 0x70,
				0x80, 0x90, 0xA0, 0xB0,
				0xC0, 0xD0, 0xE0, 0xF0 );
    this.tabbedPane.addTab( "GIDE", this.tabGIDE );

    this.tabAutoLoad = new AutoLoadSettingsFld(
			settingsFrm,
			propPrefix,
			CustomSys.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX,
			true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );

    this.tabAutoInput = new AutoInputSettingsFld(
			settingsFrm,
			propPrefix,
			AutoInputCharSet.getStdCharSet(),
			CustomSys.DEFAULT_SWAP_KEY_CHAR_CASE,
			CustomSys.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );
  }


  public void addToIOAddrMap(
			int    ioAddr,
			int    ioAddrCount,
			String text ) throws UserInputException
  {
    if( ioAddr >= 0 ) {
      for( int i = 0; i < ioAddrCount; i++ ) {
	String oldText = this.ioAddrMap.get( ioAddr );
	if( oldText != null ) {
	  throw new UserInputException(
		String.format(
			"E/A-Adresskonflikt bei Adresse %02Xh:\n"
				+ "%s <> %s\n\n"
				+ "Den E/A-Komponenten m\u00FCssen"
				+ " unterschiedliche\n"
				+ "Adressen bzw. Adressbereiche zugewiesen"
				+ " werden.\n"
				+ "PIO, SIO, CTC, KCNet und USB verwenden"
				+ " jeweils 4,\n"
				+ "K1520-Sound 8 und GIDE 16 aufeinanderfolgende\n"
				+ "Adressen.",
			ioAddr,
			oldText,
			text ) );
	}
	this.ioAddrMap.put( ioAddr++, text );
      }
    }
  }


  public int applyIOAddrInput(
			boolean     selected,
			String      labelText,
			HexDocument docIOAddr,
			String      propIOAddr,
			Properties  props ) throws UserInputException
  {
    int rv     = -1;
    int ioAddr = -1;
    try {
      ioAddr = docIOAddr.intValue();
      if( selected ) {
	if( ioAddr > 0xFF ) {
	  throw new UserInputException( labelText + TEXT_INVALID_ADDR );
	}
	rv = ioAddr;
      }
    }
    catch( NumberFormatException ex ) {
      if( selected ) {
	throw new UserInputException( labelText + TEXT_INVALID_ADDR );
      }
    }
    if( ioAddr >= 0 ) {
      EmuUtil.setProperty(
		props,
		this.propPrefix + propIOAddr,
		ioAddr );
    }
    return rv;
  }


  public void applyIOAddrInput(
			boolean       selected,
			String        text,
			JToggleButton tglEnabled,
			String        propEnabled,
			HexDocument   docIOAddr,
			int           ioAddrCount,
			String        propIOAddr,
			Properties    props ) throws UserInputException
  {
    boolean enabled = true;
    if( (tglEnabled != null) && (propEnabled != null) ) {
      enabled = tglEnabled.isSelected();
      EmuUtil.setProperty( props, this.propPrefix + propEnabled, enabled );
    }
    addToIOAddrMap(
		applyIOAddrInput(
			selected && enabled,
			tglEnabled.getText(),
			docIOAddr,
			propIOAddr,
			props ),
		ioAddrCount,
		text );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws
					UserCancelException,
					UserInputException
  {
    this.ioAddrMap.clear();
    Component tab = null;
    try {
      tab = this.tabGeneral;
      this.tabGeneral.applyInput( props, selected );

      tab = this.tabROM;
      this.tabROM.applyInput( props, selected );

      tab = this.tabScreen;
      this.tabScreen.applyInput( props, selected );

      tab = this.tabKeyboard;
      this.tabKeyboard.applyInput( props, selected );

      tab = this.tabIO;
      this.tabIO.applyInput( props, selected );

      tab = this.tabFDC;
      this.tabFDC.applyInput( props, selected );

      tab = this.tabGIDE;
      this.tabGIDE.applyInput( props, selected );

      tab = this.tabAutoLoad;
      this.tabAutoLoad.applyInput( props, selected );

      tab = this.tabAutoInput;
      this.tabAutoInput.applyInput( props, selected );

      // Abhaengigkeiten und IO-Adresskonflikte pruefen
      if( selected ) {
	tab = this.tabIO;
	if( this.tabKeyboard.isKeyboardConnectedToPIO()
	    && this.tabIO.isPIOEnabled() )
	{
	  throw new UserInputException(
		"Wenn die Tastatur an einer PIO angeschlossen ist,\n"
			+ "m\u00FCssen Sie auch die Emulation der PIO"
			+ " aktivieren." );
	}
	if( this.tabKeyboard.isKeyboardConnectedToSIO()
	    && this.tabIO.isSIOEnabled() )
	{
	  throw new UserInputException(
		"Wenn die Tastatur an einer SIO angeschlossen ist,\n"
			+ "m\u00FCssen Sie auch die Emulation der SIO"
			+ " aktivieren." );
	}
	tab = this.tabGIDE;
	int gideIOBaseAddr = this.tabGIDE.getAppliedIOBaseAddr();
	if( gideIOBaseAddr >= 0 ) {
	  addToIOAddrMap( gideIOBaseAddr, 16, "GIDE" );
	}
      }
    }
    catch( UserCancelException | UserInputException ex ) {
      if( tab != null ) {
	this.tabbedPane.setSelectedComponent( tab );
      }
      throw ex;
    }
  }


  @Override
  public void updFields( Properties props )
  {
    this.tabGeneral.updFields( props );
    this.tabROM.updFields( props );
    this.tabScreen.updFields( props );
    this.tabKeyboard.updFields( props );
    this.tabIO.updFields( props );
    this.tabFDC.updFields( props );
    this.tabGIDE.updFields( props );
    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );
  }
}
