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

public class DspState {

    Info vi;


    // local lookup storage
//!!  Envelope ve=new Envelope(); // envelope
//float                **window[2][2][2]; // block, leadin, leadout, type
    float[][][][][] window;                 // block, leadin, leadout, type
    //vorbis_look_transform **transform[2];    // block, type
    Object[][] transform;
    CodeBook[] fullbooks;

    // Analysis side code, but directly related to blocking.  Thus it's
    // here and not in analysis.c (which is for analysis transforms only).
    // The init is here because some of it is shared

    // Unike in analysis, the window is only partially applied for each
    // block.  The time domain envelope is not yet handled at the point of
    // calling (as it relies on the previous block).

}
