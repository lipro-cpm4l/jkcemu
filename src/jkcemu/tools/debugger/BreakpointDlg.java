/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Anlegen eines Haltepunktes
 */

package jkcemu.tools.debugger;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.*;
import z80emu.Z80InterruptSource;


public class BreakpointDlg extends BasicDlg
{
  public static enum BreakpointType { PC, MEMORY, IO, INTERRUPT };

  private BreakpointType     breakpointType;
  private AbstractBreakpoint breakpoint;
  private AbstractBreakpoint approvedBreakpoint;
  private HexDocument        docBegAddr;
  private HexDocument        docEndAddr;
  private HexDocument        docMask;
  private HexDocument        docValue;
  private JComboBox          comboIntSource;
  private JCheckBox          btnOnRead;
  private JCheckBox          btnOnWrite;
  private JTextField         fldBegAddr;
  private JTextField         fldEndAddr;
  private JTextField         fldValue;
  private JTextField         fldMask;
  private JButton            btnOK;
  private JButton            btnCancel;


  public BreakpointDlg(
		Window               owner,
		BreakpointType       breakpointType,
		AbstractBreakpoint   breakpoint,
		Z80InterruptSource[] iSources )
  {
    super(
	owner,
	breakpoint != null ? "Haltepunkt bearbeiten" : "Neuer Haltepunkt" );
    this.breakpointType     = breakpointType;
    this.breakpoint         = breakpoint;
    this.approvedBreakpoint = null;
    this.docBegAddr         = null;
    this.docEndAddr         = null;
    this.docValue           = null;
    this.docMask            = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 1.0,
						GridBagConstraints.WEST,
						GridBagConstraints.BOTH,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    switch( this.breakpointType ) {
      case PC:
	add(
	  createAddrPanel( "Adresse:" ),
	  gbc );
	break;

      case MEMORY:
	add(
	  createAddrPanel(
		"Adresse / Anfangsadresse:",
		"Endadresse (optional):",
		"Zus\u00E4tzlich k\u00F6nnen Sie einen Wert angeben.",
		"Es wird dann nur angehalten, wenn neben der Adresse",
		"und der Art des Zugriffs dieser Wert entsprechend"
			+ " der Maske",
		"mit dem gelesenen bzw. zu schreibenden"
			+ " \u00FCbereinstimmt." ),
	  gbc );
	break;

      case IO:
	add(
	  createAddrPanel(
		"E/A-Adresse (8 oder 16 Bit):",
		"Bis E/A-Adresse (optional):",
		"Zus\u00E4tzlich k\u00F6nnen Sie einen Wert angeben.",
		"Bei Ausgabebefehlen wird dann nur angehalten,",
		"wenn auch dieser Wert entsprechend der Maske",
		" mit dem auszugebenden \u00FCbereinstimmt",
		"(nicht relevant bei Lesebefehlen)." ),
	  gbc );
	break;

      case INTERRUPT:
	this.comboIntSource = new JComboBox();
	this.comboIntSource.setEditable( false );
	this.comboIntSource.addKeyListener( this );
	setInterruptSources( iSources );
	add( this.comboIntSource, gbc );
	break;
    }


    // Knoepfe
    JPanel panelBtn   = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.insets.bottom = 5;
    gbc.weightx       = 0.0;
    gbc.weighty       = 0.0;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );


    // Vorbelegungen
    if( breakpoint != null ) {
      if( breakpoint instanceof PCBreakpoint ) {
	updFields(
		null,
		false,
		((PCBreakpoint) breakpoint).getAddress(),
		-1,
		-1,
		-1  );
      } else if( breakpoint instanceof MemBreakpoint ) {
	updFields(
		((MemBreakpoint) breakpoint).getAccessMode(),
		false,
		((MemBreakpoint) breakpoint).getBegAddress(),
		((MemBreakpoint) breakpoint).getEndAddress(),
		((MemBreakpoint) breakpoint).getValue(),
		((MemBreakpoint) breakpoint).getMask() );
      } else if( breakpoint instanceof IOBreakpoint ) {
	updFields(
		((IOBreakpoint) breakpoint).getAccessMode(),
		((IOBreakpoint) breakpoint).is8Bit(),
		((IOBreakpoint) breakpoint).getBegPort(),
		((IOBreakpoint) breakpoint).getEndPort(),
		((IOBreakpoint) breakpoint).getValue(),
		((IOBreakpoint) breakpoint).getMask() );
      } else if( breakpoint instanceof InterruptBreakpoint ) {
	if( this.comboIntSource != null ) {
	  this.comboIntSource.setSelectedItem(
		((InterruptBreakpoint) breakpoint).getInterruptSource() );
	}
      }
    }


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );
  }


  public AbstractBreakpoint getApprovedBreakpoint()
  {
    return this.approvedBreakpoint;
  }


  public void setInterruptSources( Z80InterruptSource[] iSources )
  {
    if( this.comboIntSource != null ) {
      if( iSources != null ) {
	if( iSources.length == 0 ) {
	  iSources = null;
	}
      }
      if( iSources != null ) {
	this.comboIntSource.setModel( new DefaultComboBoxModel( iSources ) );
      } else {
	String[] a = { "--- Keine Interrupt-Quelle vorhanden---" };
	this.comboIntSource.setModel( new DefaultComboBoxModel( a ) );
      }
      pack();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.fldBegAddr ) {
	  rv = true;
	  if( this.fldEndAddr != null ) {
	    this.fldEndAddr.requestFocus();
	  } else {
	    doApprove();
	  }
	}
	else if( src == this.fldEndAddr ) {
	  rv = true;
	  if( this.fldValue != null ) {
	    this.fldValue.requestFocus();
	  } else {
	    doApprove();
	  }
	}
	else if( src == this.fldValue ) {
	  rv = true;
	  this.fldValue.transferFocus();
	}
	else if( (src == this.fldMask)
		 || (src == this.comboIntSource)
		 || (src == this.btnOK) )
	{
	  rv = true;
	  doApprove();
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	}
	else if( ((src == this.btnOnRead) || (src == this.btnOnWrite))
		 && (this.btnOnRead != null)
		 && (this.btnOnWrite != null) )
	{
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
      }
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( this.fldBegAddr != null ) {
      this.fldBegAddr.requestFocus();
    }
    else if( this.comboIntSource != null ) {
      this.comboIntSource.requestFocus();
    }
  }


	/* --- private Konstruktoren und Methoden --- */

  private JPanel createAddrPanel( String... labelTexts )
  {
    JPanel panel = new JPanel( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.EAST,
						GridBagConstraints.NONE,
						new Insets( 5, 0, 5, 0 ),
						0, 0 );

    if( labelTexts != null ) {
      if( labelTexts.length > 0 ) {
	panel.add( new JLabel( labelTexts[ 0 ] ), gbc );
	this.fldBegAddr = new JTextField( 4 );
	this.docBegAddr = new HexDocument( this.fldBegAddr, 4 );
	this.fldBegAddr.addActionListener( this );
	gbc.anchor      = GridBagConstraints.WEST;
	gbc.fill        = GridBagConstraints.HORIZONTAL;
	gbc.weightx     = 1.0;
	gbc.insets.left = 5;
	if( labelTexts.length > 1 ) {
	  gbc.gridwidth = 2;
	}
	gbc.gridx++;
	panel.add( this.fldBegAddr, gbc );
      }
      if( labelTexts.length > 1 ) {
	gbc.anchor      = GridBagConstraints.EAST;
	gbc.fill        = GridBagConstraints.NONE;
	gbc.weightx     = 0.0;
	gbc.insets.top  = 0;
	gbc.insets.left = 0;
	gbc.gridwidth   = 1;
	gbc.gridx       = 0;
	gbc.gridy++;
	panel.add( new JLabel( labelTexts[ 1 ] ), gbc );
	this.fldEndAddr = new JTextField( 4 );
	this.docEndAddr = new HexDocument( this.fldEndAddr, 4 );
	this.fldEndAddr.addActionListener( this );
	gbc.anchor      = GridBagConstraints.WEST;
	gbc.fill        = GridBagConstraints.HORIZONTAL;
	gbc.weightx     = 1.0;
	gbc.insets.left = 5;
	if( labelTexts.length > 1 ) {
	  gbc.gridwidth = 2;
	}
	gbc.gridx++;
	panel.add( this.fldEndAddr, gbc );
      }
      if( labelTexts.length > 2 ) {
	gbc.anchor      = GridBagConstraints.EAST;
	gbc.fill        = GridBagConstraints.NONE;
	gbc.weightx     = 0.0;
	gbc.insets.top  = 5;
	gbc.insets.left = 0;
	gbc.gridwidth   = 1;
	gbc.gridx       = 0;
	gbc.gridy++;
	panel.add( new JLabel( "Anhalten beim:" ), gbc );

	this.btnOnRead  = new JCheckBox( "Lesen", true );
	gbc.anchor      = GridBagConstraints.WEST;
	gbc.insets.left = 5;
	gbc.gridwidth   = 1;
	gbc.gridx++;
	panel.add( this.btnOnRead, gbc );

	this.btnOnWrite = new JCheckBox( "Schreiben", true );
	gbc.gridx++;
	panel.add( this.btnOnWrite, gbc );

	gbc.insets.top    = 10;
	gbc.insets.left   = 0;
	gbc.insets.bottom = 0;
	gbc.gridx         = 0;
	gbc.gridwidth     = 3;
	for( int i = 2; i < labelTexts.length; i++ ) {
	  if( i == (labelTexts.length - 1) ) {
	    gbc.insets.bottom = 5;
	  }
	  gbc.gridy++;
	  panel.add( new JLabel( labelTexts[ i ] ), gbc );
	  gbc.insets.top = 0;
	}

	gbc.anchor    = GridBagConstraints.EAST;
	gbc.gridwidth = 1;
	gbc.gridy++;
	panel.add( new JLabel( "Wert (hexadezimal):" ), gbc );

	this.fldValue   = new JTextField( 2 );
	this.docValue   = new HexDocument( this.fldValue, 2 );
	gbc.anchor      = GridBagConstraints.WEST;
	gbc.fill        = GridBagConstraints.HORIZONTAL;
	gbc.weightx     = 1.0;
	gbc.insets.top  = 0;
	gbc.insets.left = 5;
	gbc.gridwidth   = 2;
	gbc.gridx++;
	panel.add( this.fldValue, gbc );

	gbc.anchor    = GridBagConstraints.EAST;
	gbc.fill      = GridBagConstraints.NONE;
	gbc.weightx   = 0.0;
	gbc.gridwidth = 1;
	gbc.gridx     = 0;
	gbc.gridy++;
	panel.add( new JLabel( "Maske (hexadezimal):" ), gbc );

	this.fldMask    = new JTextField( 2 );
	this.docMask    = new HexDocument( this.fldMask, 2 );
	gbc.anchor      = GridBagConstraints.WEST;
	gbc.fill        = GridBagConstraints.HORIZONTAL;
	gbc.weightx     = 1.0;
	gbc.insets.left = 5;
	gbc.gridwidth   = 2;
	gbc.gridx++;
	panel.add( this.fldMask, gbc );
	this.docMask.setValue( 0xFF, 2 );

	this.btnOnRead.addActionListener( this );
	this.btnOnWrite.addActionListener( this );
	this.fldValue.addActionListener( this );
	this.fldMask.addActionListener( this );
      }
    }
    return panel;
  }


  private void doApprove()
  {
    if( (this.docBegAddr != null) && (this.fldBegAddr != null) ) {
      String fldName = "Adresse";
      try {
	Integer begAddr = this.docBegAddr.getInteger();
	if( begAddr != null ) {
	  AbstractBreakpoint.AccessMode accessMode = null;
	  int                           endAddr    = -1;
	  int                           value      = -1;
	  int                           mask       = 0xFF;
	  boolean                       is8Bit     = false;
	  if( this.docEndAddr != null ) {
	    fldName   = "EndAresse";
	    Integer v = this.docEndAddr.getInteger();
	    if( v != null ) {
	      endAddr = v.intValue() & 0xFFFF;
	    }
	  }
	  if( (this.btnOnRead != null) && (this.btnOnWrite != null) ) {
	    boolean r = this.btnOnRead.isSelected();
	    boolean w = this.btnOnWrite.isSelected();
	    if( r && w ) {
	      accessMode = AbstractBreakpoint.AccessMode.READ_WRITE;
	    } else if( r && !w ) {
	      accessMode = AbstractBreakpoint.AccessMode.READ;
	    } else if( !r && w ) {
	      accessMode = AbstractBreakpoint.AccessMode.WRITE;
	    }
	  }
	  if( this.docValue != null ) {
	    fldName   = "Wert";
	    Integer v = this.docValue.getInteger();
	    if( v != null ) {
	      value = v.intValue() & 0xFF;
	    }
	  }
	  if( this.docMask != null ) {
	    fldName   = "Maske";
	    Integer v = this.docMask.getInteger();
	    if( v != null ) {
	      mask = v.intValue() & 0xFF;
	    }
	  }
	  if( this.breakpointType == BreakpointType.PC ) {
	    this.approvedBreakpoint = new PCBreakpoint( begAddr.intValue() );
	  }
	  else if( this.breakpointType == BreakpointType.MEMORY ) {
	    this.approvedBreakpoint = new MemBreakpoint(
						accessMode,
						begAddr.intValue(),
						endAddr,
						value,
						mask );
	  }
	  else if( this.breakpointType == BreakpointType.IO ) {
	    String text = this.fldBegAddr.getText();
	    if( text != null ) {
	      if( text.trim().length() < 3 ) {
		is8Bit = true;
	      }
	    }
	    this.approvedBreakpoint = new IOBreakpoint(
						accessMode,
						is8Bit,
						begAddr.intValue(),
						endAddr,
						value,
						mask );
	  }
	} else {
	  BasicDlg.showErrorDlg( this, "Bitte Adresse eingeben!" );
	}
      }
      catch( NumberFormatException ex ) {
	BasicDlg.showErrorDlg(
			this,
			fldName + " hat ung\u00FCltiges Format." );
      }
    }
    else if( this.comboIntSource != null ) {
      Object o = this.comboIntSource.getSelectedItem();
      if( o != null ) {
	if( o instanceof Z80InterruptSource ) {
	  this.approvedBreakpoint = new InterruptBreakpoint(
						(Z80InterruptSource) o );
	}
      }
    }
    if( this.approvedBreakpoint != null ) {
      doClose();
    }
  }


  private void updFields(
			AbstractBreakpoint.AccessMode accessMode,
			boolean                       is8Bit,
			int                           begAddr,
			int                           endAddr,
			int                           value,
			int                           mask )
  {
    if( this.docBegAddr != null ) {
      this.docBegAddr.setValue( begAddr, is8Bit ? 2 : 4 );
    }
    if( this.docEndAddr != null ) {
      this.docEndAddr.setValue( endAddr, is8Bit ? 2 : 4 );
    }
    if( (this.btnOnRead != null) && (accessMode != null) ) {
      this.btnOnRead.setSelected(
		(accessMode == AbstractBreakpoint.AccessMode.READ)
		|| (accessMode == AbstractBreakpoint.AccessMode.READ_WRITE) );
    }
    if( (this.btnOnWrite != null) && (accessMode != null) ) {
      this.btnOnWrite.setSelected(
		(accessMode == AbstractBreakpoint.AccessMode.WRITE)
		|| (accessMode == AbstractBreakpoint.AccessMode.READ_WRITE) );
    }
    if( (this.docValue != null) && (value >= 0) ) {
      this.docValue.setValue( value, 2 );
    }
    if( this.docMask != null ) {
      this.docMask.setValue( mask & 0xFF, 2 );
    }
  }
}
