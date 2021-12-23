/*
 * (c) 2016-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Reduzieren/Indexieren der Farben
 */

package jkcemu.image;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.util.EventObject;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import jkcemu.base.BaseDlg;
import jkcemu.base.UserInputException;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.file.FileNameFld;


public class IndexColorsDlg extends BaseDlg
{
  private static Dithering.Algorithm[] dithAlgorithms = {
				Dithering.Algorithm.FLOYD_STEINBERG,
				Dithering.Algorithm.SIERRA3,
				Dithering.Algorithm.ATKINSON };

  private static final int MODE_REDUCE_COLORS        = 0;
  private static final int MODE_TO_A5105_COLORS      = 1;
  private static final int MODE_TO_KC854HIRES_COLORS = 2;
  private static final int MODE_IMPORT_COLORTAB      = 3;

  private static int             lastColorTabMode     = MODE_REDUCE_COLORS;
  private static int             lastMaxColors        = 256;
  private static int             lastTransparencyMode = 0;
  private static int             lastDitheringIndex   = 0;
  private static File            lastColorTabFile     = null;
  private static IndexColorModel lastImportedColorTab = null;

  private BufferedImage      image;
  private BufferedImage      appliedImage;
  private IndexColorModel    importedICM;
  private JRadioButton       rbReduceColors;
  private JRadioButton       rbToA5105Colors;
  private JRadioButton       rbToKC854HiresColors;
  private JRadioButton       rbImportColorTab;
  private JRadioButton       rbTranspToWhite;
  private JRadioButton       rbTranspToBlack;
  private JRadioButton       rbTranspKeep;
  private JLabel             labelMaxColors;
  private JLabel             labelMaxColorsInfo;
  private JLabel             labelTranspColor;
  private JLabel             labelDithering;
  private JComboBox<Integer> comboMaxColors;
  private JComboBox<Object>  comboDithering;
  private FileNameFld        fldColorTabFile;
  private JButton            btnColorTabSelect;
  private JButton            btnApply;
  private JButton            btnCancel;


  public static BufferedImage showDlg( Window owner, BufferedImage image )
  {
    IndexColorsDlg dlg = new IndexColorsDlg( owner, image );
    dlg.setVisible( true );
    return dlg.appliedImage;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    try {
      Object  src = e.getSource();
      if( (src == this.rbReduceColors)
	  || (src == this.rbToA5105Colors)
	  || (src == this.rbToKC854HiresColors)
	  || (src == this.rbImportColorTab) )
      {
	rv = true;
	updFieldsEnabled();
      } else if( src == this.btnColorTabSelect ) {
	rv = true;
	doImportColorTabFile();
      } else if( src == this.btnApply ) {
	rv = true;
	doApply();
      } else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      }
    }
    catch( IOException | UserInputException ex ) {
      BaseDlg.showErrorDlg( this, ex );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.btnColorTabSelect.removeActionListener( this );
      this.rbReduceColors.removeActionListener( this );
      this.rbToA5105Colors.removeActionListener( this );
      this.rbToKC854HiresColors.removeActionListener( this );
      this.rbImportColorTab.removeActionListener( this );
      this.btnApply.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private IndexColorsDlg( Window owner, BufferedImage image )
  {
    super( owner, "Farben reduzieren und indexieren" );
    this.image        = image;
    this.appliedImage = null;
    this.importedICM  = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );


    // Farbpalette
    JPanel panelColorTab = GUIFactory.createPanel( new GridBagLayout() );
    panelColorTab.setBorder(
		GUIFactory.createTitledBorder( "Farbpalette" ) );
    add( panelColorTab, gbc );

    GridBagConstraints gbcColorTab = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpColorTab = new ButtonGroup();

    this.rbReduceColors = GUIFactory.createRadioButton(
			"Farbpalette ermitteln",
			lastColorTabMode == MODE_REDUCE_COLORS );
    grpColorTab.add( this.rbReduceColors );
    panelColorTab.add( this.rbReduceColors, gbcColorTab );

    this.labelMaxColors = GUIFactory.createLabel( "Max. Anzahl Farben:" );
    gbcColorTab.insets.top  = 0;
    gbcColorTab.insets.left = 50;
    gbcColorTab.gridwidth   = 1;
    gbcColorTab.gridy++;
    panelColorTab.add( this.labelMaxColors, gbcColorTab );

    DefaultComboBoxModel<Integer> cbm = new DefaultComboBoxModel<Integer>()
		{
		  @Override
		  public void addElement( Integer v )
		  {
		    if( v != null ) {
		      if( (v.intValue() >= 2) && (v.intValue() <= 256) ) {
			super.addElement( v );
		      }
		    }
		  }

		  @Override
		  public void insertElementAt( Integer v, int idx )
		  {
		    if( v != null ) {
		      if( (v.intValue() >= 2) && (v.intValue() <= 256) ) {
			super.insertElementAt( v, idx );
		      }
		    }
		  }
		};
    cbm.addElement( 256 );
    cbm.addElement( 64 );
    cbm.addElement( 32 );
    cbm.addElement( 27 );
    cbm.addElement( 16 );
    cbm.addElement( 8 );
    cbm.addElement( 4 );
    cbm.addElement( 2 );
    this.comboMaxColors = GUIFactory.createComboBox( cbm );
    this.comboMaxColors.setEditable( true );
    this.comboMaxColors.setSelectedItem( lastMaxColors );
    gbcColorTab.insets.left = 5;
    gbcColorTab.gridx++;
    panelColorTab.add( this.comboMaxColors, gbcColorTab );

    this.labelMaxColorsInfo = GUIFactory.createLabel(
			"inkl. Farbe f\u00FCr Transparenz" );
    gbcColorTab.gridx++;
    panelColorTab.add( this.labelMaxColorsInfo, gbcColorTab );

    this.rbToA5105Colors = GUIFactory.createRadioButton(
			"A5105 (16 Farben)",
			lastColorTabMode == MODE_TO_A5105_COLORS );
    grpColorTab.add( this.rbToA5105Colors );
    gbcColorTab.insets.left = 5;
    gbcColorTab.gridwidth   = GridBagConstraints.REMAINDER;
    gbcColorTab.gridx       = 0;
    gbcColorTab.gridy++;
    panelColorTab.add( this.rbToA5105Colors, gbcColorTab );

    this.rbToKC854HiresColors = GUIFactory.createRadioButton(
			"KC85/4 HIRES (4 Farben)",
			lastColorTabMode == MODE_TO_KC854HIRES_COLORS );
    grpColorTab.add( this.rbToKC854HiresColors );
    gbcColorTab.gridy++;
    panelColorTab.add( this.rbToKC854HiresColors, gbcColorTab );

    this.rbImportColorTab = GUIFactory.createRadioButton(
			"Farbpalette importieren",
			lastColorTabMode == MODE_IMPORT_COLORTAB );
    grpColorTab.add( this.rbImportColorTab );
    gbcColorTab.gridy++;
    panelColorTab.add( this.rbImportColorTab, gbcColorTab );

    this.fldColorTabFile    = new FileNameFld();
    gbcColorTab.fill        = GridBagConstraints.HORIZONTAL;
    gbcColorTab.weightx     = 1.0;
    gbcColorTab.insets.left = 50;
    gbcColorTab.gridwidth   = 3;
    gbcColorTab.gridy++;
    panelColorTab.add( this.fldColorTabFile, gbcColorTab );

    this.btnColorTabSelect = GUIFactory.createRelImageResourceButton(
						this,
						"file/open.png",
						EmuUtil.TEXT_SELECT );
    gbcColorTab.fill          = GridBagConstraints.NONE;
    gbcColorTab.weightx       = 0.0;
    gbcColorTab.insets.left   = 5;
    gbcColorTab.insets.bottom = 5;
    gbcColorTab.gridwidth     = 1;
    gbcColorTab.gridx += 3;
    panelColorTab.add( this.btnColorTabSelect, gbcColorTab );

    if( (lastColorTabFile != null) && (lastImportedColorTab != null) ) {
      this.fldColorTabFile.setFile( lastColorTabFile );
      this.importedICM = lastImportedColorTab;
    }


    // Optionen
    JPanel panelOpt = GUIFactory.createPanel( new GridBagLayout() );
    panelOpt.setBorder( GUIFactory.createTitledBorder( "Optionen" ) );
    gbc.gridy++;
    add( panelOpt, gbc );

    GridBagConstraints gbcOpt = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.labelTranspColor = GUIFactory.createLabel( "Transparenz:" );
    panelOpt.add( this.labelTranspColor, gbcOpt );

    ButtonGroup grpTransp = new ButtonGroup();

    this.rbTranspToWhite = GUIFactory.createRadioButton(
			"Transparente Bereiche hell f\u00E4rben" );
    grpTransp.add( this.rbTranspToWhite );
    gbcOpt.insets.top = 0;
    gbcOpt.gridwidth  = GridBagConstraints.REMAINDER;
    gbcOpt.gridx++;
    panelOpt.add( this.rbTranspToWhite, gbcOpt );

    this.rbTranspToBlack = GUIFactory.createRadioButton(
			"Transparente Bereiche dunkel f\u00E4rben" );
    grpTransp.add( this.rbTranspToBlack );
    gbcOpt.gridy++;
    panelOpt.add( this.rbTranspToBlack, gbcOpt );

    this.rbTranspKeep = GUIFactory.createRadioButton(
			"Transparenz behalten (1 volltransparente Farbe)" );
    grpTransp.add( this.rbTranspKeep );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridy++;
    panelOpt.add( this.rbTranspKeep, gbcOpt );

    if( lastTransparencyMode < 0 ) {
      this.rbTranspToBlack.setSelected( true );
    } else if( lastTransparencyMode > 0 ) {
      this.rbTranspToWhite.setSelected( true );
    } else {
      this.rbTranspKeep.setSelected( true );
    }

    if( image.getTransparency() == Transparency.OPAQUE ) {
      this.labelTranspColor.setEnabled( false );
      this.rbTranspKeep.setEnabled( false );
      this.rbTranspToBlack.setEnabled( false );
      this.rbTranspToWhite.setEnabled( false );
    }

    this.labelDithering = GUIFactory.createLabel( "Dithering:" );
    gbcOpt.insets.top   = 5;
    gbcOpt.gridx        = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.labelDithering, gbcOpt );

    this.comboDithering = GUIFactory.createComboBox();
    this.comboDithering.setEditable( false );
    this.comboDithering.addItem( "Kein Dithering anwenden" );
    for( Dithering.Algorithm a : dithAlgorithms ) {
      this.comboDithering.addItem( Dithering.getAlgorithmText( a ) );
    }
    try {
      this.comboDithering.setSelectedIndex( lastDitheringIndex );
    }
    catch( IllegalArgumentException ex ) {}
    gbcOpt.gridwidth = GridBagConstraints.REMAINDER;
    gbcOpt.gridx++;
    panelOpt.add( this.comboDithering, gbcOpt );

    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.weightx     = 0.0;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnApply = GUIFactory.createButtonOK();
    panelBtn.add( this.btnApply );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // sonstiges
    updFieldsEnabled();
    this.btnColorTabSelect.addActionListener( this );
    this.rbReduceColors.addActionListener( this );
    this.rbToA5105Colors.addActionListener( this );
    this.rbToKC854HiresColors.addActionListener( this );
    this.rbImportColorTab.addActionListener( this );
    this.btnApply.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- Aktionen --- */

  private void doApply() throws IOException, UserInputException
  {
    if( this.rbReduceColors.isSelected() ) {
      doReduceColors();
      lastColorTabMode = MODE_REDUCE_COLORS;
    } else if( this.rbToA5105Colors.isSelected() ) {
      doApplyIndexColorModel( ImageUtil.getColorModelA5105() );
      lastColorTabMode = MODE_TO_A5105_COLORS;
    } else if( this.rbToKC854HiresColors.isSelected() ) {
      doApplyIndexColorModel( ImageUtil.getColorModelKC854Hires() );
      lastColorTabMode = MODE_TO_KC854HIRES_COLORS;
    } else if( this.rbImportColorTab.isSelected() ) {
      if( (this.fldColorTabFile.getFile() == null)
	  || (this.importedICM == null) )
      {
	doImportColorTabFile();
      }
      if( (this.fldColorTabFile.getFile() != null)
	  && (this.importedICM != null) )
      {
	doApplyIndexColorModel( this.importedICM );
	lastColorTabMode = MODE_IMPORT_COLORTAB;
      }
    }
  }


  private void doApplyIndexColorModel( IndexColorModel icm )
						throws UserInputException
  {
    if( icm != null ) {

      BufferedImage       retImg        = null;
      BufferedImage       srcImg        = removeTransparency( this.image );
      Dithering.Algorithm dithAlgorithm = getSelectedDithAlgorithm();
      if( dithAlgorithm != null ) {
	doClose();
	retImg = Dithering.work( getOwner(), srcImg, icm, dithAlgorithm );
      } else {
	retImg = new BufferedImage(
				srcImg.getWidth(),
				srcImg.getHeight(),
				icm.getMapSize() > 16 ?
					BufferedImage.TYPE_BYTE_INDEXED
					: BufferedImage.TYPE_BYTE_BINARY,
				icm );
	Graphics g = retImg.createGraphics();
	g.drawImage( srcImg, 0, 0, this );
	g.dispose();
	doClose();
      }
      if( retImg != null ) {
	this.appliedImage = retImg;
      }
    }
  }


  public void doImportColorTabFile() throws IOException
  {
    File file = ImageUtil.chooseColorPaletteFile(
					this,
					this.fldColorTabFile.getFile() );
    if( file != null ) {
      IndexColorModel icm = ImageUtil.readColorPaletteFile( file );
      if( icm != null ) {
	this.importedICM = icm;
	this.fldColorTabFile.setFile( file );
	lastImportedColorTab = icm;
	lastColorTabFile     = file;
      }
    }
  }


  private void doReduceColors()
  {
    Object o = this.comboMaxColors.getSelectedItem();
    if( o != null ) {
      int maxColors = 0;
      if( o instanceof Integer ) {
	maxColors = (Integer) o;
      } else {
	try {
	  maxColors = Integer.parseInt( o.toString().trim() );
	}
	catch( Exception ex ) {}
      }
      if( (maxColors >= 2) && (maxColors <= 256) ) {
	int             oldIdxColors  = -1;
	boolean         state = true;
	IndexColorModel icm   = ImageUtil.getIndexColorModel( this.image );
	if( icm != null ) {
	  oldIdxColors = icm.getMapSize();
	  if( oldIdxColors <= maxColors ) {
	    state = BaseDlg.showYesNoDlg(
		this,
		String.format(
			"Das Bild hat bereits ein indexiertes Farbmodell"
				+ " mit %d Farben.\n"
				+ "M\u00F6glicherweise werden nicht alle"
				+ " diese Farben verwendet,\n"
				+ "so dass die Farbpalette vielleicht"
				+ " verkleinert werden kann.\n"
				+ "M\u00F6chten Sie versuchen,"
				+ " die Farbpalette zu verkleinern?",
		oldIdxColors ) );
	  }
	}
	if( state ) {
	  Dithering.Algorithm dithAlgorithm  = getSelectedDithAlgorithm();
	  Color               colorForTransp = getSelectedColorForTransp();
	  lastMaxColors                      = maxColors;
	  this.btnApply.setEnabled( false );
	  doClose();
	  BufferedImage retImg = ColorReducer.work(
					getOwner(),
					"Reduziere Farben...",
					this.image,
					maxColors,
					colorForTransp,
					dithAlgorithm );
	  if( retImg != null ) {
	    if( oldIdxColors > 0 ) {
	      icm = ImageUtil.getIndexColorModel( retImg );
	      if( icm != null ) {
		if( icm.getMapSize() >= oldIdxColors ) {
		  retImg = null;
		  BaseDlg.showErrorDlg(
				getOwner(),
				"Die Anzahl der Farben konnte nicht"
					+ " reduziert werden." );
		}
	      }
	    }
	  }
	  if( retImg != null ) {
	    this.appliedImage = retImg;
	  }
	}
      } else {
	this.comboMaxColors.setSelectedItem( lastMaxColors );
      }
    }
  }


	/* --- private Methoden --- */

  private Color getSelectedColorForTransp()
  {
    Color colorForTransp = null;
    if( this.rbTranspToBlack.isSelected() ) {
      colorForTransp = Color.BLACK;
      lastTransparencyMode = -1;
    } else if( this.rbTranspToWhite.isSelected() ) {
      colorForTransp       = Color.WHITE;
      lastTransparencyMode = 1;
    } else {
      lastTransparencyMode = 0;
    }
    return colorForTransp;
  }


  private Dithering.Algorithm getSelectedDithAlgorithm()
  {
    Dithering.Algorithm dithAlgorithm = null;
    lastDitheringIndex = this.comboDithering.getSelectedIndex();
    if( (lastDitheringIndex > 0)
	&& (lastDitheringIndex <= dithAlgorithms.length) )
    {
      dithAlgorithm = dithAlgorithms[ lastDitheringIndex - 1 ];
    }
    return dithAlgorithm;
  }


  private BufferedImage removeTransparency( BufferedImage srcImg )
						throws UserInputException
  {
    BufferedImage retImg = srcImg;
    if( srcImg.getTransparency() != Transparency.OPAQUE ) {
      Color colorForTransp = getSelectedColorForTransp();
      if( colorForTransp == null ) {
	throw new UserInputException(
		"Bei dieser Funktion ist die Option\n\'"
			+ this.rbTranspKeep.getText()
			+ "\' nicht m\u00F6glich,\n"
			+ "da die Farbpalette keine transparenten Farben"
			+ " enth\u00E4lt.\n"
			+ "W\u00E4hlen Sie bitte die Option\n\'"
			+ this.rbTranspToWhite.getText()
			+ "\' oder\n\'"
			+ this.rbTranspToBlack.getText()
			+ "\' aus!" );
      }
      int w = srcImg.getWidth();
      int h = srcImg.getHeight();
      if( (w > 0) && (h > 0) ) {
	BufferedImage tmpImg = new BufferedImage(
					w,
					h,
					BufferedImage.TYPE_3BYTE_BGR );
	Graphics g = tmpImg.createGraphics();
	g.setColor( colorForTransp );
	g.fillRect( 0, 0, w, h );
	g.drawImage( srcImg, 0, 0, this );
	g.dispose();
	srcImg = tmpImg;
      }
    }
    return srcImg;
  }


  private void updFieldsEnabled()
  {
    boolean state = this.rbReduceColors.isSelected();
    this.labelMaxColors.setEnabled( state );
    this.comboMaxColors.setEnabled( state );
    this.labelMaxColorsInfo.setEnabled(
		state && this.labelTranspColor.isEnabled() );

    state = this.rbImportColorTab.isSelected();
    this.fldColorTabFile.setEnabled( state );
    this.btnColorTabSelect.setEnabled( state );
  }
}
