/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Einstellungen fuer die Bildschirmausgabe
 */

package jkcemu.settings;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserInputException;


public class SpeedSettingsFld
			extends AbstractSettingsFld
			implements DocumentListener
{
  private JLabel       labelSpeedUnit;
  private JRadioButton rbSpeedDefault;
  private JRadioButton rbSpeedValue;
  private JTextField   fldSpeed;
  private Document     docSpeed;
  private NumberFormat fmtSpeed;


  public SpeedSettingsFld( SettingsFrm settingsFrm )
  {
    super( settingsFrm );
    this.fmtSpeed = NumberFormat.getNumberInstance();
    if( this.fmtSpeed instanceof DecimalFormat ) {
      ((DecimalFormat) this.fmtSpeed).applyPattern( "#0.0##" );
    }
    setLayout( new BorderLayout() );

    JPanel panel = GUIFactory.createPanel( new GridBagLayout() );
    add( GUIFactory.createScrollPane( panel ), BorderLayout.CENTER );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    panel.add(
	GUIFactory.createLabel( "Geschwindigkeit des emulierten Systems:" ),
	gbc );

    ButtonGroup grpSpeed = new ButtonGroup();
    this.rbSpeedDefault = GUIFactory.createRadioButton(
				"Begrenzen auf Originalgeschwindigkeit",
				true );
    grpSpeed.add( this.rbSpeedDefault );
    gbc.insets.top  = 0;
    gbc.insets.left = 50;
    gbc.gridy++;
    panel.add( this.rbSpeedDefault, gbc );

    this.rbSpeedValue = GUIFactory.createRadioButton( "Begrenzen auf:" );
    grpSpeed.add( this.rbSpeedValue );
    gbc.insets.bottom = 5;
    gbc.gridwidth     = 1;
    gbc.gridy++;
    panel.add( this.rbSpeedValue, gbc );

    this.fldSpeed = GUIFactory.createTextField( 5 );
    this.docSpeed = this.fldSpeed.getDocument();
    gbc.insets.left = 5;
    gbc.gridx++;
    panel.add( this.fldSpeed, gbc );

    this.labelSpeedUnit = GUIFactory.createLabel( "MHz" );
    gbc.insets.left = 5;
    gbc.gridx++;
    panel.add( this.labelSpeedUnit, gbc );


    // Listener
    this.rbSpeedDefault.addActionListener( this );
    this.rbSpeedValue.addActionListener( this );
    if( this.docSpeed != null ) {
      this.docSpeed.addDocumentListener( this );
    }
  }


  public void setDefaultSpeedKHz( int khz )
  {
    if( !this.rbSpeedValue.isSelected() )
      setSpeedValueFld( khz );
  }


	/* --- DocumentListener --- */

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void insertUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void removeUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    if( this.rbSpeedValue.isSelected() ) {
      boolean done = false;
      String  msg  = "Sie m\u00FCssen einen Wert f\u00FCr"
				+ " die max. Geschwindigkeit eingeben.";
      String  text = this.fldSpeed.getText();
      if( text != null ) {
	if( !text.isEmpty() ) {
	  msg = "Die eingegebene max. Geschwindigkeit ist ung\u00FCltig.";
	  try {
	    Number mhzValue = this.fmtSpeed.parse( text );
	    if( mhzValue != null ) {
	      long khzValue = Math.round( mhzValue.doubleValue() * 1000.0 );
	      if( khzValue > 0 ) {
		props.setProperty(
			EmuThread.PROP_MAXSPEED_KHZ,
			String.valueOf( khzValue ) );
		done = true;
	      }
	    }
	  }
	  catch( ParseException ex ) {}
	}
      }
      if( !done ) {
	throw new UserInputException( msg );
      }
    } else {
      props.setProperty( EmuThread.PROP_MAXSPEED_KHZ, "default" );
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( (src == this.rbSpeedDefault) || (src == this.rbSpeedValue) ) {
      rv = true;
      updSpeedFieldsEnabled();
      fireDataChanged();
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    boolean done = false;
    int     defaultKHz = EmuThread.getDefaultSpeedKHz( props );
    String  speedText  = EmuUtil.getProperty(
		props,
		EmuThread.PROP_MAXSPEED_KHZ ).toLowerCase();
    if( !speedText.isEmpty() ) {
      try {
	int value = Integer.parseInt( speedText );
	if( (value > 0) && (value != defaultKHz) ) {
	  setSpeedValueFld( value );
	  this.rbSpeedValue.setSelected( true );
	  done = true;
	}
      }
      catch( NumberFormatException ex ) {}
    }
    if( !done ) {
      setSpeedValueFld( defaultKHz );
      this.rbSpeedDefault.setSelected( true );
    }
    updSpeedFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void docChanged( DocumentEvent e )
  {
    if( e.getDocument() == this.docSpeed )
      fireDataChanged();
  }


  private void setSpeedValueFld( int khzValue )
  {
    this.fldSpeed.setText(
		this.fmtSpeed.format( (double) khzValue / 1000.0 ) );
  }


  private void updSpeedFieldsEnabled()
  {
    boolean state = this.rbSpeedValue.isSelected();
    this.fldSpeed.setEnabled( state );
    this.labelSpeedUnit.setEnabled( state );
  }
}
