/*
 * (c) 2011-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die KCNet-Einstellungen
 */

package jkcemu.net;

import java.awt.*;
import java.lang.*;
import java.util.*;
import java.util.regex.PatternSyntaxException;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.Document;
import jkcemu.base.*;


public class KCNetSettingsFld
			extends AbstractSettingsFld
			implements DocumentListener
{
  private JTextField fldIpAddr;
  private JTextField fldSubnetMask;
  private JTextField fldGateway;
  private JTextField fldDNSServer;
  private JCheckBox  btnAutoConfig;


  public KCNetSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

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
	new JLabel( "Beim \"Einschalten\" KCNet konfigurieren (optional):" ),
	gbc );

    gbc.insets.left = 50;
    gbc.gridwidth   = 1;
    gbc.gridy++;
    add( new JLabel( "IP-Adresse (d.d.d.d):" ), gbc );
    gbc.gridy++;
    add( new JLabel( "Subnetzmaske (d.d.d.d):" ), gbc );
    gbc.gridy++;
    add( new JLabel( "Gateway (d.d.d.d):" ), gbc );
    gbc.gridy++;
    add( new JLabel( "DNS-Server (d.d.d.d):" ), gbc );

    this.btnAutoConfig = new JCheckBox(
		"IP-Adressen der leer gelassenen Felder"
			+ " automatisch ermitteln",
		true );
    this.btnAutoConfig.addActionListener( this );
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridy++;
    add( this.btnAutoConfig, gbc );

    this.fldIpAddr  = createJTextField();
    gbc.insets.left = 5;
    gbc.gridwidth   = 1;
    gbc.gridy       = 1;
    gbc.gridx++;
    add( this.fldIpAddr, gbc );

    this.fldSubnetMask = createJTextField();
    gbc.gridy++;
    add( this.fldSubnetMask, gbc );

    this.fldGateway = createJTextField();
    gbc.gridy++;
    add( this.fldGateway, gbc );

    this.fldDNSServer = createJTextField();
    gbc.gridy++;
    add( this.fldDNSServer, gbc );
  }


	/* --- DocumentListener --- */

  public void changedUpdate( DocumentEvent e )
  {
    fireDataChanged();
  }


  public void insertUpdate( DocumentEvent e )
  {
    fireDataChanged();
  }


  public void removeUpdate( DocumentEvent e )
  {
    fireDataChanged();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    props.setProperty(
		"jkcemu.kcnet.ip_address",
		parseIpAddrText( this.fldIpAddr, "IP-Adresse" ) );
    props.setProperty(
		"jkcemu.kcnet.subnet_mask",
		parseIpAddrText( this.fldSubnetMask, "Subnetzmaske" ) );
    props.setProperty(
		"jkcemu.kcnet.gateway",
		parseIpAddrText( this.fldGateway, "Gateway" ) );
    props.setProperty(
		"jkcemu.kcnet.dns_server",
		parseIpAddrText( this.fldDNSServer, "DNS-Server" ) );
    EmuUtil.setProperty(
		props,
		"jkcemu.kcnet.auto_config",
		this.btnAutoConfig.isSelected() );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e.getSource() == this.btnAutoConfig ) {
      rv = true;
      fireDataChanged();
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    this.fldIpAddr.setText(
		EmuUtil.getProperty( props, "jkcemu.kcnet.ip_address" ) );

    this.fldSubnetMask.setText(
		EmuUtil.getProperty( props, "jkcemu.kcnet.subnet_mask" ) );

    this.fldGateway.setText(
		EmuUtil.getProperty( props, "jkcemu.kcnet.gateway" ) );

    this.fldDNSServer.setText(
		EmuUtil.getProperty( props, "jkcemu.kcnet.dns_server" ) );

    this.btnAutoConfig.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				"jkcemu.kcnet.auto_config",
				true ) );
  }


	/* --- private Methoden --- */

  private JTextField createJTextField()
  {
    JTextField fld = new JTextField( 15 );
    Document   doc = fld.getDocument();
    if( doc != null ) {
      doc.addDocumentListener( this );
    }
    return fld;
  }


  private String parseIpAddrText(
			JTextField fld,
			String     fieldName ) throws UserInputException
  {
    String rv = "";
    String s  = fld.getText();
    if( s != null ) {
      s = s.trim();
      if( !s.isEmpty() ) {
	rv = null;
	try {
	  String[] elems = s.split( "\\.", 5 );
	  if( elems != null ) {
	    if( elems.length == 4 ) {
	      int v1 = Integer.parseInt( elems[ 0 ] );
	      int v2 = Integer.parseInt( elems[ 1 ] );
	      int v3 = Integer.parseInt( elems[ 2 ] );
	      int v4 = Integer.parseInt( elems[ 3 ] );
	      if( (v1 >= 0) && (v1 <= 255)
		  && (v2 >= 0) && (v2 <= 255)
		  && (v3 >= 0) && (v3 <= 255)
		  && (v4 >= 0) && (v4 <= 255) )
	      {
		rv = String.format( "%d.%d.%d.%d",v1, v2, v3, v4 );
	      }
	    }
	  }
	}
	catch( NumberFormatException ex ) {}
	catch( PatternSyntaxException ex ) {}
	if( rv == null ) {
	  throw new UserInputException(
				fieldName + ": Ung\u00FCltige Eingabe",
				fieldName );
	}
      }
    }
    return rv;
  }
}

