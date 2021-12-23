/*
 * (c) 2015-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Bearbeiten einer Variable
 */

package jkcemu.tools.debugger;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import jkcemu.base.BaseDlg;
import jkcemu.base.GUIFactory;
import jkcemu.base.HexDocument;
import jkcemu.base.UserInputException;


public class VarDataDlg extends BaseDlg
{
  private DebugFrm      debugFrm;
  private VarData       oldVarData;
  private VarData       appliedVarData;
  private JTextField    fldAddr;
  private JTextField    fldName;
  private HexDocument   docAddr;
  private LabelDocument docName;
  private JRadioButton  rbInt1;
  private JRadioButton  rbInt2;
  private JRadioButton  rbInt3;
  private JRadioButton  rbInt4;
  private JRadioButton  rbBCDec6;
  private JRadioButton  rbByteArray;
  private JRadioButton  rbPointer;
  private JRadioButton  rbLE;
  private JRadioButton  rbBE;
  private JSpinner      spinnerSize;
  private JLabel        labelSize;
  private JLabel        labelByteOrder;
  private JButton       btnOK;
  private JButton       btnCancel;


  public static VarData showEditVarDlg( DebugFrm debugFrm, VarData varData )
  {
    VarDataDlg dlg = new VarDataDlg( debugFrm, varData, null, -1, -1 );
    dlg.setVisible( true );
    return dlg.appliedVarData;
  }


  public static VarData showNewVarDlg(
				DebugFrm debugFrm,
				String   name,
				int      addr,
				int      size )
  {
    VarDataDlg dlg = new VarDataDlg(
				debugFrm,
				null,
				name,
				addr,
				size );
    dlg.setVisible( true );
    return dlg.appliedVarData;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.fldAddr ) {
	rv = true;
	this.fldName.requestFocus();
      } else if( (src == this.fldName) || (src == this.btnOK) ) {
	rv = true;
	doApply();
      } else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      } else if( src instanceof JRadioButton ) {
	rv = true;
	updFieldsEnabled();
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.fldName.removeActionListener( this );
      this.fldAddr.removeActionListener( this );
      this.rbInt1.removeActionListener( this );
      this.rbInt2.removeActionListener( this );
      this.rbInt3.removeActionListener( this );
      this.rbInt4.removeActionListener( this );
      this.rbBCDec6.removeActionListener( this );
      this.rbByteArray.removeActionListener( this );
      this.rbPointer.removeActionListener( this );
      this.rbLE.removeActionListener( this );
      this.rbBE.removeActionListener( this );
      this.btnOK.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( (e.getSource() == this) && (this.fldAddr != null) ) {
      this.fldAddr.requestFocus();
    }
  }


	/* --- Konstruktor --- */

  private VarDataDlg(
		DebugFrm debugFrm,
		VarData  varData,
		String   name,
		int      addr,
		int      size )
  {
    super(
	debugFrm,
	varData != null ? "Variable bearbeiten" : "Variable anlegen" );

    this.debugFrm       = debugFrm;
    this.oldVarData     = varData;
    this.appliedVarData = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    add( GUIFactory.createLabel( "Adresse (hex):" ), gbc );
    gbc.gridy++;
    add( GUIFactory.createLabel( "Name (optional):" ), gbc );
    gbc.anchor = GridBagConstraints.NORTHEAST;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Typ:" ), gbc );

    this.docAddr = new HexDocument( 4, "Adresse" );
    this.fldAddr = GUIFactory.createTextField( this.docAddr, 0 );
    gbc.anchor   = GridBagConstraints.WEST;
    gbc.weightx  = 1.0;
    gbc.fill     = GridBagConstraints.HORIZONTAL;
    gbc.gridy    = 0;
    gbc.gridx++;
    add( this.fldAddr, gbc );

    this.docName = new LabelDocument();
    this.fldName = GUIFactory.createTextField( this.docName, 0 );
    gbc.gridy++;
    add( this.fldName, gbc );

    JPanel panelType = GUIFactory.createPanel( new GridBagLayout() );
    gbc.weightx      = 0.0;
    gbc.fill         = GridBagConstraints.NONE;
    gbc.gridy++;
    add( panelType, gbc );

    GridBagConstraints gbcType = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 0, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpType = new ButtonGroup();

    this.rbInt1 = GUIFactory.createRadioButton( "1 Byte Integer" );
    grpType.add( this.rbInt1 );
    panelType.add( this.rbInt1, gbcType );

    this.rbInt2 = GUIFactory.createRadioButton( "2 Byte Integer" );
    grpType.add( this.rbInt2 );
    gbcType.gridy++;
    panelType.add( this.rbInt2, gbcType );

    this.rbInt3 = GUIFactory.createRadioButton( "3 Byte Integer" );
    grpType.add( this.rbInt3 );
    gbcType.gridy++;
    panelType.add( this.rbInt3, gbcType );

    this.rbInt4 = GUIFactory.createRadioButton( "4 Byte Integer" );
    grpType.add( this.rbInt4 );
    gbcType.gridy++;
    panelType.add( this.rbInt4, gbcType );

    this.rbBCDec6 = GUIFactory.createRadioButton( "Decimal (6 Byte BCD)" );
    grpType.add( this.rbBCDec6 );
    gbcType.gridy++;
    panelType.add( this.rbBCDec6, gbcType );

    this.rbByteArray = GUIFactory.createRadioButton( "Bytes / Text" );
    grpType.add( this.rbByteArray );
    gbcType.gridy++;
    panelType.add( this.rbByteArray, gbcType );

    this.labelSize      = GUIFactory.createLabel( "Anzahl Bytes:" );
    gbcType.insets.left = 50;
    gbcType.gridwidth   = 1;
    gbcType.gridy++;
    panelType.add( this.labelSize, gbcType );

    this.spinnerSize = GUIFactory.createSpinner(
				new SpinnerNumberModel( 16, 1, 256, 1 ) );
    gbcType.insets.left = 5;
    gbcType.gridwidth   = GridBagConstraints.REMAINDER;
    gbcType.gridx++;
    panelType.add( this.spinnerSize, gbcType );

    this.rbPointer = GUIFactory.createRadioButton(
					"Zeiger auf Bytes / Text" );
    grpType.add( this.rbPointer );
    gbcType.gridx = 0;
    gbcType.gridy++;
    panelType.add( this.rbPointer, gbcType );

    JPanel panelByteOrder = GUIFactory.createPanel();
    panelByteOrder.setLayout(
		new BoxLayout( panelByteOrder, BoxLayout.X_AXIS ) );
    gbcType.insets.top  = 10;
    gbcType.gridx       = 0;
    gbcType.gridy++;
    panelType.add( panelByteOrder, gbcType );
    
    this.labelByteOrder = GUIFactory.createLabel( "Byte Order:" );
    panelByteOrder.add( this.labelByteOrder );
    panelByteOrder.add( Box.createRigidArea( new Dimension( 5, 0 ) ) );

    ButtonGroup grpByteOrder = new ButtonGroup();

    this.rbLE = GUIFactory.createRadioButton(
			"Little Endian (LE, Intel-Format)",
			true );
    grpByteOrder.add( this.rbLE );
    panelByteOrder.add( this.rbLE, gbcType );
    panelByteOrder.add( Box.createRigidArea( new Dimension( 5, 0 ) ) );

    this.rbBE = GUIFactory.createRadioButton( "Big Endian (BE)" );
    grpByteOrder.add( this.rbBE );
    panelByteOrder.add( this.rbBE, gbcType );


    // Schaltflaechen
    JPanel panelBtn   = GUIFactory.createPanel(
					new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.insets.top    = 15;
    gbc.insets.left   = 5;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = GUIFactory.createButtonOK();
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Vorbelegung
    VarData.VarType varType = null;
    if( varData != null ) {
      this.fldName.setText( varData.getName() );
      this.docAddr.setValue( varData.getAddress(), 4 );
      this.spinnerSize.setValue( varData.getSize() );
      varType = varData.getType();
    } else {
      if( name != null ) {
	this.fldName.setText( name );
      }
      if( addr >= 0 ) {
	this.docAddr.setValue( addr, 4 );
      }
      if( size > 0 ) {
	this.spinnerSize.setValue( size );
      }
      varType = VarData.createDefaultType( name, size );
    }
    if( varType != null ) {
      switch( varType ) {
	case INT1:
	  this.rbInt1.setSelected( true );
	  break;
	case INT2_LE:
	  this.rbInt2.setSelected( true );
	  this.rbLE.setSelected( true );
	  break;
	case INT2_BE:
	  this.rbInt2.setSelected( true );
	  this.rbBE.setSelected( true );
	  break;
	case INT3_LE:
	  this.rbInt3.setSelected( true );
	  this.rbLE.setSelected( true );
	  break;
	case INT3_BE:
	  this.rbInt3.setSelected( true );
	  this.rbBE.setSelected( true );
	  break;
	case INT4_LE:
	  this.rbInt4.setSelected( true );
	  this.rbLE.setSelected( true );
	  break;
	case INT4_BE:
	  this.rbInt4.setSelected( true );
	  this.rbBE.setSelected( true );
	  break;
	case BC_DEC6:
	  this.rbBCDec6.setSelected( true );
	  break;
	case BYTE_ARRAY:
	  this.rbByteArray.setSelected( true );
	  break;
	case POINTER:
	  this.rbPointer.setSelected( true );
	  break;
	default:
	  this.rbInt1.setSelected( true );
      }
    } else {
      this.rbInt1.setSelected( true );
    }
    updFieldsEnabled();


    // Listener
    this.fldName.addActionListener( this );
    this.fldAddr.addActionListener( this );
    this.rbInt1.addActionListener( this );
    this.rbInt2.addActionListener( this );
    this.rbInt3.addActionListener( this );
    this.rbInt4.addActionListener( this );
    this.rbBCDec6.addActionListener( this );
    this.rbByteArray.addActionListener( this );
    this.rbPointer.addActionListener( this );
    this.rbLE.addActionListener( this );
    this.rbBE.addActionListener( this );
    this.btnOK.addActionListener( this );
    this.btnCancel.addActionListener( this );


    // Sonstiges
    this.docName.setReverseCase( true );
    pack();
    setParentCentered();
  }


	/* --- private Methoden --- */

  private void doApply()
  {
    try {
      String name = this.docName.getLabel();
      if( name != null ) {
	if( name.isEmpty() ) {
	  name = null;
	}
      }
      if( name != null ) {
	VarData tmpVar = this.debugFrm.getVarByName( name );
	if( (tmpVar != null)
	    && ((this.oldVarData == null) || (tmpVar != this.oldVarData)) )
	{
	  this.debugFrm.selectVar( tmpVar );
	  throw new UserInputException(
			"Eine Variable mit dem Namen existiert bereits." );
	}
      }
      VarData.VarType type = VarData.VarType.BYTE_ARRAY;
      int             size = 0;
      boolean         isLE = this.rbLE.isSelected();
      if( this.rbInt1.isSelected() ) {
	type = VarData.VarType.INT1;
	size = 1;
      } else if( this.rbInt2.isSelected() ) {
	type = isLE ? VarData.VarType.INT2_LE : VarData.VarType.INT2_BE;
	size = 2;
      } else if( this.rbInt3.isSelected() ) {
	type = isLE ? VarData.VarType.INT3_LE : VarData.VarType.INT3_BE;
	size = 3;
      } else if( this.rbInt4.isSelected() ) {
	type = isLE ? VarData.VarType.INT4_LE : VarData.VarType.INT4_BE;
	size = 4;
      } else if( this.rbBCDec6.isSelected() ) {
	type = VarData.VarType.BC_DEC6;
	size = 4;
      } else if( this.rbPointer.isSelected() ) {
	type = VarData.VarType.POINTER;
	size = 2;
      } else {
	Object o = this.spinnerSize.getValue();
	if( o != null ) {
	  if( o instanceof Number ) {
	    int v = ((Number) o).intValue();
	    if( v > 0 ) {
	      size = v;
	    }
	  }
	}
      }
      if( size < 1 ) {
	throw new UserInputException(
		  "Ung\u00FCltige Variablengr\u00F6\u00DFe (Anzahl Bytes)" );
      }
      VarData varData = this.oldVarData;
      if( varData != null ) {
	int addr = this.docAddr.intValue();
	varData.setValues(
		name,
		addr,
		type,
		size,
		varData.getImported()
			&& (addr == this.oldVarData.getAddress())
			&& name.equals( this.oldVarData.getName() ) );
      } else {
	varData = new VarData(
			name,
			this.docAddr.intValue(),
			type,
			size,
			false );
      }
      this.appliedVarData = varData;
      doClose();
    }
    catch( NumberFormatException ex ) {
      showErrorDlg( this, "Ung\u00FCltige Variablenadresse" );
    }
    catch( UserInputException ex ) {
      showErrorDlg( this, ex );
    }
  }


  private void updFieldsEnabled()
  {
    boolean state = this.rbByteArray.isSelected();
    this.labelSize.setEnabled( state );
    this.spinnerSize.setEnabled( state );

    state = this.rbInt2.isSelected()
		|| this.rbInt3.isSelected()
		|| this.rbInt4.isSelected();
    this.labelByteOrder.setEnabled( state );
    this.rbLE.setEnabled( state );
    this.rbBE.setEnabled( state );
  }
}
