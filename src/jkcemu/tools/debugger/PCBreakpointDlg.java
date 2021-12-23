/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Anlegen eines Halte-/Log-Punktes auf eine Adresse
 */

package jkcemu.tools.debugger;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import jkcemu.base.GUIFactory;
import jkcemu.base.HexDocument;
import jkcemu.base.UserInputException;


public class PCBreakpointDlg extends AbstractBreakpointDlg
{
  private static class FlagItem
  {
    private int    mask;
    private int    value;
    private String text;

    private FlagItem( int mask, int value, String text )
    {
      this.mask  = mask;
      this.value = value;
      this.text  = text;
    }

    @Override
    public String toString()
    {
      return this.text;
    }
  };


  private static String[] registers = {
				"A", "B", "C", "D", "E", "H", "L",
				"BC", "DE", "HL",
				"IX", "IXH", "IXL",
				"IY", "IYH", "IYL",
				"SP" };

  private LabelDocument       docName;
  private HexDocument         docAddr;
  private HexDocument         docRegMask;
  private HexDocument         docRegValue;
  private JLabel              labelFlag1;
  private JLabel              labelFlag2;
  private JLabel              labelReg1;
  private JLabel              labelReg2;
  private JLabel              labelReg3;
  private JComboBox<FlagItem> comboFlag;
  private JComboBox<String>   comboRegCond;
  private JComboBox<String>   comboRegName;
  private JCheckBox           cbCheckFlag;
  private JCheckBox           cbCheckReg;
  private JTextField          fldAddr;
  private JTextField          fldName;
  private JTextField          fldRegMask;
  private JTextField          fldRegValue;


  public PCBreakpointDlg(
		DebugFrm           debugFrm,
		AbstractBreakpoint breakpoint,
		String             name,
		int                addr )
  {
    super( debugFrm, "Programmadresse", breakpoint );


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

    add( GUIFactory.createLabel( "Adresse (hex):" ), gbc );
    gbc.gridy++;
    add( GUIFactory.createLabel( "Name (optional):" ), gbc );

    this.docAddr = new HexDocument( 4 );
    this.fldAddr = GUIFactory.createTextField( this.docAddr, 0 );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridy   = 0;
    gbc.gridx++;
    add( this.fldAddr, gbc );

    this.docName = new LabelDocument();
    this.fldName = GUIFactory.createTextField( this.docName, 0 );
    gbc.gridy++;
    add( this.fldName, gbc );

    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( GUIFactory.createSeparator(), gbc );

    this.cbCheckReg = GUIFactory.createCheckBox(
		"Zus\u00E4tzlich Registerinhalt vor Befehlsausf\u00FChrung"
			+ " pr\u00FCfen:" );
    gbc.insets.bottom = 0;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.gridy++;
    add( this.cbCheckReg, gbc );

    JPanel panelReg = GUIFactory.createPanel();
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.insets.top  = 0;
    gbc.gridx++;
    gbc.gridy++;
    add( panelReg, gbc );

    panelReg.setLayout( new BoxLayout( panelReg, BoxLayout.X_AXIS ) );

    this.labelReg1 = GUIFactory.createLabel(
				"Nur anhalten/loggen, wenn Register" );
    panelReg.add( this.labelReg1 );
    panelReg.add( Box.createHorizontalStrut( 5 ) );

    this.comboRegName = GUIFactory.createComboBox( registers );
    this.comboRegName.setEditable( false );
    panelReg.add( this.comboRegName );
    panelReg.add( Box.createHorizontalStrut( 5 ) );

    this.labelReg2 = GUIFactory.createLabel( "UND" );
    panelReg.add( this.labelReg2 );
    panelReg.add( Box.createHorizontalStrut( 5 ) );

    this.docRegMask = new HexDocument( 4 );
    this.fldRegMask = GUIFactory.createTextField( this.docRegMask, 4 );
    this.fldRegMask.setToolTipText( "Maske" );
    panelReg.add( this.fldRegMask );
    panelReg.add( Box.createHorizontalStrut( 5 ) );

    this.comboRegCond = GUIFactory.createComboBox( conditions );
    this.comboRegCond.setEditable( false );
    panelReg.add( this.comboRegCond );
    panelReg.add( Box.createHorizontalStrut( 5 ) );

    this.docRegValue = new HexDocument( 4 );
    this.fldRegValue = GUIFactory.createTextField( this.docRegValue, 4 );
    this.fldRegValue.setToolTipText( "Vergleichswert" );
    panelReg.add( this.fldRegValue );
    panelReg.add( Box.createHorizontalStrut( 5 ) );

    this.labelReg3 = GUIFactory.createLabel( "ist." );
    panelReg.add( this.labelReg3 );

    this.cbCheckFlag = GUIFactory.createCheckBox(
		"Zus\u00E4tzlich Flagbedingung vor Befehlsausf\u00FChrung"
			+ " pr\u00FCfen:" );
    gbc.insets.top = 10;
    gbc.gridx = 0;
    gbc.gridy++;
    add( this.cbCheckFlag, gbc );

    JPanel panelFlag = GUIFactory.createPanel();
    gbc.weightx      = 0.0;
    gbc.fill         = GridBagConstraints.NONE;
    gbc.insets.top   = 0;
    gbc.gridx++;
    gbc.gridy++;
    add( panelFlag, gbc );

    panelFlag.setLayout( new BoxLayout( panelFlag, BoxLayout.X_AXIS ) );

    this.labelFlag1 = GUIFactory.createLabel(
				"Nur anhalten/loggen, wenn Flagbedingung" );
    panelFlag.add( this.labelFlag1 );
    panelFlag.add( Box.createHorizontalStrut( 5 ) );

    this.comboFlag = GUIFactory.createComboBox();
    this.comboFlag.setEditable( false );
    this.comboFlag.addItem( new FlagItem( 0x80, 0x80, "M (S=1)" ) );
    this.comboFlag.addItem( new FlagItem( 0x80, 0,    "P (S=0)" ) );
    this.comboFlag.addItem( new FlagItem( 0x40, 0x40, "Z (Z=1)" ) );
    this.comboFlag.addItem( new FlagItem( 0x40, 0,    "NZ (Z=0)" ) );
    this.comboFlag.addItem( new FlagItem( 0x04, 0x04, "PE (PV=1)" ) );
    this.comboFlag.addItem( new FlagItem( 0x04, 0,    "PO (PV=0)" ) );
    this.comboFlag.addItem( new FlagItem( 0x01, 0x01, "C (C=1)" ) );
    this.comboFlag.addItem( new FlagItem( 0x01, 0,    "NC (C=0)" ) );
    panelFlag.add( this.comboFlag );
    panelFlag.add( Box.createHorizontalStrut( 5 ) );

    this.labelFlag2 = GUIFactory.createLabel( "erf\u00FCllt ist." );
    panelFlag.add( this.labelFlag2 );

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
    boolean regValueState = false;
    String  regName       = null;
    String  regCond       = null;
    int     regMask       = 0xFFFF;
    int     regValue      = 0;
    int     flagMask      = 0;
    int     flagValue     = 0;
    if( breakpoint != null ) {
      if( breakpoint instanceof PCBreakpoint ) {
	PCBreakpoint bp = (PCBreakpoint) breakpoint;
	this.fldName.setText( bp.getName() );
	this.docAddr.setValue( bp.getAddress(), 4 );
	regName   = bp.getRegName();
	regMask   = bp.getRegMask();
	regCond   = bp.getRegCondition();
	regValue  = bp.getRegValue();
	flagMask  = bp.getFlagMask();
	flagValue = bp.getFlagValue();
	if( (regName != null) && (regCond != null) ) {
	  regValueState = true;
	}
      }
    } else {
      if( name != null ) {
	this.fldName.setText( name );
      }
      if( addr >= 0 ) {
	this.docAddr.setValue( addr, 4 );
      }
    }
    this.comboRegName.setSelectedItem( regName != null ? regName : "A" );
    regChanged();
    this.docRegMask.setValue( regMask, this.docRegMask.getMaxLength() );
    this.comboRegCond.setSelectedItem( regCond != null ? regCond : "=" );
    this.docRegValue.setValue( regValue, this.docRegValue.getMaxLength() );
    this.cbCheckReg.setSelected( regValueState );
    updCheckRegFieldsEnabled();

    boolean flagMatched = false;
    int     nItems      = this.comboFlag.getItemCount();
    for( int i = 0; i < nItems; i++ ) {
      FlagItem item = this.comboFlag.getItemAt( i );
      if( item != null ) {
	if( (item.mask == flagMask) && (item.value == flagValue) ) {
	  this.comboFlag.setSelectedItem( item );
	  flagMatched = true;
	  break;
	}
      }
    }
    this.cbCheckFlag.setSelected( flagMatched );
    updCheckFlagFieldsEnabled();


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );


    // Sonstiges
    this.docName.setReverseCase( true );
    this.fldRegMask.setColumns( 0 );
    this.fldRegValue.setColumns( 0 );
    this.fldAddr.addActionListener( this );
    this.fldName.addActionListener( this );
    this.cbCheckReg.addActionListener( this );
    this.comboRegName.addActionListener( this );
    this.comboFlag.addActionListener( this );
    this.fldRegMask.addActionListener( this );
    this.fldRegValue.addActionListener( this );
    this.cbCheckFlag.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.cbCheckReg ) {
	  rv = true;
	  updCheckRegFieldsEnabled();
	}
	else if( src == this.comboRegName ) {
	  rv = true;
	  regChanged();
	}
	else if( src == this.fldAddr ) {
	  rv = true;
	  this.fldName.requestFocus();
	}
	else if( (src == this.fldName) || (src == this.fldRegValue) ) {
	  rv = true;
	  doApprove();
	}
	else if( src == this.fldRegMask ) {
	  rv = true;
	  this.comboRegCond.requestFocus();
	}
	else if( src == this.cbCheckFlag ) {
	  rv = true;
	  updCheckFlagFieldsEnabled();
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
    String curFldName = "Adresse";
    try {
      boolean status    = true;
      int     addr      = this.docAddr.intValue();
      String  regName   = null;
      String  regCond   = null;
      int     regMask   = 0xFFFF;
      int     regValue  = 0;
      int     flagMask  = 0;
      int     flagValue = 0;
      if( this.cbCheckReg.isSelected() ) {
	Object o = this.comboRegName.getSelectedItem();
	if( o != null ) {
	  regName = o.toString();
	}
	o = this.comboRegCond.getSelectedItem();
	if( o != null ) {
	  regCond = o.toString();
	}
	if( (regName != null) && (regCond != null) ) {
	  boolean is8Bit = true;
	  if( regName.length() == 2 ) {
	    is8Bit = false;
	  }
	  int m      = (is8Bit ? 0xFF : 0xFFFF);
	  curFldName = "Maske";
	  regMask    = this.docRegMask.intValue() & m;
	  curFldName = "Wert";
	  regValue   = this.docRegValue.intValue() & m;
	  status     = checkMaskValue( is8Bit, regMask, regValue );
	}
      }
      if( this.cbCheckFlag.isSelected() ) {
	Object o = this.comboFlag.getSelectedItem();
	if( o != null ) {
	  if( o instanceof FlagItem ) {
	    flagMask  = ((FlagItem) o).mask;
	    flagValue = ((FlagItem) o).value;
	  }
	}
      }
      if( status ) {
	approveBreakpoint(
		new PCBreakpoint(
				this.debugFrm,
				this.docName.getLabel(),
				addr,
				regName,
				regMask,
				regCond,
				regValue,
				flagMask,
				flagValue ) );
      }
    }
    catch( NumberFormatException ex ) {
      showInvalidFmt( curFldName );
    }
    catch( InvalidParamException | UserInputException ex ) {
      showErrorDlg( this, ex );
    }
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.fldAddr.removeActionListener( this );
      this.fldName.removeActionListener( this );
      this.cbCheckReg.removeActionListener( this );
      this.comboRegName.removeActionListener( this );
      this.comboFlag.removeActionListener( this );
      this.fldRegMask.removeActionListener( this );
      this.fldRegValue.removeActionListener( this );
      this.cbCheckFlag.removeActionListener( this );
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( this.fldAddr != null ) {
      this.fldAddr.requestFocus();
    }
  }


	/* --- private Konstruktoren und Methoden --- */

  private void regChanged()
  {
    int    n = 2;
    Object o = this.comboRegName.getSelectedItem();
    if( o != null ) {
      String s = o.toString();
      if( s != null ) {
	if( s.length() == 2 ) {
	  n = 4;
	}
      }
    }
    this.docRegMask.setMaxLength( n, 'F' );
    this.docRegValue.setMaxLength( n, '0' );
  }


  private void updCheckFlagFieldsEnabled()
  {
    boolean state = this.cbCheckFlag.isSelected();
    this.labelFlag1.setEnabled( state );
    this.labelFlag2.setEnabled( state );
    this.comboFlag.setEnabled( state );
  }


  private void updCheckRegFieldsEnabled()
  {
    boolean state = this.cbCheckReg.isSelected();
    this.labelReg1.setEnabled( state );
    this.labelReg2.setEnabled( state );
    this.labelReg3.setEnabled( state );
    this.comboRegName.setEnabled( state );
    this.fldRegMask.setEnabled( state );
    this.comboRegCond.setEnabled( state );
    this.fldRegValue.setEnabled( state );
  }
}
