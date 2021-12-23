/*
 * (c) 2020-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Einstellungen zur Skalierung der Fenster
 */

package jkcemu.settings;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import jkcemu.Main;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserInputException;


public class UIScaleSettingsFld extends AbstractSettingsFld
{
  private static final String DEFAULT_SCALE_ITEM = "100 %";

  private static final String[] SCALE_ITEMS_WINDOWS = {
		"75 %", "100 %", "125 %", "150 %", "175 %", "200 %" };

  private static final String[] SCALE_ITEMS_OTHER = { "100 %", "200 %" };

  private JRadioButton      rbScaleNone;
  private JRadioButton      rbScaleDefault;
  private JRadioButton      rbScaleFix;
  private JComboBox<String> comboScale;


  public UIScaleSettingsFld( SettingsFrm settingsFrm )
  {
    super( settingsFrm );
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    add(
	GUIFactory.createLabel( "Ab Java 9 k\u00F6nnen die JKCEMU-Fenster"
		+ " und Fensterinhalte skaliert werden:" ),
	gbc );

    ButtonGroup grpScale = new ButtonGroup();

    this.rbScaleNone = GUIFactory.createRadioButton( "Keine Skalierung" );
    grpScale.add( this.rbScaleNone );
    gbc.insets.left = 50;
    gbc.gridy++;
    add( this.rbScaleNone, gbc );

    this.rbScaleDefault = GUIFactory.createRadioButton(
		"Skalierung entsprechend Java-Standard"
			+ " (automatische Skalierung bei"
			+ " hochaufl\u00F6senden Bildschirmen)" );
    grpScale.add( this.rbScaleDefault );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.rbScaleDefault, gbc );

    this.rbScaleFix = GUIFactory.createRadioButton(
				"Fest eingestellte Skalierung:" );
    grpScale.add( this.rbScaleFix );
    gbc.gridwidth = 1;
    gbc.gridy++;
    add( this.rbScaleFix, gbc );

    this.comboScale = GUIFactory.createComboBox(
			Main.getOS() == Main.OS.WINDOWS ?
						SCALE_ITEMS_WINDOWS
						: SCALE_ITEMS_OTHER );
    this.comboScale.setEditable( true );
    gbc.insets.top  = 0;
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboScale, gbc );

    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.insets.top = 20;
    gbc.gridx      = 0;
    if( Main.getOS() != Main.OS.WINDOWS ) {
      gbc.gridy++;
      add(
	GUIFactory.createLabel( "Achtung! Auf diesem Betriebssystem hier"
		+ " funktionieren m\u00F6glicherweise nur Skalierungen"
		+ " in 100%-Schritten." ),
	gbc );
    }
    gbc.gridy++;
    add(
	GUIFactory.createLabel( "Achtung! Die Einstellungen auf dieser"
		+ " Unterseite werden nur wirksam, wenn Sie sie in einem"
		+ " Profil speichern und" ),
	gbc );
    gbc.insets.top = 0;
    gbc.gridy++;
    add(
	GUIFactory.createLabel( "anschlie\u00DFend JKCEMU mit diesem Profil"
		+ " neu starten und dabei Java 9 oder eine h\u00F6here"
		+ " Java-Version verwenden." ),
	gbc );

    gbc.insets.top = 20;
    gbc.gridy++;
    add(
	GUIFactory.createLabel( "Achtung! Bei einer Skalierung funktioniert"
		+ " m\u00F6glicherweise der Vollbildmodus"
		+ " nicht mehr korrekt." ),
	gbc );


    // Listener
    this.rbScaleNone.addActionListener( this );
    this.rbScaleDefault.addActionListener( this );
    this.rbScaleFix.addActionListener( this );
    this.comboScale.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( (src == this.rbScaleNone)
	|| (src == this.rbScaleDefault)
	|| (src == this.rbScaleFix) )
    {
      updComboScaleEnabled();
      fireDataChanged();
    }
    else if( src == this.comboScale ) {
      parseComboScale();
      fireDataChanged();
    }
  }


  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    String value = "default";
    if( this.rbScaleNone.isSelected() ) {
      value = Main.VALUE_UI_SCALE_NONE;
    } else if( this.rbScaleFix.isSelected() ) {
      value = parseComboScale();
      if( value == null ) {
	throw new UserInputException(
		"Skalierungsfaktor in Prozent: Ung\u00FCltiger Wert" );
      }
    }
    EmuUtil.setProperty( props, Main.PROP_UI_SCALE, value );
  }


  @Override
  public void updFields( Properties props )
  {
    boolean done = false;
    String  item = null;
    String  text = EmuUtil.getProperty(
				props,
				Main.PROP_UI_SCALE ).trim();
    if( text.equalsIgnoreCase( Main.VALUE_UI_SCALE_NONE ) ) {
      this.rbScaleNone.setSelected( true );
      done = true;
    } else {
      item = parseAndPreparePercentText( text );
      if( item != null ) {
	setSelectedScaleItem( item );
	this.rbScaleFix.setSelected( true );
	done = true;
      }
    }
    if( !done ) {
      this.rbScaleDefault.setSelected( true );
    }
    if( item == null ) {
      setSelectedScaleItem( DEFAULT_SCALE_ITEM );
    }
    updComboScaleEnabled();
  }


	/* --- private Methoden --- */

  private String parseAndPreparePercentText( String text )
  {
    String  rv = null;
    Integer v  = Main.parseUIScalePercentText( text );
    if( v != null ) {
      rv = String.format( "%d %%", v );
    }
    return rv;
  }


  private String parseComboScale()
  {
    String rv = null;
    Object o  = this.comboScale.getSelectedItem();
    if( o != null ) {
      rv = parseAndPreparePercentText( o.toString() );
      if( rv != null ) {
	setSelectedScaleItem( rv );
      }
    }
    return rv;
  }


  private void setSelectedScaleItem( String item )
  {
    this.comboScale.removeActionListener( this );
    this.comboScale.setSelectedItem( item );
    this.comboScale.addActionListener( this );
  }


  private void updComboScaleEnabled()
  {
    this.comboScale.setEnabled( this.rbScaleFix.isSelected() );
  }
}
