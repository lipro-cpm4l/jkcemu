/*
 * (c) 2013-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente zur Auswahl eines Diskettenformates
 */

package jkcemu.disk;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.base.GUIFactory;


public class FloppyDiskFormatSelectFld extends JPanel
{
  private java.util.List<ChangeListener> changeListeners;
  private JRadioButton                   rbFmt1738Ki3ds;
  private JRadioButton                   rbFmt1440K;
  private JRadioButton                   rbFmt1200K;
  private JRadioButton                   rbFmt800Ki4;
  private JRadioButton                   rbFmt780K;
  private JRadioButton                   rbFmt780Ki2;
  private JRadioButton                   rbFmt780Ki3;
  private JRadioButton                   rbFmt780Ki3ds;
  private JRadioButton                   rbFmt720K;
  private JRadioButton                   rbFmt711Ki5basdos;
  private JRadioButton                   rbFmt702Ki3ds;
  private JRadioButton                   rbFmt624K;
  private JRadioButton                   rbFmt400K;
  private JRadioButton                   rbFmtEtc;
  private JLabel                         labelCyls;
  private JLabel                         labelSides;
  private JLabel                         labelSysTracks;
  private JLabel                         labelSectPerTrack;
  private JLabel                         labelInterleave;
  private JLabel                         labelSectorSize;
  private JLabel                         labelSectorSizeUnit;
  private JLabel                         labelBlockSize;
  private JLabel                         labelBlockSizeUnit;
  private JLabel                         labelBlockNumSize;
  private JLabel                         labelBlockNumSizeUnit;
  private JLabel                         labelDirBlocks;
  private JLabel                         labelDirEntriesInfo;
  private JComboBox<Integer>             comboCyls;
  private JComboBox<Integer>             comboSides;
  private JSpinner                       spinnerSectPerTrack;
  private JSpinner                       spinnerSysTracks;
  private JSpinner                       spinnerInterleave;
  private SpinnerNumberModel             modelInterleave;
  private JComboBox<Integer>             comboSectorSize;
  private JComboBox<Integer>             comboBlockSizeKB;
  private JComboBox<Integer>             comboBlockNumSize;
  private JSpinner                       spinnerDirBlocks;
  private JCheckBox                      cbDateStamper;


  public FloppyDiskFormatSelectFld( boolean askForInterleave )
  {
    this.changeListeners = null;
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 0, 0, 0, 0 ),
					0, 0 );

    ButtonGroup grpFmt = new ButtonGroup();

    this.rbFmt1738Ki3ds = GUIFactory.createRadioButton(
			FloppyDiskFormat.FMT_1738K_I3_DS.toString() );
    grpFmt.add( this.rbFmt1738Ki3ds );
    add( this.rbFmt1738Ki3ds, gbc );

    this.rbFmt1440K = GUIFactory.createRadioButton(
			FloppyDiskFormat.FMT_1440K.toString() );
    grpFmt.add( this.rbFmt1440K );
    gbc.gridy++;
    add( this.rbFmt1440K, gbc );

    this.rbFmt1200K = GUIFactory.createRadioButton(
			FloppyDiskFormat.FMT_1200K.toString() );
    grpFmt.add( this.rbFmt1200K );
    gbc.gridy++;
    add( this.rbFmt1200K, gbc );

    this.rbFmt800Ki4 = GUIFactory.createRadioButton(
			FloppyDiskFormat.FMT_800K_I4.toString() );
    grpFmt.add( this.rbFmt800Ki4 );
    gbc.gridy++;
    add( this.rbFmt800Ki4, gbc );

    this.rbFmt780K = GUIFactory.createRadioButton(
			FloppyDiskFormat.FMT_780K.toString() );
    grpFmt.add( this.rbFmt780K );
    gbc.gridy++;
    add( this.rbFmt780K, gbc );

    this.rbFmt780Ki2 = GUIFactory.createRadioButton(
			FloppyDiskFormat.FMT_780K_I2.toString() );
    grpFmt.add( this.rbFmt780Ki2 );
    gbc.gridy++;
    add( this.rbFmt780Ki2, gbc );

    this.rbFmt780Ki3 = GUIFactory.createRadioButton(
			FloppyDiskFormat.FMT_780K_I3.toString() );
    grpFmt.add( this.rbFmt780Ki3 );
    gbc.gridy++;
    add( this.rbFmt780Ki3, gbc );

    this.rbFmt780Ki3ds = GUIFactory.createRadioButton(
		FloppyDiskFormat.FMT_780K_I3_DS.toString() );
    grpFmt.add( this.rbFmt780Ki3ds );
    gbc.gridy++;
    add( this.rbFmt780Ki3ds, gbc );

    int gridyFmtEtc = gbc.gridy + 1;

    this.rbFmt720K = GUIFactory.createRadioButton(
			FloppyDiskFormat.FMT_720K.toString() );
    grpFmt.add( this.rbFmt720K );
    gbc.insets.left = 20;
    gbc.gridy       = 0;
    gbc.gridx++;
    add( this.rbFmt720K, gbc );

    this.rbFmt711Ki5basdos = GUIFactory.createRadioButton(
			FloppyDiskFormat.FMT_711K_I5_BASDOS.toString() );
    grpFmt.add( this.rbFmt711Ki5basdos );
    gbc.gridy++;
    add( this.rbFmt711Ki5basdos, gbc );

    this.rbFmt702Ki3ds = GUIFactory.createRadioButton(
			FloppyDiskFormat.FMT_702K_I3_DS.toString() );
    grpFmt.add( this.rbFmt702Ki3ds );
    gbc.gridy++;
    add( this.rbFmt702Ki3ds, gbc );

    this.rbFmt624K = GUIFactory.createRadioButton(
			FloppyDiskFormat.FMT_624K.toString() );
    grpFmt.add( this.rbFmt624K );
    gbc.gridy++;
    add( this.rbFmt624K, gbc );

    this.rbFmt400K = GUIFactory.createRadioButton(
			FloppyDiskFormat.FMT_400K.toString() );
    grpFmt.add( this.rbFmt400K );
    gbc.gridy++;
    add( this.rbFmt400K, gbc );

    this.rbFmtEtc = GUIFactory.createRadioButton(
					"Sonstiges Format:",
					true );
    grpFmt.add( this.rbFmtEtc );
    gbc.insets.left = 0;
    gbc.gridx       = 0;
    gbc.gridy       = gridyFmtEtc;
    add( this.rbFmtEtc, gbc );

    JPanel panelFmtEtc = GUIFactory.createPanel( new GridBagLayout() );
    gbc.insets.left = 50;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridy++;
    add( panelFmtEtc, gbc );

    GridBagConstraints gbcFmtEtc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 2, 2, 2, 2),
						0, 0 );

    this.labelCyls  = GUIFactory.createLabel( "Spuren:" );
    panelFmtEtc.add( this.labelCyls, gbcFmtEtc );

    this.comboCyls = createJComboBox( 40, 77, 80 );
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.comboCyls, gbcFmtEtc );

    this.labelSysTracks = GUIFactory.createLabel( "davon Systemspuren:" );
    gbcFmtEtc.anchor          = GridBagConstraints.EAST;
    gbcFmtEtc.gridx += 2;
    panelFmtEtc.add( this.labelSysTracks, gbcFmtEtc );

    this.spinnerSysTracks = GUIFactory.createSpinner(
				new SpinnerNumberModel( 0, 0, 9, 1 ) );
    gbcFmtEtc.anchor = GridBagConstraints.WEST;
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.spinnerSysTracks, gbcFmtEtc );

    this.labelSides = GUIFactory.createLabel( "Seiten:" );
    gbcFmtEtc.gridx = 0;
    gbcFmtEtc.gridy++;
    panelFmtEtc.add( this.labelSides, gbcFmtEtc );

    this.comboSides = createJComboBox( 1, 2 );
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.comboSides, gbcFmtEtc );

    this.labelSectPerTrack = GUIFactory.createLabel( "Sektoren pro Spur:" );
    gbcFmtEtc.gridx        = 0;
    gbcFmtEtc.gridy++;
    panelFmtEtc.add( this.labelSectPerTrack, gbcFmtEtc );

    this.spinnerSectPerTrack = GUIFactory.createSpinner(
				new SpinnerNumberModel( 5, 1, 39, 1 ) );
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.spinnerSectPerTrack, gbcFmtEtc );

    this.labelInterleave   = null;
    this.modelInterleave   = null;
    this.spinnerInterleave = null;
    if( askForInterleave ) {
      this.labelInterleave = GUIFactory.createLabel(
					"Interleave (1 = keins):" );
      gbcFmtEtc.anchor = GridBagConstraints.EAST;
      gbcFmtEtc.gridx += 2;
      panelFmtEtc.add( this.labelInterleave, gbcFmtEtc );

      this.modelInterleave   = new SpinnerNumberModel( 1, 1, 1, 1 );
      this.spinnerInterleave = GUIFactory.createSpinner(
						this.modelInterleave );
      gbcFmtEtc.anchor = GridBagConstraints.WEST;
      gbcFmtEtc.gridx++;
      panelFmtEtc.add( this.spinnerInterleave, gbcFmtEtc );
    }

    this.labelSectorSize = GUIFactory.createLabel(
					"Sektorgr\u00F6\u00DFe:" );
    gbcFmtEtc.gridx = 0;
    gbcFmtEtc.gridy++;
    panelFmtEtc.add( this.labelSectorSize, gbcFmtEtc );

    this.comboSectorSize = createJComboBox( 128, 256, 512, 1024 );
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.comboSectorSize, gbcFmtEtc );

    this.labelSectorSizeUnit = GUIFactory.createLabel( "Byte" );
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.labelSectorSizeUnit, gbcFmtEtc );

    this.labelBlockSize = GUIFactory.createLabel( "Blockgr\u00F6\u00DFe:" );
    gbcFmtEtc.gridx     = 0;
    gbcFmtEtc.gridy++;
    panelFmtEtc.add( this.labelBlockSize, gbcFmtEtc );

    this.comboBlockSizeKB = createJComboBox( 1, 2, 4, 8, 16 );
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.comboBlockSizeKB, gbcFmtEtc );

    this.labelBlockSizeUnit = GUIFactory.createLabel( "KByte" );
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.labelBlockSizeUnit, gbcFmtEtc );

    this.labelBlockNumSize = GUIFactory.createLabel( "Blocknummern:" );
    gbcFmtEtc.anchor = GridBagConstraints.EAST;
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.labelBlockNumSize, gbcFmtEtc );

    this.comboBlockNumSize = createJComboBox( 8, 16 );
    gbcFmtEtc.anchor       = GridBagConstraints.WEST;
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.comboBlockNumSize, gbcFmtEtc );

    this.labelBlockNumSizeUnit = GUIFactory.createLabel( "Bit" );
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.labelBlockNumSizeUnit, gbcFmtEtc );

    this.labelDirBlocks = GUIFactory.createLabel( "Directory:" );
    gbcFmtEtc.gridx     = 0;
    gbcFmtEtc.gridy++;
    panelFmtEtc.add( this.labelDirBlocks, gbcFmtEtc );

    this.spinnerDirBlocks = GUIFactory.createSpinner(
				new SpinnerNumberModel( 2, 1, 9, 1 ) );
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.spinnerDirBlocks, gbcFmtEtc );

    this.labelDirEntriesInfo = GUIFactory.createLabel( "Bl\u00F6cke" );
    gbcFmtEtc.gridwidth      = GridBagConstraints.REMAINDER;
    gbcFmtEtc.gridx++;
    panelFmtEtc.add( this.labelDirEntriesInfo, gbcFmtEtc );

    this.cbDateStamper = GUIFactory.createCheckBox(
		"Dateien mit Zeitstempel versehen (DateStamper)" );
    gbcFmtEtc.gridx = 1;
    gbcFmtEtc.gridy++;
    panelFmtEtc.add( this.cbDateStamper, gbcFmtEtc );


    // Listener
    ActionListener fmtListener = new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    updFmtDetailsFields();
			    fireFormatChanged();
			  }
			};
    this.rbFmt1738Ki3ds.addActionListener( fmtListener );
    this.rbFmt1440K.addActionListener( fmtListener );
    this.rbFmt1200K.addActionListener( fmtListener );
    this.rbFmt800Ki4.addActionListener( fmtListener );
    this.rbFmt780K.addActionListener( fmtListener );
    this.rbFmt780Ki2.addActionListener( fmtListener );
    this.rbFmt780Ki3.addActionListener( fmtListener );
    this.rbFmt780Ki3ds.addActionListener( fmtListener );
    this.rbFmt720K.addActionListener( fmtListener );
    this.rbFmt711Ki5basdos.addActionListener( fmtListener );
    this.rbFmt702Ki3ds.addActionListener( fmtListener );
    this.rbFmt624K.addActionListener( fmtListener );
    this.rbFmt400K.addActionListener( fmtListener );
    this.rbFmtEtc.addActionListener( fmtListener );

    ActionListener fmtChangeListener = new ActionListener()
			{
			  @Override
			  public void actionPerformed( ActionEvent e )
			  {
			    fireFormatChanged();
			  }
			};
    this.comboCyls.addActionListener( fmtChangeListener );
    this.comboSides.addActionListener( fmtChangeListener );
    this.comboSectorSize.addActionListener( fmtChangeListener );
    this.comboBlockSizeKB.addActionListener(
			new ActionListener()
			{
			  @Override
			  public void actionPerformed( ActionEvent e )
			  {
			    updDirEntriesInfo();
			    fireFormatChanged();
			  }
			} );
    this.spinnerSectPerTrack.addChangeListener(
			new ChangeListener()
			{
			  @Override
			  public void stateChanged( ChangeEvent e )
			  {
			    updDirEntriesInfo();
			    updMaxInterleave();
			    fireFormatChanged();
			  }
			} );

    this.spinnerDirBlocks.addChangeListener(
			new ChangeListener()
			{
			  @Override
			  public void stateChanged( ChangeEvent e )
			  {
			    updDirEntriesInfo();
			    fireFormatChanged();
			  }
			} );

    this.spinnerSysTracks.addChangeListener(
			new ChangeListener()
			{
			  @Override
			  public void stateChanged( ChangeEvent e )
			  {
			    fireFormatChanged();
			  }
			} );


    // sonstiges
    setFmtDetailsFields( FloppyDiskFormat.FMT_780K );
    updFmtDetailsFields();
    updDirEntriesInfo();
    updMaxInterleave();
  }


  public synchronized void addChangeListener( ChangeListener listener )
  {
    if( this.changeListeners == null ) {
      this.changeListeners = new ArrayList<>();
    }
    this.changeListeners.add( listener );
  }


  public synchronized void removeChangeListener( ChangeListener listener )
  {
    if( this.changeListeners != null ) {
      this.changeListeners.remove( listener );
      if( this.changeListeners.isEmpty() ) {
	this.changeListeners = null;
      }
    }
  }


  public int getBlockSize()
  {
    return getIntValue( this.comboBlockSizeKB ) * 1024;
  }


  public int getCylinders()
  {
    return getIntValue( this.comboCyls );
  }


  public int getDirBlocks()
  {
    return getIntValue( this.spinnerDirBlocks );
  }


  public FloppyDiskFormat getFormat()
  {
    FloppyDiskFormat rv = null;
    if( this.rbFmt1738Ki3ds.isSelected() ) {
      rv = FloppyDiskFormat.FMT_1738K_I3_DS;
    } else if( this.rbFmt1440K.isSelected() ) {
      rv = FloppyDiskFormat.FMT_1440K;
    } else if( this.rbFmt1200K.isSelected() ) {
      rv = FloppyDiskFormat.FMT_1200K;
    } else if( this.rbFmt800Ki4.isSelected() ) {
      rv = FloppyDiskFormat.FMT_800K_I4;
    } else if( this.rbFmt780K.isSelected() ) {
      rv = FloppyDiskFormat.FMT_780K;
    } else if( this.rbFmt780Ki2.isSelected() ) {
      rv = FloppyDiskFormat.FMT_780K_I2;
    } else if( this.rbFmt780Ki3.isSelected() ) {
      rv = FloppyDiskFormat.FMT_780K_I3;
    } else if( this.rbFmt780Ki3ds.isSelected() ) {
      rv = FloppyDiskFormat.FMT_780K_I3_DS;
    } else if( this.rbFmt720K.isSelected() ) {
      rv = FloppyDiskFormat.FMT_720K;
    } else if( this.rbFmt711Ki5basdos.isSelected() ) {
      rv = FloppyDiskFormat.FMT_711K_I5_BASDOS;
    } else if( this.rbFmt702Ki3ds.isSelected() ) {
      rv = FloppyDiskFormat.FMT_702K_I3_DS;
    } else if( this.rbFmt624K.isSelected() ) {
      rv = FloppyDiskFormat.FMT_624K;
    } else if( this.rbFmt400K.isSelected() ) {
      rv = FloppyDiskFormat.FMT_400K;
    } else {
      rv = new FloppyDiskFormat(
			getCylinders(),
			getSides(),
			getSectorsPerTrack(),
			getSectorSize(),
			getInterleave(),
			getSysTracks(),
			getDirBlocks(),
			getBlockSize(),
			isBlockNum16Bit(),
			isDateStamperEnabled(),
			null );
    }
    return rv;
  }


  public int getInterleave()
  {
    return getIntValue( this.spinnerInterleave );
  }


  public int getSectorsPerTrack()
  {
    return getIntValue( this.spinnerSectPerTrack );
  }


  public int getSectorSize()
  {
    return getIntValue( this.comboSectorSize );
  }


  public int getSides()
  {
    return getIntValue( this.comboSides );
  }


  public int getSysTracks()
  {
    return getIntValue( this.spinnerSysTracks );
  }


  public boolean isBlockNum16Bit()
  {
    return (getIntValue( this.comboBlockNumSize ) == 16);
  }


  public boolean isDateStamperEnabled()
  {
    return this.cbDateStamper.isSelected();
  }


  public void setBlockSize( int value )
  {
    if( getBlockSize() != value ) {
      this.rbFmtEtc.setSelected( true );
      updFmtDetailsFieldsEnabled();
      setValue( this.comboBlockSizeKB, value / 1024 );
    }
  }


  public void setBlockNum16Bit( boolean state )
  {
    if( isBlockNum16Bit() != state ) {
      this.rbFmtEtc.setSelected( true );
      updFmtDetailsFieldsEnabled();
      setValue( this.comboBlockNumSize, state ? 16 : 8 );
    }
  }


  public void setDateStamperEnabled( boolean state )
  {
    this.cbDateStamper.setSelected( state );
  }


  public void setDirBlocks( int value )
  {
    if( getDirBlocks() != value ) {
      this.rbFmtEtc.setSelected( true );
      updFmtDetailsFieldsEnabled();
      setValue( this.spinnerDirBlocks, value );
    }
  }


  public void setFormat( FloppyDiskFormat fmt )
  {
    if( fmt != null ) {
      if( fmt.equals( FloppyDiskFormat.FMT_1738K_I3_DS ) ) {
	this.rbFmt1738Ki3ds.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_1440K ) ) {
	this.rbFmt1440K.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_1200K ) ) {
	this.rbFmt1200K.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_800K_I4 ) ) {
	this.rbFmt800Ki4.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_780K ) ) {
	this.rbFmt780K.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_780K_I2 ) ) {
	this.rbFmt780Ki2.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_780K_I3 ) ) {
	this.rbFmt780Ki3.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_780K_I3_DS ) ) {
	this.rbFmt780Ki3ds.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_720K ) ) {
	this.rbFmt720K.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_711K_I5_BASDOS ) ) {
	this.rbFmt711Ki5basdos.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_702K_I3_DS ) ) {
	this.rbFmt702Ki3ds.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_624K ) ) {
	this.rbFmt624K.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_400K ) ) {
	this.rbFmt400K.setSelected( true );
      } else {
	this.rbFmtEtc.setSelected( true );
	setFmtDetailsFields( fmt );
      }
      updFmtDetailsFieldsEnabled();
    }
  }


  public void setSysTracks( int value )
  {
    if( getSysTracks() != value ) {
      this.rbFmtEtc.setSelected( true );
      updFmtDetailsFieldsEnabled();
      setValue( this.spinnerSysTracks, value );
    }
  }


	/* --- private Methoden --- */

  private static JComboBox<Integer> createJComboBox( int... items )
  {
    JComboBox<Integer> combo = GUIFactory.createComboBox();
    combo.setEditable( false );
    if( items != null ) {
      for( int i = 0; i < items.length; i++ ) {
	combo.addItem( items[ i ] );
      }
    }
    return combo;
  }


  private synchronized void fireFormatChanged()
  {
    if( this.changeListeners != null ) {
      ChangeEvent e = new ChangeEvent( this );
      for( ChangeListener listener : this.changeListeners ) {
	listener.stateChanged( e );
      }
    }
  }


  private int getIntValue( JComboBox<Integer> combo )
  {
    int rv = 0;
    if( combo != null ) {
      Object o = combo.getSelectedItem();
      if( o != null ) {
	if( o instanceof Number ) {
	  rv = ((Number) o).intValue();
	}
      }
    }
    return rv;
  }


  private int getIntValue( JSpinner spinner )
  {
    int rv = 0;
    if( spinner != null ) {
      Object o = spinner.getValue();
      if( o != null ) {
	if( o instanceof Number ) {
	  rv = ((Number) o).intValue();
	}
      }
    }
    return rv;
  }


  private void setFmtDetailsFields( FloppyDiskFormat fmt )
  {
    setValue( this.comboCyls, fmt.getCylinders() );
    setValue( this.comboSides, fmt.getSides() );
    setValue( this.spinnerSectPerTrack, fmt.getSectorsPerTrack() );
    setValue( this.comboSectorSize, fmt.getSectorSize() );
    setValue( this.spinnerInterleave, fmt.getInterleave() );
    int sysTracks = fmt.getSysTracks();
    int dirBlocks = fmt.getDirBlocks();
    int blockSize = fmt.getBlockSize();
    if( (sysTracks >= 0) && (blockSize > 0) && (dirBlocks > 0) ) {
      setValue( this.spinnerSysTracks, sysTracks );
      setValue( this.spinnerDirBlocks, dirBlocks );
      setValue( this.comboBlockSizeKB, blockSize / 1024 );
      setValue( this.comboBlockNumSize, fmt.isBlockNum16Bit() ? 16 : 8 );
    }
    setDateStamperEnabled( fmt.isDateStamperEnabled() );
    updMaxInterleave();
  }


  private static void setValue( JComboBox<Integer> combo, int value )
  {
    if( combo != null )
      combo.setSelectedItem( value );
  }


  private static void setValue( JSpinner spinner, int value )
  {
    if( spinner != null ) {
      try {
	spinner.setValue( value );
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  private void updDirEntriesInfo()
  {
    if( this.labelDirEntriesInfo != null ) {
      this.labelDirEntriesInfo.setText(
		String.format(
			"Bl\u00F6cke (%d Eintr\u00E4ge)",
			getIntValue( this.comboBlockSizeKB ) * 1024
				* getIntValue( this.spinnerDirBlocks )
				/ 32 ) );
    }
  }


  private void updFmtDetailsFields()
  {
    FloppyDiskFormat fmt = null;
    if( this.rbFmt1738Ki3ds.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_1738K_I3_DS;
    } else if( this.rbFmt1440K.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_1440K;
    } else if( this.rbFmt1200K.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_1200K;
    } else if( this.rbFmt800Ki4.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_800K_I4;
    } else if( this.rbFmt780K.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_780K;
    } else if( this.rbFmt780Ki2.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_780K_I2;
    } else if( this.rbFmt780Ki3.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_780K_I3;
    } else if( this.rbFmt780Ki3ds.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_780K_I3_DS;
    } else if( this.rbFmt720K.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_720K;
    } else if( this.rbFmt711Ki5basdos.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_711K_I5_BASDOS;
    } else if( this.rbFmt702Ki3ds.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_702K_I3_DS;
    } else if( this.rbFmt624K.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_624K;
    } else if( this.rbFmt400K.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_400K;
    }
    if( fmt != null ) {
      setFmtDetailsFields( fmt );
    }
    updFmtDetailsFieldsEnabled();
  }


  private void updFmtDetailsFieldsEnabled()
  {
    boolean state = this.rbFmtEtc.isSelected();
    this.labelCyls.setEnabled( state );
    this.comboCyls.setEnabled( state );
    this.labelSides.setEnabled( state );
    this.comboSides.setEnabled( state );
    this.labelSysTracks.setEnabled( state );
    this.spinnerSysTracks.setEnabled( state );
    this.labelSectPerTrack.setEnabled( state );
    this.spinnerSectPerTrack.setEnabled( state );
    if( this.labelInterleave != null ) {
      this.labelInterleave.setEnabled( state );
    }
    if( this.spinnerInterleave != null ) {
      this.spinnerInterleave.setEnabled( state );
    }
    this.labelSectorSize.setEnabled( state );
    this.comboSectorSize.setEnabled( state );
    this.labelSectorSizeUnit.setEnabled( state );
    this.labelBlockSize.setEnabled( state );
    this.comboBlockSizeKB.setEnabled( state );
    this.labelBlockSizeUnit.setEnabled( state );
    this.labelBlockNumSize.setEnabled( state );
    this.comboBlockNumSize.setEnabled( state );
    this.labelBlockNumSize.setEnabled( state );
    this.labelBlockNumSizeUnit.setEnabled( state );
    this.labelDirBlocks.setEnabled( state );
    this.labelDirEntriesInfo.setEnabled( state );
    this.spinnerDirBlocks.setEnabled( state );
    this.labelDirEntriesInfo.setEnabled( state );
    this.cbDateStamper.setEnabled( state );
  }


  private void updMaxInterleave()
  {
    if( this.modelInterleave != null ) {
      int maxInterleave = getIntValue( this.spinnerSectPerTrack ) - 1;
      if( maxInterleave < 1 ) {
	maxInterleave = 1;
      }
      int curInterleave = getIntValue( this.spinnerInterleave );
      if( curInterleave > maxInterleave ) {
	this.modelInterleave.setValue( 1 );
      }
      this.modelInterleave.setMaximum( maxInterleave );
    }
  }
}
