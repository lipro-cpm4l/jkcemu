/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abstracte Klasse fuerr alle KC85-Tastaturen
 */

package jkcemu.emusys.kc85;

import java.lang.*;
import jkcemu.base.*;
import jkcemu.emusys.KC85;


public abstract class AbstractKC85KeyboardFld extends AbstractKeyboardFld
{
  protected KC85 kc85;


  public AbstractKC85KeyboardFld( KC85 kc85, int numKeys )
  {
    super( numKeys );
    this.kc85 = kc85;
  }


  public void fireKOut()
  {
    // leer
  }


  public abstract void updKeySelection( int keyNum );
}

