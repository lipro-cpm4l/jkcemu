/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Farbreduktion mittels modifiziertem OcTree-Verfahren
 *
 * Der OcTree (8 Kinder pro Knoten) stellt eine dreidimensionale
 * Abbildung des Farbraums dar.
 * Der oberste Knoten steht fuer den gesamten Farbraum.
 * Jeder Kindknoten umfasst 1/8 des Farbraums des Elternknotens.
 * Bei 8 Bit Farbtiefe (8 Bit pro Dimension) erreicht der Baum
 * eine Tiefe von 9.
 *
 * Im ersten Schritt werden alle Pixel in den Baum eingetragen,
 * womit jede unterschiedliche Farbe ein Blatt ergibt.
 * Aus Grueden der Performance und des Speicherbedarfs
 * wird der Baum hier allerdings nur bis zur Tiefe 6 aufgebaut,
 * d.h., jeweils 4 eng beieinander liegende Farben
 * (die unteren 2 Bit) werden schon im ersten Schritt zusammengefasst
 * und ergeben zusammen somit nur ein Blatt.
 * Eine Qualitaetseinbusse ist dadurch praktisch nicht erkennbar.
 * Das gilt allerdings nicht, wenn das Bild bereits nicht mehr
 * als die max. gewuenschte Anzahl Farben hat.
 * In dem Fall kann sich durch den nicht voll aufgebauten Baum
 * die Anzahl der Farben nochmals reduzieren,
 * was fuer den Anwender verwirrend und nicht nachvollziehbar ist.
 * Um diesen Effekt zu verweiden, werden parallel zum Baum
 * die Farben auch in einen Set eingetragen., max. jedoch 257.
 * Wenn die Set-Groesse die gewuenschte Farbanzahl nicht uebersteigt,
 * wird die Farbpalette daraus ermittelt.
 * Wenn nicht, erfolgt die Farbreduktion mit Hilfe des Baumes.
 * Dazu werden beginnend mit der untersten Ebene solange
 * die Knoten mit der jeweils kleinsten Pixelanzahl zusammengefasst,
 * bis die max. Farbanzahl (Anzahl der Blaetter) erreicht ist.
 * Wenn in der Ebene alle Knoten zusammengefasst sind
 * und die max. Farbanzahl immer noch ueberschritten wird,
 * ist die naechst hoehere Ebene an der Reihe.
 * Mit dieser Vorgehensweise wird erreicht,
 * dass haeufiger vorkommende Farben nicht mehr verfaelscht werden
 * als weniger vorkommende.
 *
 * Das OcTree-Verfahren liefert bei Reduktion auf 256 Farben sehr gute
 * Ergebnisse. Bei 64 Farben sind die Ergebnisse auch noch gut.
 * Bei Reduktion auf noch weniger Farben laesst allerdings der Kontrast
 * nach und das Bild verblasst.
 * Besonders deutlich ist das bei 16 und weniger Farben zu sehen.
 * Aus diesem Grund wird das OcTree-Verfahren hier folgendermassen
 * modizifiert:
 * In die Farbpalette wird immer die dunkelste im Bild vorkommende
 * Farbe aufgenommen und nur die restlichen Farben mit dem Baum ermittelt.
 * Der Kontrastverlust faellt dadurch wesentlich geringer aus.
 * Zwar ergibt sich damit vorallem bei sehr wenigen Farben
 * eine noch staerkere Farbverfaelschung,
 * jedoch nimmt das menschliche Auge Helligkeitsverfaelschungen
 * wesentlich deutlicher wahr als Farbverfaelschungen.
 * Aus diesem Grund sehen mit der Modifikation die Bilder besser aus.
 */

package jkcemu.image;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;


public class OcTree
{
  // Baum nur bis zur Tiefe 6 aufbauen
  private static final int MIN_COLOR_MASK = 0x04;

  private class Node
  {
    private Node   parent;
    private int    colorMask;
    private int    pixelCnt;
    private long   totalR;
    private long   totalG;
    private long   totalB;
    private Node[] subNodes;

    private Node( Node parent, int colorMask )
    {
      this.parent    = parent;
      this.colorMask = colorMask;
      this.pixelCnt  = 0;
      this.totalR    = 0;
      this.totalG    = 0;
      this.totalB    = 0;
      if( colorMask == MIN_COLOR_MASK ) {
	incLeafCount();
	this.subNodes = null;
      } else {
	this.subNodes = new Node[ 8 ];
	Arrays.fill( this.subNodes, null );
      }
    }

    private void getLeafs( Collection<Node> leafsOut )
    {
      if( this.subNodes != null ) {
	for( int i = 0; i < this.subNodes.length; i++ ) {
	  if( this.subNodes[ i ] != null ) {
	    this.subNodes[ i ].getLeafs( leafsOut );
	  }
	}
      } else {
	leafsOut.add( this );
      }
    }

    private Node getMinPixelsParent( int colorMask )
    {
      Node node = null;
      if( this.subNodes != null ) {
	int pixelCnt = -1;
	for( int i = 0; i < this.subNodes.length; i++ ) {
	  if( this.subNodes[ i ] != null ) {
	    Node node2 = this.subNodes[ i ].getMinPixelsParent( colorMask );
	    if( node2 != null ) {
	      int tmpPixelCnt = node2.pixelCnt;
	      if( (node != null) && (pixelCnt >= 0) ) {
		if( tmpPixelCnt < pixelCnt ) {
		  node     = node2;
		  pixelCnt = tmpPixelCnt;
		}
	      } else {
		node     = node2;
		pixelCnt = tmpPixelCnt;
	      }
	    }
	  }
	}
	if( (node == null)
	    && ((colorMask == 0) || (colorMask == this.colorMask)) )
	{
	  node = this;
	}
      }
      return node;
    }

    private void putPixel( int r, int g, int b )
    {
      this.pixelCnt++;
      this.totalR += r;
      this.totalG += g;
      this.totalB += b;
      if( this.subNodes != null ) {
	int idx = 0;
	if( ((int) r & this.colorMask) != 0 ) {
	  idx += 4;
	}
	if( ((int) g & this.colorMask) != 0 ) {
	  idx += 2;
	}
	if( ((int) b & this.colorMask) != 0 ) {
	  idx++;
	}
	if( this.subNodes[ idx ] == null ) {
	  this.subNodes[ idx ] = new Node( this, this.colorMask >> 1 );
	}
	this.subNodes[ idx ].putPixel( r, g, b );
      }
    }

    private void reduce( int maxColors )
    {
      if( this.subNodes != null ) {
	int n = 0;
	for( int i = 0; i < this.subNodes.length; i++ ) {
	  if( this.subNodes[ i ] != null ) {
	    n++;
	  }
	}
	if( (getLeafCount() - n + 1) >= maxColors ) {
	  for( int i = 0; i < this.subNodes.length; i++ ) {
	    if( this.subNodes[ i ] != null ) {
	      this.subNodes[ i ].reduce( maxColors );
	    }
	  }
	  this.subNodes = null;
	  addLeafCount( 1 - n );	// dieser Konten wird zum Blatt
	} else {
	  /*
	   * Es brauchen nicht alle Subknoten zusammengefasst werden,
	   * da sonst die max. Anzahl an Farben unterschritten wird.
	   */
	  Node node     = null;
	  long totalR   = 0;
	  long totalG   = 0;
	  long totalB   = 0;
	  int  pixelCnt = 0;
	  for( int i = 0; i < this.subNodes.length; i++ ) {
	    node = this.subNodes[ i ];
	    if( node != null ) {
	      node.reduce( maxColors );
	      totalR   += node.totalR;
	      totalG   += node.totalG;
	      totalB   += node.totalB;
	      pixelCnt += node.pixelCnt;
	      this.subNodes[ i ] = null;
	      addLeafCount( -1 );
	      if( getLeafCount() < maxColors ) {
		break;
	      }
	    }
	  }
	  // Blatt fuer die eleminierten Subkonten anlegen
	  if( pixelCnt > 0 ) {
	    node               = new Node( this, MIN_COLOR_MASK );  // Blatt
	    node.colorMask     = (this.colorMask >> 1);
	    node.totalR        = totalR;
	    node.totalG        = totalG;
	    node.totalB        = totalB;
	    node.pixelCnt      = pixelCnt;
	    this.subNodes[ 0 ] = node;
	  }
	}
      }
    }
  };


  private Node         root;
  private Set<Integer> allRGBs;
  private Integer      darkestRGB;
  private Integer      darkestGray;
  private int          leafCnt;

  public OcTree()
  {
    this.root        = new Node( null, 0x80 );
    this.allRGBs     = new TreeSet<>();
    this.darkestRGB  = null;
    this.darkestGray = null;
    this.leafCnt     = 0;
  }


  public void putPixel( int rgb )
  {
    this.root.putPixel( (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF );
    if( this.allRGBs != null ) {
      this.allRGBs.add( rgb & 0x00FFFFFF );
      if( this.allRGBs.size() > 256 ) {
	this.allRGBs = null;
      }
    }
    int grayValue = GrayScaler.toGray( rgb ) & 0xFF;
    if( this.darkestGray != null ) {
      if( grayValue < this.darkestGray.intValue() ) {
	this.darkestRGB  = rgb;
	this.darkestGray = grayValue;
      }
    } else {
      this.darkestRGB  = rgb;
      this.darkestGray = grayValue;
    }
  }


  /*
   * Die Methode reduzierte die Farben und gibt die Farbpalette
   * in Form eines Arrays mit RGB-Werten zurueck.
   */
  public int[] reduceColors( int maxColors )
  {
    int[] rgbs = null;
    if( this.allRGBs != null ) {
      int n = this.allRGBs.size();
      if( (n > 0) && (n <= maxColors) ) {
	rgbs = new int[ n ];
	int idx = 0;
	for( Integer rgb : this.allRGBs ) {
	  if( idx >= n ) {
	    rgbs = null;
	    break;
	  }
	  rgbs[ idx++ ] = 0xFF000000 | rgb.intValue();
	}
      }
    }
    if( rgbs == null ) {


    // Blaetter des Baums ermitteln und daraus die Farbpalette erzeugen


      int maxTreeColors = maxColors;
      if( this.darkestRGB != null ) {
	--maxTreeColors;
      }
      java.util.List<Node> leafs = new ArrayList<>();
      if( maxTreeColors > 0 ) {
	reduceTreeLeafs( maxTreeColors, leafs );
      }
      int colorCnt = leafs.size();
      if( this.darkestRGB != null ) {
	colorCnt++;
      }
      if( colorCnt > maxColors ) {
	colorCnt = maxColors;
      }
      if( colorCnt > 0 ) {
	rgbs    = new int[ colorCnt ];
	int idx = 0;
	for( Node node : leafs ) {
	  if( idx >= rgbs.length ) {
	    break;
	  }
	  double n      = (double) node.pixelCnt;
	  int    r      = (int) Math.round( (double) node.totalR / n );
	  int    g      = (int) Math.round( (double) node.totalG / n );
	  int    b      = (int) Math.round( (double) node.totalB / n );
	  rgbs[ idx++ ] = 0xFF000000
				| (((r << 16) & 0xFF0000)
				| ((g << 8) & 0x00FF00)
				| (b & 0x0000FF));
	}
	if( (idx < rgbs.length) && (this.darkestRGB != null) ) {
	  rgbs[ idx++ ] = 0xFF000000 | this.darkestRGB.intValue();
	}
      }
    }

    // aus Gruenden der Schoenheit eine sortierte Farbpalette erzeugen
    if( rgbs != null ) {
      Arrays.sort( rgbs );
    }
    return rgbs;
  }


	/* --- private Methoden --- */

  private void addLeafCount( int n )
  {
    this.leafCnt += n;
  }


  private int getLeafCount()
  {
    return this.leafCnt;
  }


  private void incLeafCount()
  {
    this.leafCnt++;
  }


  private void reduceTreeLeafs(
			int                  maxLeafs,
			java.util.List<Node> leafsOut )
  {
    int colorMask = MIN_COLOR_MASK;
    while( (this.leafCnt > maxLeafs) && (colorMask < 0x100) ) {
      /*
       * Konten mit der kleinsten Pixelanzahl
       * in der betreffenden Ebene zusammenfassen
       */
      while( this.leafCnt > maxLeafs ) {
	Node node = this.root.getMinPixelsParent( colorMask );
	if( node == null ) {
	  break;
	}
	node.reduce( maxLeafs );
      }
      colorMask <<= 1;
    }
    this.root.getLeafs( leafsOut );
  }
}
