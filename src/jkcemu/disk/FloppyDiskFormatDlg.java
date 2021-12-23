/*
 * (c) 2009-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Eingabe des Diskettenformats
 */

package jkcemu.disk;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JRadioButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.base.BaseDlg;
import jkcemu.base.GUIFactory;


public class FloppyDiskFormatDlg extends BaseDlg implements ChangeListener
{
  public static enum Flag {
			READONLY,
			FULL_FORMAT,
			PHYS_FORMAT,
			SYSTEM_TRACKS,
			BLOCK_SIZE,
			BLOCK_NUM_SIZE,
			DIR_BLOCKS,
			APPLY_READONLY,
			AUTO_REFRESH,
			FORCE_LOWERCASE };

  private static final String TEXT_CHOOSE      = "Bitte ausw\u00E4hlen!";
  private static final Color  COLOR_EMPHASIZED = Color.RED;
  private static final Color  COLOR_RECOGNIZED = new Color( 0xFF007F00 );

  private static boolean lastApplyReadOnly  = false;
  private static boolean lastForceLowerCase = true;

  private boolean                   approved;
  private boolean                   notified;
  private Boolean                   recognizedBlockNum16Bit;
  private Integer                   recognizedSysTracks;
  private FloppyDiskFormat          selectedFmt;
  private FloppyDiskFormatSelectFld fmtSelectFld;
  private JComboBox<Object>         comboFmt;
  private JCheckBox                 cbReadOnly;
  private JCheckBox                 cbApplyReadOnly;
  private JCheckBox                 cbAutoRefresh;
  private JCheckBox                 cbForceLowerCase;
  private JSpinner                  spinnerSysTracks;
  private JComboBox<Integer>        comboBlockSize;
  private JRadioButton              rbBlockNum8Bit;
  private JRadioButton              rbBlockNum16Bit;
  private JSpinner                  spinnerDirBlocks;
  private JLabel                    labelDirSizeUnit;
  private JLabel                    infoSysTracks;
  private JLabel                    infoBlockSize;
  private JLabel                    infoBlockNumFmt;
  private JButton                   btnOK;
  private JButton                   btnCancel;


  public FloppyDiskFormatDlg(
			Window           owner,
			boolean          withHDformats,
			FloppyDiskFormat preSelFmt,
			Flag...          flags )
  {
    super( owner, "Datei laden" );
    setTitle( "JKCEMU Diskettenformat" );
    this.approved                = false;
    this.notified                = false;
    this.recognizedBlockNum16Bit = null;
    this.recognizedSysTracks     = null;
    this.selectedFmt             = null;


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

    // vollstaendiges Format
    if( containsFlag( flags, Flag.FULL_FORMAT ) ) {
      this.fmtSelectFld = new FloppyDiskFormatSelectFld( false );
      this.fmtSelectFld.setFormat( preSelFmt );
      add( this.fmtSelectFld, gbc );
      gbc.gridy++;
    } else {
      this.fmtSelectFld = null;
    }

    // Formatauswahl
    if( containsFlag( flags, Flag.PHYS_FORMAT ) ) {
      this.comboFmt = GUIFactory.createComboBox();
      this.comboFmt.setEditable( false );
      if( preSelFmt == null ) {
	this.comboFmt.addItem( "--- Bitte ausw\u00E4hlen ---" );
      }
      FloppyDiskFormat[] formats = FloppyDiskFormat.getFormats();
      if( formats != null ) {
	boolean state = false;
	for( int i = 0; i < formats.length; i++ ) {
	  FloppyDiskFormat fmt = formats[ i ];
	  if( (withHDformats || !fmt.isHD())
	      && (fmt.getSides() == 2)
	      && (fmt.getCylinders() == 80)
	      && (fmt.getSectorSize() >= 512) )
	  {
	    this.comboFmt.addItem( fmt );
	    state = true;
	  }
	}
	for( int i = 0; i < formats.length; i++ ) {
	  FloppyDiskFormat fmt = formats[ i ];
	  if( (withHDformats || !fmt.isHD())
	      && ((fmt.getSides() != 2)
			|| (fmt.getCylinders() != 80)
			|| (fmt.getSectorSize() < 512)) )
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
      add( GUIFactory.createLabel( "Systemspuren:" ), gbc );

      this.spinnerSysTracks = GUIFactory.createSpinner(
			new SpinnerNumberModel( 0, 0, 9, 1 ) );
      gbc.gridx++;
      add( this.spinnerSysTracks, gbc );

      this.infoSysTracks = GUIFactory.createLabel( TEXT_CHOOSE );
      this.infoSysTracks.setForeground( COLOR_EMPHASIZED );
      gbc.gridx += 2;
      add( this.infoSysTracks, gbc );
      gbc.gridx = 0;
      gbc.gridy++;
    } else {
      this.spinnerSysTracks = null;
      this.infoSysTracks    = null;
    }

    // Blockgroesse
    this.comboBlockSize = null;
    if( containsFlag( flags, Flag.BLOCK_SIZE ) ) {
      gbc.anchor    = GridBagConstraints.WEST;
      gbc.gridwidth = 1;
      add( GUIFactory.createLabel( "Blockgr\u00F6\u00DFe:" ), gbc );

      this.comboBlockSize = GUIFactory.createComboBox();
      this.comboBlockSize.setEditable( false );
      this.comboBlockSize.addItem( 1 );
      this.comboBlockSize.addItem( 2 );
      this.comboBlockSize.addItem( 4 );
      this.comboBlockSize.addItem( 8 );
      this.comboBlockSize.addItem( 16 );
      this.comboBlockSize.setSelectedItem(
				DiskUtil.DEFAULT_BLOCK_SIZE / 1024 );
      gbc.gridx++;
      add( this.comboBlockSize, gbc );

      gbc.gridx++;
      add( GUIFactory.createLabel( "KByte" ), gbc );

      this.infoBlockSize = GUIFactory.createLabel( TEXT_CHOOSE );
      this.infoBlockSize.setForeground( COLOR_EMPHASIZED );
      gbc.gridx++;
      add( this.infoBlockSize, gbc );
      gbc.gridx = 0;
      gbc.gridy++;
    }

    // Blocknummerngroesse
    this.rbBlockNum8Bit  = null;
    this.rbBlockNum16Bit = null;
    if( containsFlag( flags, Flag.BLOCK_NUM_SIZE ) ) {
      gbc.anchor    = GridBagConstraints.WEST;
      gbc.gridwidth = 1;
      add( GUIFactory.createLabel( "Blocknummern:" ), gbc );

      ButtonGroup grpBlkNumSize = new ButtonGroup();

      this.rbBlockNum8Bit = GUIFactory.createRadioButton( "8 Bit" );
      grpBlkNumSize.add( this.rbBlockNum8Bit );
      gbc.gridx++;
      add( this.rbBlockNum8Bit, gbc );

      this.rbBlockNum16Bit = GUIFactory.createRadioButton( "16 Bit", true );
      grpBlkNumSize.add( this.rbBlockNum16Bit );
      gbc.gridx++;
      add( this.rbBlockNum16Bit, gbc );

      this.infoBlockNumFmt = GUIFactory.createLabel( TEXT_CHOOSE );
      this.infoBlockNumFmt.setForeground( COLOR_EMPHASIZED );
      gbc.gridx++;
      add( this.infoBlockNumFmt, gbc );
      gbc.gridx = 0;
      gbc.gridy++;
    }

    // Directory-Groesse
    this.spinnerDirBlocks = null;
    this.labelDirSizeUnit = null;
    if( containsFlag( flags, Flag.DIR_BLOCKS ) ) {
      gbc.anchor    = GridBagConstraints.WEST;
      gbc.gridwidth = 1;
      add( GUIFactory.createLabel( "Directory-Gr\u00F6\u00DFe:" ), gbc );

      this.spinnerDirBlocks = GUIFactory.createSpinner(
			new SpinnerNumberModel( 1, 1, 9, 1 ) );
      gbc.gridx++;
      add( this.spinnerDirBlocks, gbc );

      this.labelDirSizeUnit = GUIFactory.createLabel();
      gbc.gridx++;
      add( this.labelDirSizeUnit, gbc );
      gbc.gridx = 0;
      gbc.gridy++;
      updDirSizeUnitLabel();
    }

    // Checkboxen
    java.util.List<JCheckBox> checkBoxes = null;
    this.cbApplyReadOnly                 = null;
    this.cbReadOnly                      = null;
    this.cbAutoRefresh                   = null;
    this.cbForceLowerCase                = null;
    if( flags != null ) {
      checkBoxes = new ArrayList<>( 4 );
      for( int i = 0; i < flags.length; i++ ) {
	if( flags[ i ] != null ) {
	  switch( flags[ i ] ) {
	    case APPLY_READONLY:
	      this.cbApplyReadOnly = GUIFactory.createCheckBox(
					"Schreibschutzattribut anwenden",
					lastApplyReadOnly );
	      checkBoxes.add( this.cbApplyReadOnly );
	      break;

	    case READONLY:
	      this.cbReadOnly = GUIFactory.createCheckBox(
					"Schreibschutz (Nur-Lese-Modus)",
					true );
	      checkBoxes.add( this.cbReadOnly );
	      break;

	    case AUTO_REFRESH:
	      this.cbAutoRefresh = GUIFactory.createCheckBox(
					"Automatisch aktualisieren",
					false );
	      checkBoxes.add( this.cbAutoRefresh );
	      break;

	    case FORCE_LOWERCASE:
	      this.cbForceLowerCase = GUIFactory.createCheckBox(
					"Dateinamen klein schreiben",
					lastForceLowerCase );
	      checkBoxes.add( this.cbForceLowerCase );
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
    JPanel panelBtns = GUIFactory.createPanel(
					new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    add( panelBtns, gbc );

    this.btnOK = GUIFactory.createButtonOK();
    panelBtns.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtns.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );
  }


  public boolean getApplyReadOnly()
  {
    return this.cbApplyReadOnly != null ?
				this.cbApplyReadOnly.isSelected()
				: false;
  }


  public boolean getAutoRefresh()
  {
    boolean state = false;
    if( this.cbAutoRefresh != null ) {
      state = this.cbAutoRefresh.isSelected();
    }
    return state;
  }


  public boolean getBlockNum16Bit()
  {
    boolean rv = true;
    if( this.fmtSelectFld != null ) {
      rv = this.fmtSelectFld.isBlockNum16Bit();
    } else if( this.rbBlockNum16Bit != null ) {
      rv = this.rbBlockNum16Bit.isSelected();
    }
    return rv;
  }


  public int getBlockSize()
  {
    int rv = 2048;
    if( this.fmtSelectFld != null ) {
      rv = this.fmtSelectFld.getBlockSize();
    } else if( this.comboBlockSize != null ) {
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
    int rv = 2;
    if( this.fmtSelectFld != null ) {
      rv = this.fmtSelectFld.getDirBlocks();
    } else if( this.spinnerDirBlocks != null ) {
      rv = getIntValue( this.spinnerDirBlocks );
    }
    return rv;
  }


  public boolean getForceLowerCase()
  {
    return this.cbForceLowerCase != null ?
				this.cbForceLowerCase.isSelected()
				: false;
  }


  public FloppyDiskFormat getFormat()
  {
    return this.selectedFmt;
  }


  public boolean getReadOnly()
  {
    return this.cbReadOnly != null ? this.cbReadOnly.isSelected() : false;
  }


  public int getSysTracks()
  {
    int rv = 2;
    if( this.fmtSelectFld != null ) {
      rv = this.fmtSelectFld.getSysTracks();
    } else if( this.spinnerSysTracks != null ) {
      rv = getIntValue( this.spinnerSysTracks );
    }
    return rv;
  }


  public void setAutoRefresh( boolean state )
  {
    if( this.cbAutoRefresh != null )
      this.cbAutoRefresh.setSelected( state );
  }


  public void setForceLowerCase( boolean state )
  {
    if( this.cbForceLowerCase != null )
      this.cbForceLowerCase.setSelected( state );
  }


  public void setRecognizedBlockNum16Bit( Boolean value )
  {
    if( value != null ) {
      boolean state = value.booleanValue();
      if( this.fmtSelectFld != null ) {
	this.fmtSelectFld.setBlockNum16Bit( state );
      }
      if( state ) {
	if( this.rbBlockNum16Bit != null ) {
	  this.rbBlockNum16Bit.setSelected( true );
	}
      } else {
	if( this.rbBlockNum8Bit != null ) {
	  this.rbBlockNum8Bit.setSelected( true );
	}
      }
    }
    if( this.infoBlockNumFmt != null ) {
      String text  = TEXT_CHOOSE;
      Color  color = COLOR_EMPHASIZED;
      if( value != null ) {
	text = String.format(
			"%s Bit Blocknummern erkannt",
			value.booleanValue() ? "16" : "8" );
	color = COLOR_RECOGNIZED;
      }
      this.infoBlockNumFmt.setText( text );
      this.infoBlockNumFmt.setForeground( color );
      pack();
    }
  }


  public void setRecognizedBlockSize( Integer value )
  {
    if( value != null ) {
      if( this.fmtSelectFld != null ) {
	this.fmtSelectFld.setBlockSize( value );
      }
      if( this.comboBlockSize != null ) {
	this.comboBlockSize.setSelectedItem( value / 1024 );
	updDirSizeUnitLabel();
      }
    }
    if( this.infoBlockSize != null ) {
      /*
       * Die Blockgroesse laesst sich nicht zu 100% sicher erkennen.
       * Aus diesem Grund wird die Info hervorgehoben mit dem Hinweis
       * zur Pruefung angezeigt.
       */
      String text  = TEXT_CHOOSE;
      Color  color = COLOR_EMPHASIZED;
      if( value != null ) {
	text = String.format(
			"%d KByte erkannt",
			value.intValue() / 1024 );
	color = COLOR_RECOGNIZED;
      }
      this.infoBlockSize.setText( text );
      this.infoBlockSize.setForeground( color );
      pack();
    }
  }


  public void setRecognizedSysTracks( Integer value )
  {
    if( value != null ) {
      if( this.fmtSelectFld != null ) {
	this.fmtSelectFld.setSysTracks( value.intValue() );
      }
      if( this.spinnerSysTracks != null ) {
	try {
	  this.spinnerSysTracks.setValue( value );
	}
	catch( IllegalArgumentException ex ) {}
      }
    }
    if( this.infoSysTracks != null ) {
      String text  = TEXT_CHOOSE;
      Color  color = COLOR_EMPHASIZED;
      if( value != null ) {
	if( value.intValue() == 1 ) {
	  text = "1 Systemspur erkannt";
	} else {
	  text = String.format( "%d Systemspuren erkannt", value );
	}
	color = COLOR_RECOGNIZED;
      }
      this.infoSysTracks.setText( text );
      this.infoSysTracks.setForeground( color );
      pack();
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
    if( e.getSource() == this.spinnerDirBlocks ) {
      updDirSizeUnitLabel();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      if( this.labelDirSizeUnit != null ) {
	if( this.spinnerDirBlocks != null ) {
	  this.spinnerDirBlocks.addChangeListener( this );
	}
	if( this.comboBlockSize != null ) {
	  this.comboBlockSize.addActionListener( this );
	}
      }
      if( this.cbReadOnly != null ) {
	this.cbReadOnly.addActionListener( this );
      }
      this.btnOK.addActionListener( this );
      this.btnCancel.addActionListener( this );
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.btnOK ) {
	rv = true;
	if( this.fmtSelectFld != null ) {
	  this.selectedFmt = this.fmtSelectFld.getFormat();
	  this.approved    = true;
	} else if( this.comboFmt != null ) {
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
	  if( this.cbApplyReadOnly != null ) {
	    lastApplyReadOnly = this.cbApplyReadOnly.isSelected();
	  }
	  if( this.cbForceLowerCase != null ) {
	    lastForceLowerCase = this.cbForceLowerCase.isSelected();
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
      else if( src == this.cbReadOnly ) {
	rv = true;
	updReadOnlyDependingFlds();
      }
    }
    return rv;
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      if( this.labelDirSizeUnit != null ) {
	if( this.spinnerDirBlocks != null ) {
	  this.spinnerDirBlocks.removeChangeListener( this );
	}
	if( this.comboBlockSize != null ) {
	  this.comboBlockSize.removeActionListener( this );
	}
      }
      if( this.cbReadOnly != null ) {
	this.cbReadOnly.removeActionListener( this );
      }
      this.btnOK.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
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
    if( this.cbReadOnly != null ) {
      if( this.cbForceLowerCase != null ) {
	this.cbForceLowerCase.setEnabled( !this.cbReadOnly.isSelected() );
      }
    }
  }
}
