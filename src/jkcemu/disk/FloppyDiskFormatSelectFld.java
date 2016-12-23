/*
 * (c) 2013-2016 Jens Mueller
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
import java.lang.*;
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


public class FloppyDiskFormatSelectFld extends JPanel
{
  private java.util.List<ChangeListener> changeListeners;
  private JRadioButton                   btnFmt800Ki4;
  private JRadioButton                   btnFmt780K;
  private JRadioButton                   btnFmt780Ki2;
  private JRadioButton                   btnFmt780Ki3;
  private JRadioButton                   btnFmt780Ki3ds;
  private JRadioButton                   btnFmt780Ki4;
  private JRadioButton                   btnFmt720K;
  private JRadioButton                   btnFmt711Ki5;
  private JRadioButton                   btnFmt624K;
  private JRadioButton                   btnFmt400K;
  private JRadioButton                   btnFmtEtc;
  private JLabel                         labelSides;
  private JLabel                         labelCyls;
  private JLabel                         labelSysTracks;
  private JLabel                         labelSectPerCyl;
  private JLabel                         labelInterleave;
  private JLabel                         labelSectorSize;
  private JLabel                         labelSectorSizeUnit;
  private JLabel                         labelBlockSize;
  private JLabel                         labelBlockSizeUnit;
  private JLabel                         labelBlockNumSize;
  private JLabel                         labelBlockNumSizeUnit;
  private JLabel                         labelDirBlocks;
  private JLabel                         labelDirEntriesInfo;
  private JComboBox<Integer>             comboSides;
  private JComboBox<Integer>             comboCyls;
  private JSpinner                       spinnerSysTracks;
  private JSpinner                       spinnerInterleave;
  private SpinnerNumberModel             modelInterleave;
  private JComboBox<Integer>             comboSectPerCyl;
  private JComboBox<Integer>             comboSectorSize;
  private JComboBox<Integer>             comboBlockSizeKB;
  private JComboBox<Integer>             comboBlockNumSize;
  private JSpinner                       spinnerDirBlocks;
  private JCheckBox                      btnDateStamper;


  public FloppyDiskFormatSelectFld( boolean askForInterleave )
  {
    this.changeListeners = null;
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 0, 0, 0, 0 ),
					0, 0 );

    ButtonGroup grpFmt = new ButtonGroup();

    this.btnFmt800Ki4 = new JRadioButton(
			FloppyDiskFormat.FMT_800K_I4.toString(),
			false );
    grpFmt.add( this.btnFmt800Ki4 );
    add( this.btnFmt800Ki4, gbc );

    this.btnFmt780K = new JRadioButton(
			FloppyDiskFormat.FMT_780K.toString(),
			false );
    grpFmt.add( this.btnFmt780K );
    gbc.gridy++;
    add( this.btnFmt780K, gbc );

    this.btnFmt780Ki2 = new JRadioButton(
			FloppyDiskFormat.FMT_780K_I2.toString(),
			false );
    grpFmt.add( this.btnFmt780Ki2 );
    gbc.gridy++;
    add( this.btnFmt780Ki2, gbc );

    this.btnFmt780Ki3 = new JRadioButton(
			FloppyDiskFormat.FMT_780K_I3.toString(),
			false );
    grpFmt.add( this.btnFmt780Ki3 );
    gbc.gridy++;
    add( this.btnFmt780Ki3, gbc );

    this.btnFmt780Ki3ds = new JRadioButton(
			FloppyDiskFormat.FMT_780K_I3_DATESTAMPER.toString(),
			false );
    grpFmt.add( this.btnFmt780Ki3ds );
    gbc.gridy++;
    add( this.btnFmt780Ki3ds, gbc );

    this.btnFmt720K = new JRadioButton(
			FloppyDiskFormat.FMT_720K.toString(),
			false );
    grpFmt.add( this.btnFmt720K );
    gbc.gridy++;
    add( this.btnFmt720K, gbc );

    this.btnFmt711Ki5 = new JRadioButton(
			FloppyDiskFormat.FMT_711K_I5_BASDOS.toString(),
			false );
    grpFmt.add( this.btnFmt711Ki5 );
    gbc.gridy++;
    add( this.btnFmt711Ki5, gbc );

    this.btnFmt624K = new JRadioButton(
			FloppyDiskFormat.FMT_624K.toString(),
			false );
    grpFmt.add( this.btnFmt624K );
    gbc.gridy++;
    add( this.btnFmt624K, gbc );

    this.btnFmt400K = new JRadioButton(
			FloppyDiskFormat.FMT_400K.toString(),
			false );
    grpFmt.add( this.btnFmt400K );
    gbc.gridy++;
    add( this.btnFmt400K, gbc );

    this.btnFmtEtc = new JRadioButton( "Sonstiges Format:", true );
    grpFmt.add( this.btnFmtEtc );
    gbc.gridy++;
    add( this.btnFmtEtc, gbc );

    this.labelSides = new JLabel( "Seiten:" );
    gbc.insets.left = 50;
    gbc.gridwidth   = 1;
    gbc.gridy++;
    add( this.labelSides, gbc );

    this.comboSides = createJComboBox( 1, 2 );
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboSides, gbc );

    this.labelCyls  = new JLabel( "Spuren:" );
    gbc.insets.top  = 2;
    gbc.insets.left = 50;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.labelCyls, gbc );

    this.comboCyls  = createJComboBox( 40, 80 );
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboCyls, gbc );

    this.labelSysTracks = new JLabel( "davon Systemspuren:" );
    gbc.anchor          = GridBagConstraints.EAST;
    gbc.gridx += 2;
    add( this.labelSysTracks, gbc );

    this.spinnerSysTracks = new JSpinner(
				new SpinnerNumberModel( 0, 0, 9, 1 ) );
    gbc.anchor            = GridBagConstraints.WEST;
    gbc.insets.left       = 5;
    gbc.gridx++;
    add( this.spinnerSysTracks, gbc );

    this.labelSectPerCyl = new JLabel( "Sektoren pro Spur:" );
    gbc.insets.left      = 50;
    gbc.gridx            = 0;
    gbc.gridy++;
    add( this.labelSectPerCyl, gbc );

    this.comboSectPerCyl = createJComboBox( 5, 8, 9, 15, 16, 18, 36 );
    gbc.insets.left      = 5;
    gbc.gridx++;
    add( this.comboSectPerCyl, gbc );

    this.labelInterleave   = null;
    this.modelInterleave   = null;
    this.spinnerInterleave = null;
    if( askForInterleave ) {
      this.labelInterleave = new JLabel(
			"Interleave (1 = kein Interleave):" );
      gbc.anchor = GridBagConstraints.EAST;
      gbc.gridx += 2;
      add( this.labelInterleave, gbc );

      this.modelInterleave   = new SpinnerNumberModel( 1, 1, 1, 1 );
      this.spinnerInterleave = new JSpinner( this.modelInterleave );
      gbc.anchor = GridBagConstraints.WEST;
      gbc.gridx++;
      add( this.spinnerInterleave, gbc );
    }

    this.labelSectorSize = new JLabel( "Sektorgr\u00F6\u00DFe:" );
    gbc.insets.left      = 50;
    gbc.gridx            = 0;
    gbc.gridy++;
    add( this.labelSectorSize, gbc );

    this.comboSectorSize = createJComboBox( 256, 512, 1024 );
    gbc.insets.left      = 5;
    gbc.gridx++;
    add( this.comboSectorSize, gbc );

    this.labelSectorSizeUnit = new JLabel( "Byte" );
    gbc.gridx++;
    add( this.labelSectorSizeUnit, gbc );

    this.labelBlockSize = new JLabel( "Blockgr\u00F6\u00DFe:" );
    gbc.insets.left     = 50;
    gbc.gridx           = 0;
    gbc.gridy++;
    add( this.labelBlockSize, gbc );

    this.comboBlockSizeKB = createJComboBox( 1, 2, 4, 8, 16 );
    gbc.insets.left       = 5;
    gbc.gridx++;
    add( this.comboBlockSizeKB, gbc );

    this.labelBlockSizeUnit = new JLabel( "KByte" );
    gbc.gridx++;
    add( this.labelBlockSizeUnit, gbc );

    this.labelBlockNumSize = new JLabel( "Blocknummern:" );
    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridx++;
    add( this.labelBlockNumSize, gbc );

    this.comboBlockNumSize = createJComboBox( 8, 16 );
    gbc.anchor             = GridBagConstraints.WEST;
    gbc.insets.left        = 5;
    gbc.gridx++;
    add( this.comboBlockNumSize, gbc );

    this.labelBlockNumSizeUnit = new JLabel( "Bit" );
    gbc.gridx++;
    add( this.labelBlockNumSizeUnit, gbc );

    this.labelDirBlocks = new JLabel( "Directory:" );
    gbc.insets.left     = 50;
    gbc.gridx           = 0;
    gbc.gridy++;
    add( this.labelDirBlocks, gbc );

    this.spinnerDirBlocks = new JSpinner(
			new SpinnerNumberModel( 2, 1, 9, 1 ) );
    gbc.insets.left  = 5;
    gbc.gridx++;
    add( this.spinnerDirBlocks, gbc );

    this.labelDirEntriesInfo = new JLabel( "Bl\u00F6cke" );
    gbc.gridwidth            = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    add( this.labelDirEntriesInfo, gbc );

    this.btnDateStamper = new JCheckBox(
		"Dateien mit Zeitstempel versehen (DateStamper)" );
    gbc.insets.left   = 5;
    gbc.insets.bottom = 5;
    gbc.gridx = 1;
    gbc.gridy++;
    add( this.btnDateStamper, gbc );


    // Listener
    ActionListener fmtListener = new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    updFmtDetailsFields();
			    fireFormatChanged();
			  }
			};
    this.btnFmt800Ki4.addActionListener( fmtListener );
    this.btnFmt780K.addActionListener( fmtListener );
    this.btnFmt780Ki2.addActionListener( fmtListener );
    this.btnFmt780Ki3.addActionListener( fmtListener );
    this.btnFmt780Ki3ds.addActionListener( fmtListener );
    this.btnFmt720K.addActionListener( fmtListener );
    this.btnFmt711Ki5.addActionListener( fmtListener );
    this.btnFmt624K.addActionListener( fmtListener );
    this.btnFmt400K.addActionListener( fmtListener );
    this.btnFmtEtc.addActionListener( fmtListener );

    ActionListener fmtChangeListener = new ActionListener()
			{
			  @Override
			  public void actionPerformed( ActionEvent e )
			  {
			    fireFormatChanged();
			  }
			};
    this.comboSides.addActionListener( fmtChangeListener );
    this.comboCyls.addActionListener( fmtChangeListener );
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
    this.comboSectPerCyl.addActionListener(
			new ActionListener()
			{
			  @Override
			  public void actionPerformed( ActionEvent e )
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
    if( this.btnFmt800Ki4.isSelected() ) {
      rv = FloppyDiskFormat.FMT_800K_I4;
    } else if( this.btnFmt780K.isSelected() ) {
      rv = FloppyDiskFormat.FMT_780K;
    } else if( this.btnFmt780Ki2.isSelected() ) {
      rv = FloppyDiskFormat.FMT_780K_I2;
    } else if( this.btnFmt780Ki3.isSelected() ) {
      rv = FloppyDiskFormat.FMT_780K_I3;
    } else if( this.btnFmt780Ki3ds.isSelected() ) {
      rv = FloppyDiskFormat.FMT_780K_I3_DATESTAMPER;
    } else if( this.btnFmt720K.isSelected() ) {
      rv = FloppyDiskFormat.FMT_720K;
    } else if( this.btnFmt711Ki5.isSelected() ) {
      rv = FloppyDiskFormat.FMT_711K_I5_BASDOS;
    } else if( this.btnFmt624K.isSelected() ) {
      rv = FloppyDiskFormat.FMT_624K;
    } else if( this.btnFmt400K.isSelected() ) {
      rv = FloppyDiskFormat.FMT_400K;
    } else {
      rv = new FloppyDiskFormat(
			getSides(),
			getCylinders(),
			getSectorsPerCylinder(),
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


  public int getSectorsPerCylinder()
  {
    return getIntValue( this.comboSectPerCyl );
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
    return this.btnDateStamper.isSelected();
  }


  public void setBlockSize( int value )
  {
    if( getBlockSize() != value ) {
      this.btnFmtEtc.setSelected( true );
      updFmtDetailsFieldsEnabled();
      setValue( this.comboBlockSizeKB, value / 1024 );
    }
  }


  public void setBlockNum16Bit( boolean state )
  {
    if( isBlockNum16Bit() != state ) {
      this.btnFmtEtc.setSelected( true );
      updFmtDetailsFieldsEnabled();
      setValue( this.comboBlockNumSize, state ? 16 : 8 );
    }
  }


  public void setDateStamperEnabled( boolean state )
  {
    this.btnDateStamper.setSelected( state );
  }


  public void setDirBlocks( int value )
  {
    if( getDirBlocks() != value ) {
      this.btnFmtEtc.setSelected( true );
      updFmtDetailsFieldsEnabled();
      setValue( this.spinnerDirBlocks, value );
    }
  }


  public void setFormat( FloppyDiskFormat fmt )
  {
    if( fmt != null ) {
      if( fmt.equals( FloppyDiskFormat.FMT_800K_I4 ) ) {
	this.btnFmt800Ki4.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_780K ) ) {
	this.btnFmt780K.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_780K_I2 ) ) {
	this.btnFmt780Ki2.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_780K_I3 ) ) {
	this.btnFmt780Ki3.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_780K_I3_DATESTAMPER ) ) {
	this.btnFmt780Ki3ds.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_720K ) ) {
	this.btnFmt720K.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_711K_I5_BASDOS ) ) {
	this.btnFmt711Ki5.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_624K ) ) {
	this.btnFmt624K.setSelected( true );
      } else if( fmt.equals( FloppyDiskFormat.FMT_400K ) ) {
	this.btnFmt400K.setSelected( true );
      } else {
	this.btnFmtEtc.setSelected( true );
	setFmtDetailsFields( fmt );
      }
      updFmtDetailsFieldsEnabled();
    }
  }


  public void setSysTracks( int value )
  {
    if( getSysTracks() != value ) {
      this.btnFmtEtc.setSelected( true );
      updFmtDetailsFieldsEnabled();
      setValue( this.spinnerSysTracks, value );
    }
  }


	/* --- private Methoden --- */

  private static JComboBox<Integer> createJComboBox( int... items )
  {
    JComboBox<Integer> combo = new JComboBox<>();
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


  private int getIntValue( JComboBox combo )
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
    setValue( this.comboSides, fmt.getSides() );
    setValue( this.comboCyls, fmt.getCylinders() );
    setValue( this.comboSectPerCyl, fmt.getSectorsPerCylinder() );
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


  private static void setValue( JComboBox combo, int value )
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
    if( this.btnFmt800Ki4.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_800K_I4;
    } else if( this.btnFmt780K.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_780K;
    } else if( this.btnFmt780Ki2.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_780K_I2;
    } else if( this.btnFmt780Ki3.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_780K_I3;
    } else if( this.btnFmt780Ki3ds.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_780K_I3_DATESTAMPER;
    } else if( this.btnFmt720K.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_720K;
    } else if( this.btnFmt711Ki5.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_711K_I5_BASDOS;
    } else if( this.btnFmt624K.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_624K;
    } else if( this.btnFmt400K.isSelected() ) {
      fmt = FloppyDiskFormat.FMT_400K;
    }
    if( fmt != null ) {
      setFmtDetailsFields( fmt );
    }
    updFmtDetailsFieldsEnabled();
  }


  private void updFmtDetailsFieldsEnabled()
  {
    boolean state = this.btnFmtEtc.isSelected();
    this.labelSides.setEnabled( state );
    this.comboSides.setEnabled( state );
    this.labelCyls.setEnabled( state );
    this.comboCyls.setEnabled( state );
    this.labelSysTracks.setEnabled( state );
    this.spinnerSysTracks.setEnabled( state );
    this.labelSectPerCyl.setEnabled( state );
    this.comboSectPerCyl.setEnabled( state );
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
    this.btnDateStamper.setEnabled( state );
  }


  private void updMaxInterleave()
  {
    if( this.modelInterleave != null ) {
      int maxInterleave = getIntValue( this.comboSectPerCyl ) - 1;
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
