/*
 * (c) 2008-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Factory fuer GUI-Objekte
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.ListModel;
import javax.swing.SpinnerModel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import jkcemu.Main;


public class GUIFactory
{
  public static final String PROP_LARGE_SYMBOLS = "jkcemu.symbols.large";


  /*
   * Diese Klasse dient nur zur Unterscheidung,
   * ob eine JTextArea Programmcode (Instanz von CodeArea)
   * oder allgemeinen Text (keine Instanz von CodeArea) anzeigt.
   */
  public static class CodeArea extends JTextArea
  {
    public CodeArea( int rows, int cols )
    {
      super( rows, cols );
    }

    public CodeArea()
    {
      // leer
    }
  };


  public static class RelImgResourceButton extends JButton
  {
    private String relImgResource;
    private String text;

    public RelImgResourceButton(
			String relImgResource,
			String text,
			Icon   icon )
    {
      super( icon );
      this.relImgResource = relImgResource;
      this.text           = text;
      setToolTipText( text );
    }

    public RelImgResourceButton( String relImgResource, String text )
    {
      super( text );
      this.relImgResource = relImgResource;
      this.text           = text;
    }

    public String getRelImgResource()
    {
      return this.relImgResource;
    }

    @Override
    public void setIcon( Icon icon )
    {
      super.setIcon( icon );
      if( icon != null ) {
	setText( null );
	setToolTipText( this.text );
      } else {
	setText( this.text );
	setToolTipText( null );
      }
    }
  };


  public static class StyledLabel extends JLabel
  {
    private String fontName    = null;
    private int    fontStyle   = Font.PLAIN;
    private float  relFontSize = 0F;

    public StyledLabel(
		String text,
		String fontName,
		int    fontStyle,
		float  relFontSize )
    {
      super( text );
      this.fontName    = fontName;
      this.fontStyle   = fontStyle;
      this.relFontSize = relFontSize;
      setFont( UIManager.getLookAndFeelDefaults().getFont( "Label.font" ) );
    }

    @Override
    public void setFont( Font font )
    {
      if( (font != null)
	  && (this.fontName != null)
	  && (this.relFontSize > 0F) )
      {
	super.setFont(
		new Font(
			this.fontName,
			this.fontStyle,
			Math.round( font.getSize2D() * this.relFontSize ) ) );
      }
    }
  };


  private static final String SMALL_IMG_RES_PREFIX = "/images/s/";
  private static final String LARGE_IMG_RES_PREFIX = "/images/x/";

  private static volatile String  imgResPrefix       = SMALL_IMG_RES_PREFIX;
  private static volatile Integer tableRowHeightDiff = null;
  private static volatile Integer treeRowHeightDiff  = null;


  public static JButton createButton( Icon icon )
  {
    return initFont( new JButton( icon ) );
  }


  public static JButton createButton( String text )
  {
    return initFont( new JButton( text ) );
  }


  public static JButton createButtonAdd()
  {
    return createButton( "Hinzuf\u00FCgen" );
  }


  public static JButton createButtonApply()
  {
    return createButton( "Anwenden" );
  }


  public static JButton createButtonCancel()
  {
    return createButton( "Abbrechen" );
  }


  public static JButton createButtonClose()
  {
    return createButton( EmuUtil.TEXT_CLOSE );
  }


  public static JButton createButtonEdit()
  {
    return createButton( "Bearbeiten" );
  }


  public static JButton createButtonHelp()
  {
    return createButton( EmuUtil.TEXT_HELP );
  }


  public static JButton createButtonOK()
  {
    return createButton( "OK" );
  }


  public static JButton createButtonRemove()
  {
    return createButton( "Entfernen" );
  }


  public static JButton createButtonReset()
  {
    return createButton( "Zur\u00FCcksetzen" );
  }


  public static JButton createButtonSave()
  {
    return createButton( EmuUtil.TEXT_SAVE );
  }


  public static JCheckBox createCheckBox( String text )
  {
    return initFont( new JCheckBox( text ) );
  }


  public static JCheckBox createCheckBox( String text, boolean selected )
  {
    return initFont( new JCheckBox( text, selected ) );
  }


  public static JCheckBoxMenuItem createCheckBoxMenuItem(
							String  text,
							boolean selected )
  {
    return initFont( new JCheckBoxMenuItem( text, selected ) );
  }


  public static JTextArea createCodeArea()
  {
    return initFont( new CodeArea() );
  }


  public static JTextArea createCodeArea( int rows, int cols )
  {
    return initFont( new CodeArea( rows, cols ) );
  }


  public static <E> JComboBox<E> createComboBox()
  {
    return initFont( new JComboBox<E>() );
  }


  public static <E> JComboBox<E> createComboBox( ComboBoxModel<E> model )
  {
    return initFont( new JComboBox<>( model ) );
  }


  public static <E> JComboBox<E> createComboBox( E[] items )
  {
    return initFont( new JComboBox<>( items ) );
  }


  public static JEditorPane createEditorPane()
  {
    JEditorPane editorPane = new JEditorPane();
    editorPane.putClientProperty(
			JEditorPane.HONOR_DISPLAY_PROPERTIES,
			Boolean.TRUE );
    return initFont( editorPane );
  }


  public static JButton createImageButton(
					Component owner,
					String    imgName,
					String    text )
  {
    JButton btn = null;
    Image   img = Main.getLoadedImage( owner, imgName );
    if( img != null ) {
      btn = createButton( new ImageIcon( img ) );
      btn.setToolTipText( text );
    } else {
      btn = createButton( text );
    }
    return btn;
  }


  public static JLabel createLabel()
  {
    return initFont( new JLabel() );
  }


  public static JLabel createLabel( Icon icon )
  {
    return initFont( new JLabel( icon ) );
  }


  public static JLabel createLabel( String text )
  {
    return initFont( new JLabel( text ) );
  }


  public static JLabel createLabel(
				String text,
				String fontName,
				int    fontStyle,
				float  relFontSize )
  {
    return initFont(
		new StyledLabel( text, fontName, fontStyle, relFontSize ) );
  }


  public static <E> JList<E> createList()
  {
    return initFont( new JList<E>() );
  }


  public static <E> JList<E> createList( ListModel<E> model )
  {
    return initFont( new JList<>( model ) );
  }


  public static JMenu createMenu( String text )
  {
    return initFont( new JMenu( text ) );
  }


  public static JMenuBar createMenuBar( JMenu... menus )
  {
    JMenuBar menuBar = initFont( new JMenuBar() );
    for( JMenu menu : menus ) {
      if( menu != null ) {
	menuBar.add( menu );
      }
    }
    return menuBar;
  }


  public static JMenuItem createMenuItem( String text )
  {
    return initFont( new JMenuItem( text ) );
  }


  public static JPanel createPanel()
  {
    return initFont( new JPanel() );
  }


  public static JPanel createPanel( LayoutManager layoutMngr )
  {
    return initFont( new JPanel( layoutMngr ) );
  }


  public static JPopupMenu createPopupMenu()
  {
    return initFont( new JPopupMenu() );
  }


  public static JProgressBar createProgressBar( int orientation )
  {
    return initFont( new JProgressBar( orientation ) );
  }


  public static JProgressBar createProgressBar(
					int orientation,
					int minValue,
					int maxValue )
  {
    return initFont( new JProgressBar( orientation, minValue, maxValue ) );
  }


  public static JRadioButton createRadioButton()
  {
    return initFont( new JRadioButton() );
  }


  public static JRadioButton createRadioButton( String text )
  {
    return initFont( new JRadioButton( text ) );
  }


  public static JRadioButton createRadioButton(
					String  text,
					boolean selected )
  {
    return initFont( new JRadioButton( text, selected ) );
  }


  public static JRadioButtonMenuItem createRadioButtonMenuItem( String text )
  {
    return initFont( new JRadioButtonMenuItem( text ) );
  }


  public static JRadioButtonMenuItem createRadioButtonMenuItem(
							String  text,
							boolean selected )
  {
    return initFont( new JRadioButtonMenuItem( text, selected ) );
  }


  public static JButton createRelImageResourceButton(
						Component owner,
						String    relResource,
						String    text )
  {
    JButton btn = null;
    Image   img = getRelResourceImage( owner, relResource );
    if( img != null ) {
      btn = new RelImgResourceButton(
				relResource,
				text,
				new ImageIcon( img ) );
    } else {
      btn = new RelImgResourceButton( relResource, text );
    }
    return initFont( btn );
  }


  public static JScrollPane createScrollPane( Component view )
  {
    return initFont( new JScrollPane( view ) );
  }


  public static JScrollPane createScrollPane(
					Component view,
					int       vsbPolicy,
					int       hsbPolicy )
  {
    return initFont( new JScrollPane( view, vsbPolicy, hsbPolicy ) );
  }


  public static JSeparator createSeparator()
  {
    return initFont( new JSeparator() );
  }


  public static JSlider createSlider(
				int orientation,
				int minValue,
				int maxValue,
				int value )
  {
    return initFont( new JSlider( orientation, minValue, maxValue, value ) );
  }


  public static JSpinner createSpinner( SpinnerModel model )
  {
    return initFont( new JSpinner( model ) );
  }


  public static JSplitPane createSplitPane(
				int       orientation,
				boolean   continuousLayout,
				Component leftFld,
				Component rightFld )
  {
    return initFont( new JSplitPane(
				orientation,
				continuousLayout,
				leftFld,
				rightFld ) );
  }


  public static JTabbedPane createTabbedPane()
  {
    return initFont( new JTabbedPane(  JTabbedPane.TOP ) );
  }


  public static JTable createTable( TableModel model )
  {
    JTable table = new JTable( model )
			{
			  @Override
			  public JTableHeader createDefaultTableHeader()
			  {
			    JTableHeader th = super.createDefaultTableHeader();
			    initFont( th );
			    return th;
			  }
			};
    if( tableRowHeightDiff == null ) {
      int  rh = table.getRowHeight();
      Font f  = table.getFont();
      if( (rh > 0) && (f != null) ) {
	int fh = f.getSize();
	if( fh > 0 ) {
	  int d = rh - fh;
	  if( d < 0 ) {
	    d = 0;
	  }
	  tableRowHeightDiff = Integer.valueOf( d );
	}
      }
    }
    return initFont( table );
  }


  public static JTextArea createTextArea()
  {
    return initFont( new JTextArea() );
  }


  public static JTextArea createTextArea( int rows, int cols )
  {
    return initFont( new JTextArea( rows, cols ) );
  }


  public static JTextField createTextField()
  {
    return initFont( new JTextField() );
  }


  public static JTextField createTextField( Document doc, int cols )
  {
    return createTextField( doc, "", cols );
  }


  public static JTextField createTextField(
					Document doc,
					String   text,
					int      cols )
  {
    return initFont( new JTextField( doc, text, cols ) );
  }


  public static JTextField createTextField( int cols )
  {
    return initFont( new JTextField( cols ) );
  }


  public static JTextField createTextField( String text )
  {
    return initFont( new JTextField( text ) );
  }


  public static TitledBorder createTitledBorder( String title )
  {
    TitledBorder border = BorderFactory.createTitledBorder( title );
    Font font = FontMngr.getFont( FontMngr.FontUsage.GENERAL, false );
    if( font != null ) {
      border.setTitleFont( font );
    }
    return border;
  }


  public static JToolBar createToolBar()
  {
    return initFont( new JToolBar() );
  }


  public static JTree createTree( TreeModel model )
  {
    JTree tree = new JTree( model );
    if( treeRowHeightDiff == null ) {
      int  rh = tree.getRowHeight();
      Font f  = tree.getFont();
      if( (rh > 0) && (f != null) ) {
	int fh = f.getSize();
	if( fh > 0 ) {
	  int d = rh - fh;
	  if( d < 0 ) {
	    d = 0;
	  }
	  treeRowHeightDiff = Integer.valueOf( d );
	}
      }
    }
    return initFont( tree );
  }


  public static JViewport createViewport()
  {
    return initFont( new JViewport() );
  }


  public static boolean getLargeSymbolsEnabled( Properties props )
  {
    return EmuUtil.getBooleanProperty(
				props,
				PROP_LARGE_SYMBOLS,
				false );
  }


  public static <T extends Component> T initFont( T c )
  {
    Font font = getUserFont( c );
    if( font != null ) {
      c.setFont( font );
      updRowHeight( c, font );
      Component[] children = EmuUtil.getChildren( c );
      if( children != null ) {
	for( Component child : children ) {
	  initFont( child );
	}
      }
    }
    return c;
  }


  public static void putProperties( Properties props )
  {
    tableRowHeightDiff = null;
    treeRowHeightDiff  = null;
    imgResPrefix       = (getLargeSymbolsEnabled( props ) ?
					LARGE_IMG_RES_PREFIX
					: SMALL_IMG_RES_PREFIX);
  }


  public static void updFont( Component c )
  {
    if( c != null ) {
      Font font      = getUserFont( c );
      Font titleFont = font;
      if( font == null ) {
	if( isCodeFld( c ) ) {
	  font = FontMngr.getDefaultFont( FontMngr.FontUsage.CODE );
	} else if( c instanceof JTableHeader ) {
	  font = UIManager.getLookAndFeelDefaults().getFont( "TableHeader.font" );
	}
      }
      if( font == null ) {
	final UIDefaults uiDefaults = UIManager.getLookAndFeelDefaults();
	final String     prefix     = "javax.swing.J";

	Class<?> cl = c.getClass();
	while( cl != null ) {
	  String className = cl.getName();
	  if( className.startsWith( prefix ) ) {
	    String keyBase = className.substring( prefix.length() );
	    if( !keyBase.isEmpty() ) {
	      font = uiDefaults.getFont( keyBase + ".font" );
	      if( font != null ) {
		break;
	      }
	    }
	  }
	  cl = cl.getSuperclass();
	}
	if( font instanceof UIResource ) {
	  /*
	   * Wenn vorher eine Nicht-UIResource als Font gesetzt war,
	   * funktioniert das Zuruckschalten auf den Standard-Font (UIResource)
	   * nicht bei allen Komponenten korrekt.
	   */
	  Font oldFont = c.getFont();
	  if( oldFont != null ) {
	    if( !(oldFont instanceof UIResource) ) {
	      font = new Font( font.getName(), font.getStyle(), font.getSize() );
	    }
	  }
	}
      }
      if( font != null ) {
	c.setFont( font );
	updRowHeight( c, font );
	if( c instanceof JComponent ) {
	  Border border = ((JComponent) c).getBorder();
	  if( border != null ) {
	    if( border instanceof TitledBorder ) {
	      if( titleFont == null ) {
		titleFont = UIManager.getLookAndFeelDefaults().getFont(
							"TitledBorder.font" );
	      }
	      if( titleFont == null ) {
		titleFont = font;
	      }
	      ((TitledBorder) border).setTitleFont( titleFont );
	    }
	  }
	}
      }
    }
  }


  public static void updIcon( Component c )
  {
    if( c != null ) {
      if( c instanceof RelImgResourceButton ) {
	String relResource = ((RelImgResourceButton) c).getRelImgResource();
	if( relResource != null ) {
	  Image img = getRelResourceImage( c, relResource );
	  if( img != null ) {
	    ((RelImgResourceButton) c).setIcon( new ImageIcon( img ) );
	  } else {
	    ((RelImgResourceButton) c).setIcon( null );
	  }
	}
      }
    }
  }


	/* --- private Methoden --- */

  private static Image getRelResourceImage(
				Component c,
				String    relResource )
  {
    return Main.getLoadedImage( c, imgResPrefix + relResource );
  }


  private static Font getUserFont( Component c )
  {
    FontMngr.FontUsage fontUsage = FontMngr.FontUsage.GENERAL;
    if( c != null ) {
      if( c instanceof JMenuItem ) {
	fontUsage = FontMngr.FontUsage.MENU;
      } else if( isCodeFld( c ) ) {
	fontUsage = FontMngr.FontUsage.CODE;
      } else if( c instanceof JEditorPane ) {
	fontUsage = FontMngr.FontUsage.HTML;
      } else if( c instanceof JTextComponent ) {
	fontUsage = FontMngr.FontUsage.INPUT;
      }
    }
    return FontMngr.getFont( fontUsage, false );
  }


  private static boolean isCodeFld( Component c )
  {
    return (c instanceof CodeArea)
		|| (c instanceof CharPageFld)
		|| (c instanceof HexCharFld);
  }


  private static void updRowHeight( Component c, Font font )
  {
    if( c instanceof JTable ) {
      Integer v = tableRowHeightDiff;
      int     d = (v != null ? v.intValue() : 0);
      ((JTable) c).setRowHeight( font.getSize() + d );
    }
    if( c instanceof JTree ) {
      JTree   t = (JTree) c;
      Integer v = treeRowHeightDiff;
      int     d = (v != null ? v.intValue() : 0);
      t.setRowHeight( Math.max( font.getSize() + d, 12 ) );
      TreeCellRenderer r = t.getCellRenderer();
      if( r != null ) {
	if( r instanceof Component ) {
	  ((Component) r).setFont( font );
	}
      }
    }
  }


	/* --- Konstruktor --- */

  private GUIFactory()
  {
    // nicht instanziierbar
  }
}
