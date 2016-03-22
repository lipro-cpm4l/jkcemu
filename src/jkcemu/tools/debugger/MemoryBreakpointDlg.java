/*
 * (c) 2011-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Anlegen eines Halte-/Log-Punktes auf eine Speicherzelle
 * bzw. einen Speicherbereich
 */

package jkcemu.tools.debugger;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.*;


public class MemoryBreakpointDlg extends AbstractBreakpointDlg
{
  private HexDocument       docBegAddr;
  private HexDocument       docEndAddr;
  private HexDocument       docMask;
  private HexDocument       docValue;
  private JCheckBox         btnOnRead;
  private JCheckBox         btnOnWrite;
  private JCheckBox         btnCheckValue;
  private JLabel            labelValue1;
  private JLabel            labelValue2;
  private JComboBox<String> comboCond;
  private JTextField        fldBegAddr;
  private JTextField        fldEndAddr;
  private JTextField        fldMask;
  private JTextField        fldValue;


  public MemoryBreakpointDlg(
			DebugFrm           debugFrm,
			AbstractBreakpoint breakpoint )
  {
    super( debugFrm, "Speicherbereich", breakpoint );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.EAST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    add( new JLabel( "Adresse/Anfangsadresse:" ), gbc );

    this.docBegAddr = new HexDocument( 4 );
    this.fldBegAddr = new JTextField( this.docBegAddr, "", 0 );
    gbc.anchor      = GridBagConstraints.WEST;
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.gridwidth   = 2;
    gbc.gridx++;
    add( this.fldBegAddr, gbc );

    gbc.anchor    = GridBagConstraints.EAST;
    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.gridwidth = 1;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( new JLabel( "Endadresse (optional):" ), gbc );

    this.docEndAddr = new HexDocument( 4 );
    this.fldEndAddr = new JTextField( this.docEndAddr, "", 0 );
    gbc.anchor      = GridBagConstraints.WEST;
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.gridwidth   = 2;
    gbc.gridx++;
    add( this.fldEndAddr, gbc );

    gbc.anchor        = GridBagConstraints.EAST;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = 1;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( new JLabel( "Anhalten/Loggen beim:" ), gbc );

    this.btnOnRead = new JCheckBox( "Lesen", true );
    gbc.anchor     = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.btnOnRead, gbc );

    this.btnOnWrite = new JCheckBox( "Schreiben", true );
    gbc.gridx++;
    add( this.btnOnWrite, gbc );

    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.weightx    = 1.0;
    gbc.insets.top = 5;
    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    this.btnCheckValue = new JCheckBox(
		"Zus\u00E4tzlich Wert der Speicherzelle"
			+ " bzw. zu schreibenden Wert pr\u00FCfen",
		false );
    gbc.insets.bottom = 0;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.gridy++;
    add( this.btnCheckValue, gbc );

    JPanel panelValue = new JPanel( new GridBagLayout() );
    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.weightx       = 1.0;
    gbc.insets.left   = 50;
    gbc.gridy++;
    add( panelValue, gbc );

    GridBagConstraints gbcValue = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 0, 0, 0, 0 ),
						0, 0 );

    this.labelValue1 = new JLabel( "Nur anhalten/loggen wenn Wert UND" );
    panelValue.add( this.labelValue1, gbcValue );

    this.docMask = new HexDocument( 2 );
    this.fldMask = new JTextField( this.docMask, "", 2 );
    this.fldMask.setToolTipText( "Maske" );
    gbcValue.fill        = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx     = 0.5;
    gbcValue.insets.left = 5;
    gbcValue.gridx++;
    panelValue.add( this.fldMask, gbcValue );

    this.comboCond = new JComboBox<>( conditions );
    this.comboCond.setEditable( false );
    gbcValue.fill    = GridBagConstraints.NONE;
    gbcValue.weightx = 0.0;
    gbcValue.gridx++;
    panelValue.add( this.comboCond, gbcValue );

    this.docValue = new HexDocument( 2 );
    this.fldValue = new JTextField( this.docValue, "", 2 );
    this.fldValue.setToolTipText( "Vergleichswert" );
    gbcValue.fill    = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx = 0.5;
    gbcValue.gridx++;
    panelValue.add( this.fldValue, gbcValue );

    this.labelValue2 = new JLabel( "ist." );
    gbcValue.fill    = GridBagConstraints.NONE;
    gbcValue.weightx = 0.0;
    gbcValue.gridx++;
    panelValue.add( this.labelValue2, gbcValue );

    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.insets.top  = 10;
    gbc.insets.left = 5;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    gbc.anchor     = GridBagConstraints.CENTER;
    gbc.fill       = GridBagConstraints.NONE;
    gbc.weightx    = 0.0;
    gbc.insets.top = 5;
    gbc.gridy++;
    add( createGeneralButtons(), gbc );


    // Vorbelegungen
    boolean valueState = false;
    String  cond       = null;
    int     mask       = 0xFF;
    int     value      = 0;
    if( breakpoint != null ) {
      if( breakpoint instanceof MemoryBreakpoint ) {
	MemoryBreakpoint bp = (MemoryBreakpoint) breakpoint;
	this.docBegAddr.setValue( bp.getBegAddress(), 4 );
	int endAddr = bp.getEndAddress();
	if( endAddr >= 0 ) {
	  this.docEndAddr.setValue( endAddr, 4 );
	}
	this.btnOnRead.setSelected( bp.getOnRead() );
	this.btnOnWrite.setSelected( bp.getOnWrite() );
	mask  = bp.getMask();
	cond  = bp.getCondition();
	value = bp.getValue();
	if( cond != null ) {
	  valueState = true;
	}
      }
    }
    this.docMask.setValue( mask, 2 );
    this.comboCond.setSelectedItem( cond != null ? cond : "=" );
    this.docValue.setValue( value, 2 );
    this.btnCheckValue.setSelected( valueState );
    updCheckValueFieldsEnabled();


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );


    // Sonstiges
    this.fldMask.setColumns( 0 );
    this.fldValue.setColumns( 0 );
    this.fldBegAddr.addActionListener( this );
    this.fldEndAddr.addActionListener( this );
    this.btnOnRead.addActionListener( this );
    this.btnOnWrite.addActionListener( this );
    this.btnCheckValue.addActionListener( this );
    this.fldMask.addActionListener( this );
    this.fldValue.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.btnCheckValue ) {
	  rv = true;
	  updCheckValueFieldsEnabled();
	}
	else if( src == this.fldBegAddr ) {
	  rv = true;
	  this.fldEndAddr.requestFocus();
	}
	else if( (src == this.fldEndAddr) || (src == this.fldValue) ) {
	  rv = true;
	  doApprove();
	}
	else if( (src == this.btnOnRead) || (src == this.btnOnWrite) ) {
	  rv = true;
	  if( !this.btnOnRead.isSelected()
	      && !this.btnOnWrite.isSelected() )
	  {
	    if( src == this.btnOnRead ) {
	      this.btnOnWrite.setSelected( true );
	    } else {
	      this.btnOnRead.setSelected( true );
	    }
	  }
	}
	else if( src == this.fldMask ) {
	  rv = true;
	  this.comboCond.requestFocus();
	}
      }
    }
    if( !rv ) {
      rv = super.doAction( e );
    }
    return rv;
  }


  @Override
  protected void doApprove()
  {
    String fldName = "Anfangsadresse";
    try {
      boolean status  = true;
      int     begAddr = this.docBegAddr.intValue();
      int     endAddr = -1;
      Integer tmpEndAddr = this.docEndAddr.getInteger();
      if( tmpEndAddr != null ) {
	endAddr = tmpEndAddr.intValue() & 0xFFFF;
      }
      String cond  = null;
      int    mask  = 0xFF;
      int    value = 0;
      if( this.btnCheckValue.isSelected() ) {
	Object o = this.comboCond.getSelectedItem();
	if( o != null ) {
	  cond = o.toString();
	}
	if( cond != null ) {
	  fldName = "Maske";
	  mask    = this.docMask.intValue() & 0xFF;
	  fldName = "Vergleichswert";
	  value   = this.docValue.intValue() & 0xFF;
	  status  = checkMaskValue( true, mask, value );
	}
      }
      if( status ) {
	approveBreakpoint( new MemoryBreakpoint(
					this.debugFrm,
					begAddr,
					endAddr,
					this.btnOnRead.isSelected(),
					this.btnOnWrite.isSelected(),
					mask,
					cond,
					value ) );
      }
    }
    catch( NumberFormatException ex ) {
      showInvalidFmt( fldName );
    }
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( this.fldBegAddr != null )
      this.fldBegAddr.requestFocus();
  }


	/* --- private Konstruktoren und Methoden --- */

  private void updCheckValueFieldsEnabled()
  {
    boolean state = this.btnCheckValue.isSelected();
    this.labelValue1.setEnabled( state );
    this.labelValue2.setEnabled( state );
    this.fldMask.setEnabled( state );
    this.comboCond.setEnabled( state );
    this.fldValue.setEnabled( state );
  }
}

