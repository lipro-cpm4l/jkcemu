/*
 * (c) 2015-2016 Jens Mueller
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
import java.lang.*;
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
import javax.swing.table.TableModel;
import jkcemu.base.BaseDlg;
import jkcemu.base.HexDocument;
import jkcemu.base.UserInputException;


public class VarDataDlg extends BaseDlg
{
  private DebugFrm     debugFrm;
  private VarData      varData;
  private VarData      appliedVarData;
  private JTextField   fldName;
  private JTextField   fldAddr;
  private HexDocument  docAddr;
  private JRadioButton btnInt1;
  private JRadioButton btnInt2;
  private JRadioButton btnInt3;
  private JRadioButton btnInt4;
  private JRadioButton btnInt8;
  private JRadioButton btnFloat4;
  private JRadioButton btnFloat8;
  private JRadioButton btnByteArray;
  private JRadioButton btnPointer;
  private JRadioButton btnLE;
  private JRadioButton btnBE;
  private JSpinner     spinnerSize;
  private JLabel       labelSize;
  private JLabel       labelByteOrder;
  private JButton      btnOK;
  private JButton      btnCancel;


  public static VarData showEditVarDlg( DebugFrm debugFrm, VarData varData )
  {
    VarDataDlg dlg = new VarDataDlg( debugFrm, varData );
    dlg.setVisible( true );
    return dlg.appliedVarData;
  }


  public static VarData showNewVarDlg( DebugFrm debugFrm )
  {
    VarDataDlg dlg = new VarDataDlg( debugFrm, null );
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
      if( src == this.fldName ) {
	rv = true;
	this.fldAddr.requestFocus();
      } else if( src == this.fldAddr ) {
	rv = true;
	this.fldAddr.transferFocus();
      } else if( src == this.btnOK ) {
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
  public void windowOpened( WindowEvent e )
  {
    if( (e.getSource() == this) && (this.fldAddr != null) ) {
      this.fldAddr.requestFocus();
    }
  }


	/* --- Konstruktor --- */

  private VarDataDlg( DebugFrm debugFrm, VarData varData )
  {
    super(
	debugFrm,
	varData != null ? "Variable bearbeiten" : "Variable anlegen" );

    this.debugFrm       = debugFrm;
    this.varData        = varData;
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

    add( new JLabel( "Bezeichnung (optional):" ), gbc );
    gbc.gridy++;
    add( new JLabel( "Adresse (hex):" ), gbc );
    gbc.anchor = GridBagConstraints.NORTHEAST;
    gbc.gridy++;
    add( new JLabel( "Typ:" ), gbc );

    this.fldName = new JTextField();
    gbc.anchor   = GridBagConstraints.WEST;
    gbc.weightx  = 1.0;
    gbc.fill     = GridBagConstraints.HORIZONTAL;
    gbc.gridy    = 0;
    gbc.gridx++;
    add( this.fldName, gbc );

    this.docAddr = new HexDocument( 4, "Adresse" );
    this.fldAddr = new JTextField( this.docAddr, "", 0 );
    gbc.gridy++;
    add( this.fldAddr, gbc );

    JPanel panelType = new JPanel( new GridBagLayout() );
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

    this.btnInt1 = new JRadioButton( "1 Byte Integer", true );
    grpType.add( this.btnInt1 );
    panelType.add( this.btnInt1, gbcType );

    this.btnInt2 = new JRadioButton( "2 Byte Integer" );
    grpType.add( this.btnInt2 );
    gbcType.gridy++;
    panelType.add( this.btnInt2, gbcType );

    this.btnInt3 = new JRadioButton( "3 Byte Integer" );
    grpType.add( this.btnInt3 );
    gbcType.gridy++;
    panelType.add( this.btnInt3, gbcType );

    this.btnInt4 = new JRadioButton( "4 Byte Integer" );
    grpType.add( this.btnInt4 );
    gbcType.gridy++;
    panelType.add( this.btnInt4, gbcType );

    this.btnInt8 = new JRadioButton( "8 Byte Integer" );
    grpType.add( this.btnInt8 );
    gbcType.gridy++;
    panelType.add( this.btnInt8, gbcType );

    this.btnFloat4 = new JRadioButton( "4 Byte Float (IEEE 754)" );
    grpType.add( this.btnFloat4 );
    gbcType.gridy++;
    panelType.add( this.btnFloat4, gbcType );

    this.btnFloat8 = new JRadioButton( "8 Byte Float (IEEE 754)" );
    grpType.add( this.btnFloat8 );
    gbcType.gridy++;
    panelType.add( this.btnFloat8, gbcType );

    this.btnByteArray = new JRadioButton( "Bytes / Text" );
    grpType.add( this.btnByteArray );
    gbcType.gridy++;
    panelType.add( this.btnByteArray, gbcType );

    this.labelSize      = new JLabel( "Anzahl Bytes:" );
    gbcType.insets.left = 50;
    gbcType.gridwidth   = 1;
    gbcType.gridy++;
    panelType.add( this.labelSize, gbcType );

    this.spinnerSize = new JSpinner(
				new SpinnerNumberModel( 16, 1, 256, 1 ) );
    gbcType.insets.left = 5;
    gbcType.gridwidth   = GridBagConstraints.REMAINDER;
    gbcType.gridx++;
    panelType.add( this.spinnerSize, gbcType );

    this.btnPointer = new JRadioButton( "Zeiger auf Bytes / Text" );
    grpType.add( this.btnPointer );
    gbcType.gridx = 0;
    gbcType.gridy++;
    panelType.add( this.btnPointer, gbcType );

    JPanel panelByteOrder = new JPanel();
    panelByteOrder.setLayout(
		new BoxLayout( panelByteOrder, BoxLayout.X_AXIS ) );
    gbcType.insets.top  = 10;
    gbcType.gridx       = 0;
    gbcType.gridy++;
    panelType.add( panelByteOrder, gbcType );
    
    this.labelByteOrder = new JLabel( "Byte Order:" );
    panelByteOrder.add( this.labelByteOrder );
    panelByteOrder.add( Box.createRigidArea( new Dimension( 5, 0 ) ) );

    ButtonGroup grpByteOrder = new ButtonGroup();

    this.btnLE = new JRadioButton( "Little Endian (LE, Intel-Format)", true );
    grpByteOrder.add( this.btnLE );
    panelByteOrder.add( this.btnLE, gbcType );
    panelByteOrder.add( Box.createRigidArea( new Dimension( 5, 0 ) ) );

    this.btnBE = new JRadioButton( "Big Endian (BE)" );
    grpByteOrder.add( this.btnBE );
    panelByteOrder.add( this.btnBE, gbcType );


    // Schaltflaechen
    JPanel panelBtn   = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.insets.top    = 15;
    gbc.insets.left   = 5;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );


    // Vorbelegung
    if( varData != null ) {
      this.fldName.setText( varData.getName() );
      this.docAddr.setValue( varData.getAddress(), 4 );
      switch( varData.getType() ) {
	case INT1:
	  this.btnInt1.setSelected( true );
	  break;
	case INT2LE:
	  this.btnInt2.setSelected( true );
	  this.btnLE.setSelected( true );
	  break;
	case INT2BE:
	  this.btnInt2.setSelected( true );
	  this.btnBE.setSelected( true );
	  break;
	case INT3LE:
	  this.btnInt3.setSelected( true );
	  this.btnLE.setSelected( true );
	  break;
	case INT3BE:
	  this.btnInt3.setSelected( true );
	  this.btnBE.setSelected( true );
	  break;
	case INT4LE:
	  this.btnInt4.setSelected( true );
	  this.btnLE.setSelected( true );
	  break;
	case INT4BE:
	  this.btnInt4.setSelected( true );
	  this.btnBE.setSelected( true );
	  break;
	case INT8LE:
	  this.btnInt8.setSelected( true );
	  this.btnLE.setSelected( true );
	  break;
	case INT8BE:
	  this.btnInt8.setSelected( true );
	  this.btnBE.setSelected( true );
	  break;
	case FLOAT4LE:
	  this.btnFloat4.setSelected( true );
	  this.btnLE.setSelected( true );
	  break;
	case FLOAT4BE:
	  this.btnFloat4.setSelected( true );
	  this.btnBE.setSelected( true );
	  break;
	case FLOAT8LE:
	  this.btnFloat8.setSelected( true );
	  this.btnLE.setSelected( true );
	  break;
	case FLOAT8BE:
	  this.btnFloat8.setSelected( true );
	  this.btnBE.setSelected( true );
	  break;
	case BYTE_ARRAY:
	  this.btnByteArray.setSelected( true );
	  break;
	case POINTER:
	  this.btnPointer.setSelected( true );
	  break;
      }
      this.spinnerSize.setValue( varData.getSize() );
    }
    updFieldsEnabled();


    // Listener
    this.fldName.addActionListener( this );
    this.fldAddr.addActionListener( this );
    this.btnInt1.addActionListener( this );
    this.btnInt2.addActionListener( this );
    this.btnInt3.addActionListener( this );
    this.btnInt4.addActionListener( this );
    this.btnInt8.addActionListener( this );
    this.btnFloat4.addActionListener( this );
    this.btnFloat8.addActionListener( this );
    this.btnByteArray.addActionListener( this );
    this.btnPointer.addActionListener( this );
    this.btnLE.addActionListener( this );
    this.btnBE.addActionListener( this );
    this.btnOK.addActionListener( this );
    this.btnCancel.addActionListener( this );


    // Sonstiges
    pack();
    setParentCentered();
  }


	/* --- private Methoden --- */

  private void doApply()
  {
    try {
      String name = this.fldName.getText();
      if( name != null ) {
	if( name.isEmpty() ) {
	  name = null;
	}
      }
      if( name != null ) {
	VarData tmpVar = this.debugFrm.getVarByName( name );
	if( (tmpVar != null)
	    && ((this.varData == null) || (tmpVar != this.varData)) )
	{
	  this.debugFrm.selectVar( tmpVar );
	  throw new UserInputException(
			"Eine Variable mit dem Namen existiert bereits." );
	}
      }
      VarData.VarType type = VarData.VarType.BYTE_ARRAY;
      int             size = 0;
      boolean         isLE = this.btnLE.isSelected();
      if( this.btnInt1.isSelected() ) {
	type = VarData.VarType.INT1;
	size = 1;
      } else if( this.btnInt2.isSelected() ) {
	type = isLE ? VarData.VarType.INT2LE : VarData.VarType.INT2BE;
	size = 2;
      } else if( this.btnInt3.isSelected() ) {
	type = isLE ? VarData.VarType.INT3LE : VarData.VarType.INT3BE;
	size = 3;
      } else if( this.btnInt4.isSelected() ) {
	type = isLE ? VarData.VarType.INT4LE : VarData.VarType.INT4BE;
	size = 4;
      } else if( this.btnInt8.isSelected() ) {
	type = isLE ? VarData.VarType.INT8LE : VarData.VarType.INT8BE;
	size = 8;
      } else if( this.btnFloat4.isSelected() ) {
	type = isLE ? VarData.VarType.FLOAT4LE : VarData.VarType.FLOAT4BE;
	size = 4;
      } else if( this.btnFloat8.isSelected() ) {
	type = isLE ? VarData.VarType.FLOAT8LE : VarData.VarType.FLOAT8BE;
	size = 8;
      } else if( this.btnPointer.isSelected() ) {
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
      VarData varDate = this.varData;
      if( varData != null ) {
	int addr = this.docAddr.intValue();
	varData.setValues(
		name,
		addr,
		type,
		size,
		varData.wasImported()
			&& (addr == this.varData.getAddress())
			&& name.equalsIgnoreCase( this.varData.getName() ) );
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
    boolean state = this.btnByteArray.isSelected();
    this.labelSize.setEnabled( state );
    this.spinnerSize.setEnabled( state );

    state = this.btnInt2.isSelected()
		|| this.btnInt3.isSelected()
		|| this.btnInt4.isSelected()
		|| this.btnInt8.isSelected()
		|| this.btnFloat4.isSelected()
		|| this.btnFloat8.isSelected();
    this.labelByteOrder.setEnabled( state );
    this.btnLE.setEnabled( state );
    this.btnBE.setEnabled( state );
  }
}
