/* JOrbis
 * Copyright (C) 2000 ymnk, JCraft,Inc.
 *  
 * Written by: 2000 ymnk<ymnk@jcraft.com>
 *   
 * Many thanks to 
 *   Monty <monty@xiph.org> and 
 *   The XIPHOPHORUS Company http://www.xiph.org/ .
 * JOrbis has been based on their awesome works, Vorbis codec.
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.jcraft.jorbis;

class Lpc {
    // en/decode lookups
    Drft fft = new Drft();

    // Autocorrelation LPC coeff generation algorithm invented by
    // N. Levinson in 1947, modified by J. Durbin in 1959.

    // Input : n elements of time doamin data
    // Output: m lpc coefficients, excitation energy

    // Input : n element envelope spectral curve
    // Output: m lpc coefficients, excitation energy

    void init(int mapped) {
        //memset(l,0,sizeof(lpc_lookup));

        // we cheat decoding the LPC spectrum via FFTs
        fft.init(mapped * 2);
    }

    // One can do this the long way by generating the transfer function in
    // the time domain and taking the forward FFT of the result.  The
    // results from direct calculation are cleaner and faster.
    //
    // This version does a linear curve generation and then later
    // interpolates the log curve from the linear curve.

    /*
  // subtract or add an lpc filter to data.  Vorbis doesn't actually use this.

  static void lpc_residue(float[] coeff, float[] prime,int m,
			  float[] data, int n){

    // in: coeff[0...m-1] LPC coefficients 
    //     prime[0...m-1] initial values 
    //     data[0...n-1] data samples 
    // out: data[0...n-1] residuals from LPC prediction

    float[] work=new float[m+n];
    float y;

    if(prime==null){
      for(int i=0;i<m;i++){
	work[i]=0;
      }
    }
    else{
      for(int i=0;i<m;i++){
	work[i]=prime[i];
      }
    }

    for(int i=0;i<n;i++){
      y=0;
      for(int j=0;j<m;j++){
	y-=work[i+j]*coeff[m-j-1];
      }
      work[i+m]=data[i];
      data[i]-=y;
    }
  }

  static void lpc_predict(float[] coeff, float[] prime,int m,
			  float[] data, int n){

    // in: coeff[0...m-1] LPC coefficients 
    //     prime[0...m-1] initial values (allocated size of n+m-1)
    //     data[0...n-1] residuals from LPC prediction   
    // out: data[0...n-1] data samples

    int o,p;
    float y;
    float[] work=new float[m+n];

    if(prime==null){
      for(int i=0;i<m;i++){
	work[i]=0.f;
      }
    }
    else{
      for(int i=0;i<m;i++){
	work[i]=prime[i];
      }
    }

    for(int i=0;i<n;i++){
      y=data[i];
      o=i;
      p=m;
      for(int j=0;j<m;j++){
	y-=work[o++]*coeff[--p];
      }
      data[i]=work[o]=y;
    }
  }
*/
}
