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

import com.jcraft.jogg.Buffer;

class StaticCodeBook {
    int dim;            // codebook dimensions (elements per vector)
    // 1=implicitly populated values from map column
    // 2=listed arbitrary values
    int entries;        // codebook entries
    int[] lengthlist;     // codeword lengths in bits
    // mapping
    int maptype;        // 0=none
    // The below does a linear, single monotonic sequence mapping.
    int q_min;       // packed 32 bit float; quant value 0 maps to minval

    // additional information for log (dB) mapping; the linear mapping
    // is assumed to actually be values in dB.  encodebias is used to
    // assign an error weight to 0 dB. We have two additional flags:
    // zeroflag indicates if entry zero is to represent -Inf dB; negflag
    // indicates if we're to represent negative linear values in a
    // mirror of the positive mapping.
    int q_delta;     // packed 32 bit float; val 1 - val 0 == delta
    // map == 2: list of dim*entries quantized entry vals
    int q_quant;     // bits: 0 < quant <= 16
    int q_sequencep; // bitflag
    int[] quantlist;  // map == 1: (int)(entries/dim) element column map
    /*
*/

    StaticCodeBook() {
    }

    private static int ilog(int v) {
        int ret = 0;
        while (v != 0) {
            ret++;
            v >>>= 1;
        }
        return (ret);
    }

    // 32 bit float (not IEEE; nonnormalized mantissa +
    // biased exponent) : neeeeeee eeemmmmm mmmmmmmm mmmmmmmm
    // Why not IEEE?  It's just not that important here.

    // unpacks a codebook from the packet buffer into the codebook struct,
    // readies the codebook auxiliary structures for decode
    int unpack(Buffer opb) {
        int i;
        //memset(s,0,sizeof(static_codebook));

        // make sure alignment is correct
        if (opb.read(24) != 0x564342) {
//    goto _eofout;
            return (-1);
        }

        // first the basic parameters
        dim = opb.read(16);
        entries = opb.read(24);
        if (entries == -1) {
//    goto _eofout;
            return (-1);
        }

        // codeword ordering.... length ordered or unordered?
        switch (opb.read(1)) {
            case 0:
                // unordered
                lengthlist = new int[entries];

                // allocated but unused entries?
                if (opb.read(1) != 0) {
                    // yes, unused entries

                    for (i = 0; i < entries; i++) {
                        if (opb.read(1) != 0) {
                            int num = opb.read(5);
                            if (num == -1) {
//            goto _eofout;
                                return (-1);
                            }
                            lengthlist[i] = num + 1;
                        } else {
                            lengthlist[i] = 0;
                        }
                    }
                } else {
                    // all entries used; no tagging
                    for (i = 0; i < entries; i++) {
                        int num = opb.read(5);
                        if (num == -1) {
//          goto _eofout;
                            return (-1);
                        }
                        lengthlist[i] = num + 1;
                    }
                }
                break;
            case 1:
                // ordered
            {
                int length = opb.read(5) + 1;
                lengthlist = new int[entries];

                for (i = 0; i < entries; ) {
                    int num = opb.read(ilog(entries - i));
                    if (num == -1) {
//          goto _eofout;
                        return (-1);
                    }
                    for (int j = 0; j < num; j++, i++) {
                        lengthlist[i] = length;
                    }
                    length++;
                }
            }
            break;
            default:
                // EOF
                return (-1);
        }

        // Do we have a mapping to unpack?
        switch ((maptype = opb.read(4))) {
            case 0:
                // no mapping
                break;
            case 1:
            case 2:
                // implicitly populated value mapping
                // explicitly populated value mapping
                q_min = opb.read(32);
                q_delta = opb.read(32);
                q_quant = opb.read(4) + 1;
                q_sequencep = opb.read(1);

            {
                int quantvals = 0;
                switch (maptype) {
                    case 1:
                        quantvals = maptype1_quantvals();
                        break;
                    case 2:
                        quantvals = entries * dim;
                        break;
                }

                // quantized values
                quantlist = new int[quantvals];
                for (i = 0; i < quantvals; i++) {
                    quantlist[i] = opb.read(q_quant);
                }
                if (quantlist[quantvals - 1] == -1) {
//        goto _eofout;
                    return (-1);
                }
            }
            break;
            default:
//    goto _eofout;
                return (-1);
        }
        // all set
        return (0);
    }

    // there might be a straightforward one-line way to do the below
    // that's portable and totally safe against roundoff, but I haven't
    // thought of it.  Therefore, we opt on the side of caution
    private int maptype1_quantvals() {
        int vals = (int) (Math.floor(Math.pow(entries, 1. / dim)));

        // the above *should* be reliable, but we'll not assume that FP is
        // ever reliable when bitstream sync is at stake; verify via integer
        // means that vals really is the greatest value of dim for which
        // vals^b->bim <= b->entries
        // treat the above as an initial guess
        while (true) {
            int acc = 1;
            int acc1 = 1;
            for (int i = 0; i < dim; i++) {
                acc *= vals;
                acc1 *= vals + 1;
            }
            if (acc <= entries && acc1 > entries) {
                return (vals);
            } else {
                if (acc > entries) {
                    vals--;
                } else {
                    vals++;
                }
            }
        }
    }

}





