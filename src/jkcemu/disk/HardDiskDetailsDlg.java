/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer die Festplattengeometrie
 */

package jkcemu.disk;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.util.EventObject;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import jkcemu.base.BaseDlg;
import jkcemu.base.GUIFactory;
import jkcemu.text.TextUtil;


public class HardDiskDetailsDlg extends BaseDlg
{
  private HardDiskInfo      approvedData;
  private JComboBox<String> comboProducer;
  private JTextField        fldProducer;
  private JTextField        fldModel;
  private JTextField        fldCylinders;
  private JTextField        fldHeads;
  private JTextField        fldSectors;
  private JButton           btnOK;
  private JButton           btnCancel;


  public static HardDiskInfo showHardDiskDetailsDlg(
					Window      owner,
					String      title,
					Set<String> producers )
  {
    HardDiskDetailsDlg dlg = new HardDiskDetailsDlg( owner, producers );
    dlg.setTitle( title );
    dlg.setVisible( true );
    return dlg.approvedData;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( (src == this.btnOK) || (src == this.fldSectors) ) {
	  rv = true;
	  doApprove();
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	} else if( src instanceof JTextField ) {
	  rv = true;
	  ((JTextField) src).transferFocus();
	}
      }
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( this.comboProducer != null ) {
      this.comboProducer.requestFocus();
    } else if( this.fldProducer != null ) {
      this.fldProducer.requestFocus();
    }
  }


	/* --- private Konstruktoren und Methoden --- */

  private HardDiskDetailsDlg( Window owner, Set<String> producers )
  {
    super( owner, "JKCEMU Festplattendetails" );
    this.approvedData = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );
    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 1.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    // Eingabefelder
    add( GUIFactory.createLabel( "Hersteller:" ), gbc );
    gbc.gridy++;
    add( GUIFactory.createLabel( "Modell:" ), gbc );
    gbc.gridy++;
    add( GUIFactory.createLabel( "Zylinder:" ), gbc );
    gbc.gridy++;
    add( GUIFactory.createLabel( "K\u00F6pfe:" ), gbc );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Sektoren pro Spur:" ), gbc );

    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.weightx       = 1.0;
    gbc.insets.bottom = 0;
    gbc.gridy         = 0;
    gbc.gridx++;

    this.comboProducer = null;
    this.fldProducer   = null;
    if( producers != null ) {
      if( !producers.isEmpty() ) {
	this.comboProducer = GUIFactory.createComboBox();
	for( String producer : producers ) {
	  this.comboProducer.addItem( producer );
	}
	this.comboProducer.setEditable( true );
	this.comboProducer.setSelectedItem( "" );
	add( this.comboProducer, gbc );
      }
    }
    if( this.comboProducer == null ) {
      this.fldProducer = GUIFactory.createTextField();
      add( this.fldProducer, gbc );
    }

    this.fldModel = GUIFactory.createTextField();
    this.fldModel.addActionListener( this );
    gbc.gridy++;
    add( this.fldModel, gbc );

    this.fldCylinders = GUIFactory.createTextField();
    this.fldCylinders.addActionListener( this );
    gbc.gridy++;
    add( this.fldCylinders, gbc );

    this.fldHeads = GUIFactory.createTextField();
    this.fldHeads.addActionListener( this );
    gbc.gridy++;
    add( this.fldHeads, gbc );

    this.fldSectors   = GUIFactory.createTextField();
    this.fldSectors.addActionListener( this );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.fldSectors, gbc );

    if( this.comboProducer != null ) {
      Font font = this.fldModel.getFont();
      if( font != null ) {
	this.comboProducer.setFont( font );
      }
    }

    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = GUIFactory.createButtonOK();
    this.btnOK.addActionListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse
    pack();
    setParentCentered();
    setResizable( true );
  }


  private void doApprove()
  {
    try {
      String producer = null;
      if( this.comboProducer != null ) {
	Object o = this.comboProducer.getSelectedItem();
	if( o != null ) {
	  producer = TextUtil.emptyToNull( o.toString() );
	}
      } else if( this.fldProducer != null ) {
	producer = TextUtil.emptyToNull( this.fldProducer.getText() );
      }
      String model      = TextUtil.emptyToNull( this.fldModel.getText() );
      String cylText    = TextUtil.emptyToNull( this.fldCylinders.getText() );
      String headText   = TextUtil.emptyToNull( this.fldHeads.getText() );
      String sectorText = TextUtil.emptyToNull( this.fldSectors.getText() );
      if( (model != null)
	  && (cylText != null)
	  && (headText != null)
	  && (sectorText != null) )
      {
	this.approvedData = new HardDiskInfo(
		producer,
		model,
		parseInt( cylText, "Zylinder" ),
		parseInt( headText, "K\u00F6pfe" ),
		parseInt( sectorText, "Sektoren pro Spur" ) );
	doClose();
      } else {
	BaseDlg.showErrorDlg(
		this,
		"Mindestens eins der folgende Felder"
			+ " ist nicht ausgef\u00FCllt:\n"
			+ "  Modell\n"
			+ "  Zylinder\n"
			+ "  K\u00F6pfe\n"
			+ "  Sektoren pro Spur" );
      }
    }
    catch( Exception ex ) {
      BaseDlg.showErrorDlg( this, ex );
    }
  }


  private int parseInt(
		String text,
		String fldName ) throws ParseException
  {
    int rv = 0;
    try {
      rv = Integer.parseInt( text );
    }
    catch( NumberFormatException ex ) {
      throw new ParseException( fldName + ": Ung\u00FCltige Eingabe", 0 );
    }
    return rv;
  }
}
