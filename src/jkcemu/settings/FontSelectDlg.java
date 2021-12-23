/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Auswahldialog fuer Schriften
 */

package jkcemu.settings;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.FontMngr;
import jkcemu.base.GUIFactory;


public class FontSelectDlg extends BaseDlg implements ListSelectionListener
{
  private static final String EXAMPLE_TEXT = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n"
					+ "abcdefghijklmnopqrstuvwxyz\n"
					+ "0123 4567 89 +-*/=()_:;#~?";

  private Integer[] fontSizes = {
			6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
			17, 18, 20, 22, 23, 24, 25, 26, 28, 30 };

  private static final String[] EMPTY_STRING_ARRAY          = {};
  private static volatile String[] allFontFamilies          = null;
  private static volatile String[] monospacedFontFamilies   = null;
  private static volatile String[] proportionalFontFamilies = null;
  private static volatile boolean  fontLoadingStarted       = false;

  private Font               approvedFont;
  private Font               preselectedFont;
  private Font               selectedFont;
  private Dimension          prefFontFamilyListSize;
  private JComboBox<Integer> comboFontSize;
  private JCheckBox          cbBold;
  private JCheckBox          cbItalic;
  private JRadioButton       rbAllFonts;
  private JRadioButton       rbMonospacedFonts;
  private JRadioButton       rbProportionalFonts;
  private JButton            btnApply;
  private JButton            btnCancel;
  private JList<String>      listFontFamily;
  private JTextArea          fldFontExample;


  public static Font showDlg(
			Window             owner,
			FontMngr.FontUsage fontUsage,
			Font               preselectedFont )
  {
    FontSelectDlg dlg = new FontSelectDlg(
					owner,
					fontUsage,
					preselectedFont );
    dlg.setVisible( true );
    return dlg.approvedFont;
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    updFontExample();
  }



	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( (src == this.rbAllFonts)
	|| (src == this.rbMonospacedFonts)
	|| (src == this.rbProportionalFonts) )
    {
      rv = true;
      doFontTypeSelection();
    }
    else if( (src == this.comboFontSize)
	     || (src == this.cbBold)
	     || (src == this.cbItalic) )
    {
      rv = true;
      updFontExample();
    }
    else if( src == this.btnApply ) {
      rv = true;
      doApply();
    }
    else if( src == this.btnCancel ) {
      rv                = true;
      this.approvedFont = null;
      doClose();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.listFontFamily.removeListSelectionListener( this );
      this.rbAllFonts.removeActionListener( this );
      this.rbMonospacedFonts.removeActionListener( this );
      this.rbProportionalFonts.removeActionListener( this );
      this.cbBold.removeActionListener( this );
      this.cbItalic.removeActionListener( this );
      this.comboFontSize.removeActionListener( this );
      this.btnApply.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( e.getWindow() == this ) {
      this.listFontFamily.requestFocus();
      if( this.preselectedFont != null ) {
	String family = this.preselectedFont.getFamily();
	if( family != null ) {
	  this.listFontFamily.setSelectedValue( family, true );
	}
      }
    }
  }


	/* --- Konstruktor --- */

  private FontSelectDlg(
		Window             owner,
		FontMngr.FontUsage fontUsage,
		Font               preselectedFont )
  {
    super( owner, "Auswahl Schrift" );
    this.approvedFont = null;
    this.selectedFont = null;
    if( preselectedFont == null ) {
      preselectedFont = FontMngr.getDefaultFont( fontUsage );
    }
    this.preselectedFont = preselectedFont;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.NORTHEAST,
					GridBagConstraints.VERTICAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    add( GUIFactory.createLabel( "Schriftart:" ), gbc );

    this.listFontFamily = GUIFactory.createList();
    this.listFontFamily.setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION );
    gbc.anchor      = GridBagConstraints.NORTHWEST;
    gbc.insets.left = 5;
    gbc.fill        = GridBagConstraints.BOTH;
    gbc.weightx     = 1.0;
    gbc.weighty     = 0.5;
    gbc.gridwidth   = 1;
    gbc.gridheight  = 7;
    gbc.gridx++;
    add( GUIFactory.createScrollPane( this.listFontFamily ), gbc );

    ButtonGroup grpFontType = new ButtonGroup();

    this.rbAllFonts = GUIFactory.createRadioButton(
			"Alle Schriftarten",
			!fontUsage.equals( FontMngr.FontUsage.CODE ) );
    grpFontType.add( this.rbAllFonts );
    gbc.insets.left   = 15;
    gbc.insets.right  = 15;
    gbc.insets.bottom = 0;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.gridwidth     = 2;
    gbc.weightx       = 0.0;
    gbc.weighty       = 0.0;
    gbc.gridheight    = 1;
    gbc.gridx++;
    add( this.rbAllFonts, gbc );

    this.rbMonospacedFonts = GUIFactory.createRadioButton(
			"Nur Schriften mit gleicher Zeichenbreite",
			fontUsage.equals( FontMngr.FontUsage.CODE ) );
    grpFontType.add( this.rbMonospacedFonts );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.rbMonospacedFonts, gbc );

    this.rbProportionalFonts = GUIFactory.createRadioButton(
				"Nur Proportionalschriften" );
    grpFontType.add( this.rbProportionalFonts );
    gbc.gridy++;
    add( this.rbProportionalFonts, gbc );

    this.cbBold    = GUIFactory.createCheckBox( "Fett" );
    gbc.insets.top = 20;
    gbc.gridy++;
    add( this.cbBold, gbc );

    this.cbItalic = GUIFactory.createCheckBox( "Kursiv" );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.cbItalic, gbc );

    gbc.anchor        = GridBagConstraints.WEST;
    gbc.insets.top    = 20;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = 1;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Schriftgr\u00F6\u00DFe:" ), gbc );

    this.comboFontSize = GUIFactory.createComboBox( fontSizes );
    this.comboFontSize.setEditable( false );
    this.comboFontSize.setSelectedItem( FontMngr.DEFAULT_FONT_SIZE );
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.comboFontSize, gbc );

    gbc.anchor       = GridBagConstraints.NORTHEAST;
    gbc.insets.top   = 5;
    gbc.insets.right = 5;
    gbc.gridx        = 0;
    gbc.gridy += 7;
    add( GUIFactory.createLabel( "Beispiel:" ), gbc );

    this.fldFontExample = new JTextArea( 10, 0 );
    this.fldFontExample.setEditable( false );

    gbc.anchor        = GridBagConstraints.NORTHWEST;
    gbc.fill          = GridBagConstraints.BOTH;
    gbc.weightx       = 1.0;
    gbc.weighty       = 0.5;
    gbc.insets.left   = 5;
    gbc.insets.right  = 15;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx++;
    add( GUIFactory.createScrollPane( this.fldFontExample ), gbc );

    JPanel panelBtn   = GUIFactory.createPanel(
				new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.weighty       = 0.0;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnApply = GUIFactory.createButtonOK();
    this.btnApply.setEnabled( false );
    panelBtn.add( this.btnApply );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Vorbelegung
    if( (allFontFamilies != null)
	&& (monospacedFontFamilies != null)
	&& (proportionalFontFamilies != null) )
    {
      updFontFamilyList();
      if( preselectedFont != null ) {
	if( (indexOfFontFamily( preselectedFont ) < 0)
	    && !this.rbAllFonts.isSelected() )
	{
	  this.rbAllFonts.setSelected( true );
	  updFontFamilyList();
	  if( indexOfFontFamily( preselectedFont ) < 0 ) {
	    this.rbMonospacedFonts.setSelected( true );
	    updFontFamilyList();
	  }
	}
	this.cbBold.setSelected( preselectedFont.isBold() );
	this.cbItalic.setSelected( preselectedFont.isItalic() );
	this.comboFontSize.setSelectedItem( preselectedFont.getSize() );
      } else {
	Font font = this.fldFontExample.getFont();
	if( font != null ) {
	  this.comboFontSize.setSelectedItem( font.getSize() );
	}
      }
    } else {
      this.rbAllFonts.setSelected( true );
      this.listFontFamily.setListData(
		GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getAvailableFontFamilyNames() );
      if( !fontLoadingStarted ) {
	fontLoadingStarted = true;
	final Component c  = this;
	(new Thread(
		Main.getThreadGroup(),
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    loadFontFamilies( c );
		  }
		},
		"JKCEMU font loader" )).start();
      }
    }


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    this.fldFontExample.setRows( 0 );


    // Listener
    this.listFontFamily.addListSelectionListener( this );
    this.rbAllFonts.addActionListener( this );
    this.rbMonospacedFonts.addActionListener( this );
    this.rbProportionalFonts.addActionListener( this );
    this.cbBold.addActionListener( this );
    this.cbItalic.addActionListener( this );
    this.comboFontSize.addActionListener( this );
    this.btnApply.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- private Methoden --- */

  private void doApply()
  {
    if( this.selectedFont != null ) {
      this.approvedFont = this.selectedFont;
      doClose();
    }
  }


  private void doFontTypeSelection()
  {
    if( (allFontFamilies != null)
	&& (monospacedFontFamilies != null)
	&& (proportionalFontFamilies != null) )
    {
      updFontFamilyList();
    } else {
      if( !this.rbAllFonts.isSelected() ) {
	this.rbAllFonts.setSelected( true );
	BaseDlg.showInfoDlg(
		this,
		"Die Ermittlung der Zeichenbreiten aller Schriften"
			+ " zur Einteilung in\n"
			+ "Proportional- und Nicht-Proportionalschriften"
			+ " l\u00E4uft noch.\n"
			+ "Aus diesem Grund k\u00F6nnen Sie noch nicht"
			+ " umschalten." );
      }
    }
  }


  private int indexOfFontFamily( Font font )
  {
    int rv = -1;
    if( font != null ) {
      String            family = font.getFamily();
      ListModel<String> model  = this.listFontFamily.getModel();
      if( (family != null) && (model != null) ) {
	int n = model.getSize();
	for( int i = 0; i < n; i++ ) {
	  String s = model.getElementAt( i );
	  if( s != null ) {
	    if( s.equals( family ) ) {
	      rv = i;
	      break;
	    }
	  }
	}
      }
    }
    return rv;
  }


  private static void loadFontFamilies( Component c )
  {
    try {
      /*
       * Beispieltext zur Ermittlung von Nicht-Proportionalschriften
       * erzeugen
       */
      String[] charsAsString = new String[ 0x7E - 0x20 ];
      char     ch            = '\u0020';
      int      idx           = 0;
      while( idx < charsAsString.length ) {
	if( ch != 'W' ) {
	  charsAsString[ idx++ ] = Character.toString( ch );
	}
	ch++;
      }
      java.util.List<String> allList          = new ArrayList<>();
      java.util.List<String> monospacedList   = new ArrayList<>();
      java.util.List<String> proportionalList = new ArrayList<>();
      for( String fontFamily : GraphicsEnvironment
				.getLocalGraphicsEnvironment()
				.getAvailableFontFamilyNames() )
      {
	if( !fontFamily.equalsIgnoreCase( Font.DIALOG )
	    && !fontFamily.equalsIgnoreCase( Font.DIALOG_INPUT )
	    && !fontFamily.equalsIgnoreCase( Font.MONOSPACED )
	    && !fontFamily.equalsIgnoreCase( Font.SANS_SERIF )
	    && !fontFamily.equalsIgnoreCase( Font.SERIF ) )
	{
	  FontMetrics fm = c.getFontMetrics(
				new Font(
					fontFamily,
					Font.PLAIN,
					FontMngr.DEFAULT_FONT_SIZE ) );
	  if( fm != null ) {
	    int w = fm.stringWidth( "W" );
	    if( w > 0 ) {
	      boolean monospaced = true;
	      for( int i = 1; i < charsAsString.length; i++ ) {
		if( fm.stringWidth( charsAsString[ i ] ) != w ) {
		  monospaced = false;
		  break;
		}
	      }
	      if( monospaced ) {
		monospacedList.add( fontFamily );
	      } else {
		proportionalList.add( fontFamily );
	      }
	      allList.add( fontFamily );
	    }
	  } else {
	    allList.add( fontFamily );
	  }
	}
      }
      allFontFamilies = allList.toArray(
				new String[ allList.size() ] );
      monospacedFontFamilies = monospacedList.toArray(
				new String[ monospacedList.size() ] );
      proportionalFontFamilies = proportionalList.toArray(
				new String[ proportionalList.size() ] );
      try {
	Arrays.sort( allFontFamilies );
	Arrays.sort( monospacedFontFamilies );
	Arrays.sort( proportionalFontFamilies );
      }
      catch( ClassCastException ex ) {}
    }
    catch( ArrayStoreException ex ) {
      allFontFamilies          = EMPTY_STRING_ARRAY;
      monospacedFontFamilies   = EMPTY_STRING_ARRAY;
      proportionalFontFamilies = EMPTY_STRING_ARRAY;
    }
  }


  private void updFontExample()
  {
    Font    font   = null;
    String  family = this.listFontFamily.getSelectedValue();
    int     size   = 0;
    Object  item   = this.comboFontSize.getSelectedItem();
    if( item != null ) {
      if( item instanceof Number ) {
	size = ((Number) item).intValue();
      }
    }
    if( (family != null) && (size > 0) ) {
      int style = 0;
      if( this.cbBold.isSelected() ) {
	style |= Font.BOLD;
      }
      if( this.cbItalic.isSelected() ) {
	style |= Font.ITALIC;
      }
      font = new Font( family, style != 0 ? style : Font.PLAIN, size );
    }
    if( font != null ) {
      this.selectedFont = font;
      this.fldFontExample.setFont( font );
      this.fldFontExample.setText( EXAMPLE_TEXT );
      this.btnApply.setEnabled( true );
    } else {
      this.selectedFont = null;
      this.fldFontExample.setText( "" );
      this.btnApply.setEnabled( false );
    }
  }


  private void updFontFamilyList()
  {
    String oldValue = this.listFontFamily.getSelectedValue();
    if( this.rbMonospacedFonts.isSelected() ) {
      this.listFontFamily.setListData(
		monospacedFontFamilies != null ?
				monospacedFontFamilies
				: EMPTY_STRING_ARRAY );
    } else if( this.rbProportionalFonts.isSelected() ) {
      this.listFontFamily.setListData(
		proportionalFontFamilies != null ?
				proportionalFontFamilies
				: EMPTY_STRING_ARRAY );
    } else {
      this.listFontFamily.setListData(
		allFontFamilies != null ?
				allFontFamilies
				: EMPTY_STRING_ARRAY );
    }
    if( oldValue != null ) {
      this.listFontFamily.setSelectedValue( oldValue, true );
    }
  }
}
