/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Anlegen eines Halte-/Log-Punktes auf eine Speicherzelle
 * bzw. einen Speicherbereich
 */

package jkcemu.tools.debugger;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import jkcemu.base.GUIFactory;
import jkcemu.base.HexDocument;
import jkcemu.base.UserInputException;


public class MemoryBreakpointDlg extends AbstractBreakpointDlg
{
  private LabelDocument     docName;
  private HexDocument       docBegAddr;
  private HexDocument       docEndAddr;
  private HexDocument       docMask;
  private HexDocument       docValue;
  private JCheckBox         cbOnRead;
  private JCheckBox         cbOnWrite;
  private JCheckBox         cbCheckValue;
  private JLabel            labelValue1;
  private JLabel            labelValue2;
  private JComboBox<String> comboCond;
  private JTextField        fldName;
  private JTextField        fldBegAddr;
  private JTextField        fldEndAddr;
  private JTextField        fldMask;
  private JTextField        fldValue;


  public MemoryBreakpointDlg(
			DebugFrm           debugFrm,
			AbstractBreakpoint breakpoint,
			String             name,
			int                begAddr,
			int                endAddr )
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

    add( GUIFactory.createLabel( "Adresse/Anfangsadresse (hex):" ), gbc );

    this.docBegAddr = new HexDocument( 4 );
    this.fldBegAddr = GUIFactory.createTextField( this.docBegAddr, 0 );
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
    add( GUIFactory.createLabel( "Endadresse (optional, hex):" ), gbc );

    this.docEndAddr = new HexDocument( 4 );
    this.fldEndAddr = GUIFactory.createTextField( this.docEndAddr, 0 );
    gbc.anchor      = GridBagConstraints.WEST;
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.gridwidth   = 2;
    gbc.gridx++;
    add( this.fldEndAddr, gbc );

    gbc.anchor    = GridBagConstraints.EAST;
    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.gridwidth = 1;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Name (optional):" ), gbc );

    this.docName  = new LabelDocument();
    this.fldName  = GUIFactory.createTextField( this.docName, 0 );
    gbc.anchor    = GridBagConstraints.WEST;
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = 2;
    gbc.gridx++;
    add( this.fldName, gbc );

    gbc.anchor        = GridBagConstraints.EAST;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = 1;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Anhalten/Loggen beim:" ), gbc );

    this.cbOnRead = GUIFactory.createCheckBox( "Lesen", true );
    gbc.anchor     = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.cbOnRead, gbc );

    this.cbOnWrite = GUIFactory.createCheckBox( "Schreiben", true );
    gbc.gridx++;
    add( this.cbOnWrite, gbc );

    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.weightx    = 1.0;
    gbc.insets.top = 5;
    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( GUIFactory.createSeparator(), gbc );

    this.cbCheckValue = GUIFactory.createCheckBox(
		"Zus\u00E4tzlich Wert der Speicherzelle"
			+ " bzw. zu schreibenden Wert pr\u00FCfen" );
    gbc.insets.bottom = 0;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.gridy++;
    add( this.cbCheckValue, gbc );

    JPanel panelValue = GUIFactory.createPanel( new GridBagLayout() );
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

    this.labelValue1 = GUIFactory.createLabel(
				"Nur anhalten/loggen wenn Wert UND" );
    panelValue.add( this.labelValue1, gbcValue );

    this.docMask = new HexDocument( 2 );
    this.fldMask = GUIFactory.createTextField( this.docMask, 2 );
    this.fldMask.setToolTipText( "Maske" );
    gbcValue.fill        = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx     = 0.5;
    gbcValue.insets.left = 5;
    gbcValue.gridx++;
    panelValue.add( this.fldMask, gbcValue );

    this.comboCond = GUIFactory.createComboBox( conditions );
    this.comboCond.setEditable( false );
    gbcValue.fill    = GridBagConstraints.NONE;
    gbcValue.weightx = 0.0;
    gbcValue.gridx++;
    panelValue.add( this.comboCond, gbcValue );

    this.docValue = new HexDocument( 2 );
    this.fldValue = GUIFactory.createTextField( this.docValue, 2 );
    this.fldValue.setToolTipText( "Vergleichswert" );
    gbcValue.fill    = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx = 0.5;
    gbcValue.gridx++;
    panelValue.add( this.fldValue, gbcValue );

    this.labelValue2 = GUIFactory.createLabel( "ist." );
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
    add( GUIFactory.createSeparator(), gbc );

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
	begAddr = bp.getBegAddress();
	endAddr = bp.getEndAddress();
	this.docBegAddr.setValue( begAddr, 4 );
	if( endAddr > begAddr ) {
	  this.docEndAddr.setValue( endAddr, 4 );
	}
	this.fldName.setText( bp.getName() );
	this.cbOnRead.setSelected( bp.getOnRead() );
	this.cbOnWrite.setSelected( bp.getOnWrite() );
	mask  = bp.getMask();
	cond  = bp.getCondition();
	value = bp.getValue();
	if( cond != null ) {
	  valueState = true;
	}
      }
    } else {
      if( name != null ) {
	this.fldName.setText( name );
      }
      if( begAddr >= 0 ) {
	this.docBegAddr.setValue( begAddr, 4 );
	if( endAddr > begAddr ) {
	  this.docEndAddr.setValue( endAddr, 4 );
	}
      }
    }
    this.docMask.setValue( mask, 2 );
    this.comboCond.setSelectedItem( cond != null ? cond : "=" );
    this.docValue.setValue( value, 2 );
    this.cbCheckValue.setSelected( valueState );
    updCheckValueFieldsEnabled();


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );


    // Sonstiges
    this.docName.setReverseCase( true );
    this.fldMask.setColumns( 0 );
    this.fldValue.setColumns( 0 );
    this.fldBegAddr.addActionListener( this );
    this.fldEndAddr.addActionListener( this );
    this.fldName.addActionListener( this );
    this.cbOnRead.addActionListener( this );
    this.cbOnWrite.addActionListener( this );
    this.cbCheckValue.addActionListener( this );
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
	if( src == this.cbCheckValue ) {
	  rv = true;
	  updCheckValueFieldsEnabled();
	}
	else if( src == this.fldBegAddr ) {
	  rv = true;
	  this.fldEndAddr.requestFocus();
	}
	else if( src == this.fldEndAddr ) {
	  rv = true;
	  this.fldName.requestFocus();
	}
	else if( (src == this.fldName) || (src == this.fldValue) ) {
	  rv = true;
	  doApprove();
	}
	else if( (src == this.cbOnRead) || (src == this.cbOnWrite) ) {
	  rv = true;
	  if( !this.cbOnRead.isSelected()
	      && !this.cbOnWrite.isSelected() )
	  {
	    if( src == this.cbOnRead ) {
	      this.cbOnWrite.setSelected( true );
	    } else {
	      this.cbOnRead.setSelected( true );
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
    String curFldName = "Anfangsadresse";
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
      if( this.cbCheckValue.isSelected() ) {
	Object o = this.comboCond.getSelectedItem();
	if( o != null ) {
	  cond = o.toString();
	}
	if( cond != null ) {
	  curFldName = "Maske";
	  mask       = this.docMask.intValue() & 0xFF;
	  curFldName = "Vergleichswert";
	  value   = this.docValue.intValue() & 0xFF;
	  status  = checkMaskValue( true, mask, value );
	}
      }
      if( status ) {
	approveBreakpoint(
		new MemoryBreakpoint(
				this.debugFrm,
				this.docName.getLabel(),
				begAddr,
				endAddr,
				this.cbOnRead.isSelected(),
				this.cbOnWrite.isSelected(),
				mask,
				cond,
				value ) );
      }
    }
    catch( InvalidParamException | NumberFormatException ex ) {
      showInvalidFmt( curFldName );
    }
    catch( UserInputException ex ) {
      showErrorDlg( this, ex );
    }
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.fldBegAddr.removeActionListener( this );
      this.fldEndAddr.removeActionListener( this );
      this.fldName.removeActionListener( this );
      this.cbOnRead.removeActionListener( this );
      this.cbOnWrite.removeActionListener( this );
      this.cbCheckValue.removeActionListener( this );
      this.fldMask.removeActionListener( this );
      this.fldValue.removeActionListener( this );
    }
    return rv;
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
    boolean state = this.cbCheckValue.isSelected();
    this.labelValue1.setEnabled( state );
    this.labelValue2.setEnabled( state );
    this.fldMask.setEnabled( state );
    this.comboCond.setEnabled( state );
    this.fldValue.setEnabled( state );
  }
}
