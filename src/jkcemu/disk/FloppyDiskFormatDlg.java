/*
 * (c) 2009-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Eingabe des Diskettenformats
 */

package jkcemu.disk;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import jkcemu.base.BasicDlg;


public class FloppyDiskFormatDlg extends BasicDlg implements ChangeListener
{
  public static enum Flag {
			READONLY,
			PHYS_FORMAT,
			SYSTEM_TRACKS,
			BLOCK_SIZE,
			BLOCK_NUM_SIZE,
			DIR_BLOCKS,
			APPLY_READONLY,
			AUTO_REFRESH,
			FORCE_LOWERCASE };

  private static boolean lastApplyReadOnly  = false;
  private static boolean lastForceLowerCase = true;

  private boolean          approved;
  private FloppyDiskFormat selectedFmt;
  private JComboBox        comboFmt;
  private JCheckBox        btnReadOnly;
  private JCheckBox        btnApplyReadOnly;
  private JCheckBox        btnAutoRefresh;
  private JCheckBox        btnForceLowerCase;
  private JSpinner         spinnerSysTracks;
  private JComboBox        comboBlockSize;
  private JRadioButton     btnBlockNum8Bit;
  private JRadioButton     btnBlockNum16Bit;
  private JSpinner         spinnerDirBlocks;
  private JLabel           labelDirSizeUnit;
  private JButton          btnOK;
  private JButton          btnCancel;


  public FloppyDiskFormatDlg(
			Window           owner,
			FloppyDiskFormat preSelFmt,
			Flag...          flags )
  {
    super( owner, "Datei laden" );
    setTitle( "JKCEMU Diskettenformat" );
    this.approved    = false;
    this.selectedFmt = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    // Formatauswahl
    if( containsFlag( flags, Flag.PHYS_FORMAT ) ) {
      this.comboFmt = new JComboBox();
      this.comboFmt.setEditable( false );
      if( preSelFmt == null ) {
	this.comboFmt.addItem( "--- Bitte ausw\u00E4hlen ---" );
      }
      FloppyDiskFormat[] formats = FloppyDiskFormat.getFormats();
      if( formats != null ) {
	boolean state = false;
	for( int i = 0; i < formats.length; i++ ) {
	  FloppyDiskFormat fmt = formats[ i ];
	  if( (fmt.getSides() == 2)
	      && (fmt.getCylinders() == 80)
	      && (fmt.getSectorSize() >= 512) )
	  {
	    this.comboFmt.addItem( fmt );
	    state = true;
	  }
	}
	for( int i = 0; i < formats.length; i++ ) {
	  FloppyDiskFormat fmt = formats[ i ];
	  if( (fmt.getSides() != 2)
	      || (fmt.getCylinders() != 80)
	      || (fmt.getSectorSize() < 512) )
	  {
	    if( state ) {
	      this.comboFmt.addItem( "--- \u00C4ltere Formate ---" );
	      state = false;
	    }
	    this.comboFmt.addItem( fmt );
	  }
	}
      }
      if( preSelFmt != null ) {
	this.comboFmt.setSelectedItem( preSelFmt );
      }
      add( this.comboFmt, gbc );
      gbc.gridy++;
    } else {
      this.comboFmt = null;
    }

    // Systemspuren
    if( containsFlag( flags, Flag.SYSTEM_TRACKS ) ) {
      gbc.anchor    = GridBagConstraints.WEST;
      gbc.gridwidth = 1;
      add( new JLabel( "Systemspuren:" ), gbc );

      this.spinnerSysTracks = new JSpinner(
			new SpinnerNumberModel( 0, 0, 9, 1 ) );
      gbc.gridx++;
      add( this.spinnerSysTracks, gbc );
      gbc.gridx = 0;
      gbc.gridy++;
    } else {
      this.spinnerSysTracks = null;
    }

    // Block-Groesse
    this.comboBlockSize = null;
    if( containsFlag( flags, Flag.BLOCK_SIZE ) ) {
      gbc.anchor    = GridBagConstraints.WEST;
      gbc.gridwidth = 1;
      add( new JLabel( "Block-Gr\u00F6\u00DFe:" ), gbc );

      this.comboBlockSize = new JComboBox();
      this.comboBlockSize.setEditable( false );
      this.comboBlockSize.addItem( new Integer( 1 ) );
      this.comboBlockSize.addItem( new Integer( 2 ) );
      this.comboBlockSize.addItem( new Integer( 4 ) );
      this.comboBlockSize.addItem( new Integer( 8 ) );
      this.comboBlockSize.addItem( new Integer( 16 ) );
      gbc.gridx++;
      add( this.comboBlockSize, gbc );

      gbc.gridx++;
      add( new JLabel( "KByte" ), gbc );
      gbc.gridx = 0;
      gbc.gridy++;
    }

    // Blocknummerngroesse
    this.btnBlockNum8Bit  = null;
    this.btnBlockNum16Bit = null;
    if( containsFlag( flags, Flag.BLOCK_NUM_SIZE ) ) {
      gbc.anchor    = GridBagConstraints.WEST;
      gbc.gridwidth = 1;
      add( new JLabel( "Blocknummern:" ), gbc );

      ButtonGroup grpBlkNumSize = new ButtonGroup();

      this.btnBlockNum8Bit = new JRadioButton( "8 Bit", false );
      grpBlkNumSize.add( this.btnBlockNum8Bit );
      gbc.gridx++;
      add( this.btnBlockNum8Bit, gbc );

      this.btnBlockNum16Bit = new JRadioButton( "16 Bit", false );
      grpBlkNumSize.add( this.btnBlockNum16Bit );
      gbc.gridx++;
      add( this.btnBlockNum16Bit, gbc );
      gbc.gridx = 0;
      gbc.gridy++;
    }

    // Directory-Groesse
    this.spinnerDirBlocks = null;
    this.labelDirSizeUnit = null;
    if( containsFlag( flags, Flag.DIR_BLOCKS ) ) {
      gbc.anchor    = GridBagConstraints.WEST;
      gbc.gridwidth = 1;
      add( new JLabel( "Directory-Gr\u00F6\u00DFe:" ), gbc );

      this.spinnerDirBlocks = new JSpinner(
			new SpinnerNumberModel( 1, 1, 9, 1 ) );
      gbc.gridx++;
      add( this.spinnerDirBlocks, gbc );

      this.labelDirSizeUnit = new JLabel();
      gbc.gridx++;
      add( this.labelDirSizeUnit, gbc );
      gbc.gridx = 0;
      gbc.gridy++;
      updDirSizeUnitLabel();
    }

    // Checkboxen
    java.util.List<JCheckBox> checkBoxes = null;
    this.btnApplyReadOnly                = null;
    this.btnReadOnly                     = null;
    this.btnAutoRefresh                  = null;
    this.btnForceLowerCase               = null;
    if( flags != null ) {
      checkBoxes = new ArrayList<JCheckBox>( 4 );
      for( int i = 0; i < flags.length; i++ ) {
	if( flags[ i ] != null ) {
	  switch( flags[ i ] ) {
	    case APPLY_READONLY:
	      this.btnApplyReadOnly = new JCheckBox(
				"Schreibschutzattribut anwenden",
				lastApplyReadOnly );
	      checkBoxes.add( this.btnApplyReadOnly );
	      break;

	    case READONLY:
	      this.btnReadOnly = new JCheckBox(
				"Schreibschutz (Nur-Lese-Modus)",
				true );
	      checkBoxes.add( this.btnReadOnly );
	      break;

	    case AUTO_REFRESH:
	      this.btnAutoRefresh = new JCheckBox(
				"Automatisch aktualisieren",
				false );
	      checkBoxes.add( this.btnAutoRefresh );
	      break;

	    case FORCE_LOWERCASE:
	      this.btnForceLowerCase = new JCheckBox(
				"Dateinamen klein schreiben",
				lastForceLowerCase );
	      checkBoxes.add( this.btnForceLowerCase );
	      break;
	  }
	}
      }
    }
    if( checkBoxes != null ) {
      int n = checkBoxes.size();
      if( n > 0 ) {
	if( n > 1 ) {
	  gbc.anchor = GridBagConstraints.WEST;
	} else {
	  gbc.anchor = GridBagConstraints.CENTER;
	}
	gbc.insets.bottom = 0;
	gbc.gridwidth     = GridBagConstraints.REMAINDER;
	for( int i = 0; i < n; i++ ) {
	  if( i == 1 ) {
	    gbc.insets.top = 0;
	  }
	  if( i == (n - 1) ) {
	    gbc.insets.bottom = 5;
	  }
	  add( checkBoxes.get( i ), gbc );
	  gbc.gridy++;
	}
      }
    }
    updReadOnlyDependingFlds();

    // Knoepfe
    JPanel panelBtns = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    add( panelBtns, gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    this.btnOK.addKeyListener( this );
    panelBtns.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtns.add( this.btnCancel );


    // Listener
    if( this.labelDirSizeUnit != null ) {
      if( this.spinnerDirBlocks != null ) {
	this.spinnerDirBlocks.addChangeListener( this );
      }
      if( this.comboBlockSize != null ) {
	this.comboBlockSize.addActionListener( this );
      }
    }
    if( this.btnReadOnly != null ) {
      this.btnReadOnly.addActionListener( this );
    }


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


  public boolean getApplyReadOnly()
  {
    return this.btnApplyReadOnly != null ?
				this.btnApplyReadOnly.isSelected()
				: false;
  }


  public boolean getAutoRefresh()
  {
    boolean state = false;
    if( this.btnAutoRefresh != null ) {
      if( this.btnAutoRefresh.isEnabled() ) {
	state = this.btnAutoRefresh.isSelected();
      }
    }
    return state;
  }


  public boolean getBlockNum16Bit()
  {
    return this.btnBlockNum16Bit != null ?
				this.btnBlockNum16Bit.isSelected()
				: true;
  }


  public int getBlockSize()
  {
    int rv = 2048;
    if( this.comboBlockSize != null ) {
      Object o = this.comboBlockSize.getSelectedItem();
      if( o != null ) {
	if( o instanceof Integer ) {
	  rv = ((Integer) o).intValue() * 1024;
	}
      }
    }
    return rv;
  }


  public int getDirBlocks()
  {
    return getIntValue( this.spinnerDirBlocks );
  }


  public boolean getForceLowerCase()
  {
    return this.btnForceLowerCase != null ?
				this.btnForceLowerCase.isSelected()
				: false;
  }


  public FloppyDiskFormat getFormat()
  {
    return this.selectedFmt;
  }


  public boolean getReadOnly()
  {
    return this.btnReadOnly != null ? this.btnReadOnly.isSelected() : false;
  }


  public int getSystemTracks()
  {
    return getIntValue( this.spinnerSysTracks );
  }


  public void setAutoRefresh( boolean state )
  {
    if( this.btnAutoRefresh != null )
      this.btnAutoRefresh.setSelected( state );
  }


  public void setBlockSize( int value )
  {
    if( this.comboBlockSize != null ) {
      this.comboBlockSize.setSelectedItem( new Integer( value / 1024 ) );
      updDirSizeUnitLabel();
    }
  }


  public void setBlockNum16Bit( boolean state )
  {
    if( state ) {
      if( this.btnBlockNum16Bit != null ) {
	this.btnBlockNum16Bit.setSelected( true );
      }
    } else {
      if( this.btnBlockNum8Bit != null ) {
	this.btnBlockNum8Bit.setSelected( true );
      }
    }
  }


  public void setDirBlocks( int value )
  {
    if( this.spinnerDirBlocks != null ) {
      try {
	this.spinnerDirBlocks.setValue( new Integer( value ) );
	updDirSizeUnitLabel();
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  public void setForceLowerCase( boolean state )
  {
    if( this.btnForceLowerCase != null )
      this.btnForceLowerCase.setSelected( state );
  }


  public void setSystemTracks( int value )
  {
    if( this.spinnerSysTracks != null ) {
      try {
	this.spinnerSysTracks.setValue( new Integer( value ) );
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  public boolean wasApproved()
  {
    return this.approved;
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    if( e.getSource() == this.spinnerDirBlocks )
      updDirSizeUnitLabel();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.btnOK ) {
	  rv = true;
	  if( this.comboFmt != null ) {
	    Object value = this.comboFmt.getSelectedItem();
	    if( value != null ) {
	      if( value instanceof FloppyDiskFormat ) {
		this.selectedFmt = (FloppyDiskFormat) value;
		this.approved    = true;
	      }
	    }
	  } else {
	    this.approved = true;
	  }
	  if( this.approved ) {
	    if( this.btnApplyReadOnly != null ) {
	      lastApplyReadOnly = this.btnApplyReadOnly.isSelected();
	    }
	    if( this.btnForceLowerCase != null ) {
	      lastForceLowerCase = this.btnForceLowerCase.isSelected();
	    }
	    doClose();
	  }
	}
	else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	}
	else if( src == this.comboBlockSize ) {
	  rv = true;
	  updDirSizeUnitLabel();
	}
	else if( src == this.btnReadOnly ) {
	  rv = true;
	  updReadOnlyDependingFlds();
	}
      }
    }
    return rv;
  }


  /*
   * Wenn bereits eine Format vorausgewaehlt ist,
   * wird der Focus auf den OK-Knopf gesetzt,
   * damit man einfach Enter druecken kann.
   */
  @Override
  public void windowOpened( WindowEvent e )
  {
    if( (e.getWindow() == this)
	&& (this.comboFmt != null)
	&& (this.btnOK != null) )
    {
      Object o = this.comboFmt.getSelectedItem();
      if( o != null ) {
	if( o instanceof FloppyDiskFormat ) {
	  this.btnOK.requestFocus();
	}
      }
    }
  }


	/* --- private Methoden --- */

  private static boolean containsFlag( Flag[] flags, Flag flag )
  {
    boolean rv = false;
    if( (flags != null) && (flag != null) ) {
      for( int i = 0; i < flags.length; i++ ) {
	if( flags[ i ] != null ) {
	  if( flags[ i ].equals( flag ) ) {
	    rv = true;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  private static int getIntValue( JSpinner spinner  )
  {
    int rv = 0;
    if( spinner != null ) {
      Object value = spinner.getValue();
      if( value != null ) {
	if( value instanceof Number ) {
	  rv = ((Number) value).intValue();
	}
      }
    }
    return rv;
  }


  private void updDirSizeUnitLabel()
  {
    if( this.labelDirSizeUnit != null ) {
      StringBuilder buf = new StringBuilder( 40 );
      buf.append( "Bl\u00F6cke" );

      int nBlocks = getDirBlocks();
      if( nBlocks > 0 ) {
	buf.append( " (" );
	buf.append( (nBlocks * getBlockSize()) / 32 );
	buf.append( " Eintr\u00E4ge)" );
      }
      this.labelDirSizeUnit.setText( buf.toString() );
    }
  }


  private void updReadOnlyDependingFlds()
  {
    if( this.btnReadOnly != null ) {
      boolean state = this.btnReadOnly.isSelected();
      if( this.btnAutoRefresh != null ) {
	this.btnAutoRefresh.setEnabled( state );
      }
      if( this.btnForceLowerCase != null ) {
	this.btnForceLowerCase.setEnabled( !state );
      }
    }
  }
}

