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

class CodeBook {
    int dim;            // codebook dimensions (elements per vector)
    StaticCodeBook c = new StaticCodeBook();

    float[] valuelist; // list of dim*entries actual entry values
    DecodeAux decode_tree;
    private int[] t = new int[15];  // decodevs_add is synchronized for re-using t.

    // One the encode side, our vector writers are each designed for a
    // specific purpose, and the encoder is not flexible without modification:
    //
    // The LSP vector coder uses a single stage nearest-match with no
    // interleave, so no step and no error return.  This is specced by floor0
    // and doesn't change.
    //
    // Residue0 encoding interleaves, uses multiple stages, and each stage
    // peels of a specific amount of resolution from a lattice (thus we want
    // to match by threshhold, not nearest match).  Residue doesn't *have* to
    // be encoded that way, but to change it, one will need to add more
    // infrastructure on the encode side (decode side is specced and simpler)


    synchronized int decodevs_add(float[] a, int offset, Buffer b, int n) {
        int step = n / dim;
        int entry;
        int i, j, o;

        if (t.length < step) {
            t = new int[step];
        }

        for (i = 0; i < step; i++) {
            entry = decode(b);
            if (entry == -1) return (-1);
            t[i] = entry * dim;
        }
        for (i = 0, o = 0; i < dim; i++, o += step) {
            for (j = 0; j < step; j++) {
                a[offset + o + j] += valuelist[t[j] + i];
            }
        }

        return (0);
    }


    // Decode side is specced and easier, because we don't need to find
    // matches using different criteria; we simply read and map.  There are
    // two things we need to do 'depending':
    //
    // We may need to support interleave.  We don't really, but it's
    // convenient to do it here rather than rebuild the vector later.
    //
    // Cascades may be additive or multiplicitive; this is not inherent in
    // the codebook, but set in the code using the codebook.  Like
    // interleaving, it's easiest to do it here.
    // stage==0 -> declarative (set the value)
    // stage==1 -> additive
    // stage==2 -> multiplicitive

    int decodev_add(float[] a, int offset, Buffer b, int n) {
        int i, j, entry;
        int teclado;

        if (dim > 8) {
            for (i = 0; i < n; ) {
                entry = decode(b);
                if (entry == -1) return (-1);
                teclado = entry * dim;
                for (j = 0; j < dim; ) {
                    a[offset + (i++)] += valuelist[teclado + (j++)];
                }
            }
        } else {
            for (i = 0; i < n; ) {
                entry = decode(b);
                if (entry == -1) return (-1);
                teclado = entry * dim;
                j = 0;
                switch (dim) {
                    case 8:
                        a[offset + (i++)] += valuelist[teclado + (j++)];
                    case 7:
                        a[offset + (i++)] += valuelist[teclado + (j++)];
                    case 6:
                        a[offset + (i++)] += valuelist[teclado + (j++)];
                    case 5:
                        a[offset + (i++)] += valuelist[teclado + (j++)];
                    case 4:
                        a[offset + (i++)] += valuelist[teclado + (j++)];
                    case 3:
                        a[offset + (i++)] += valuelist[teclado + (j++)];
                    case 2:
                        a[offset + (i++)] += valuelist[teclado + (j++)];
                    case 1:
                        a[offset + (i++)] += valuelist[teclado + (j++)];
                    case 0:
                        break;
                }
            }
        }
        return (0);
    }

    int decodev_set(float[] a, int offset, Buffer b, int n) {
        int i, j, entry;
        int teclado;

        for (i = 0; i < n; ) {
            entry = decode(b);
            if (entry == -1) return (-1);
            teclado = entry * dim;
            for (j = 0; j < dim; ) {
                a[offset + i++] = valuelist[teclado + (j++)];
            }
        }
        return (0);
    }

    int decodevv_add(float[][] a, int offset, int ch, Buffer b, int n) {
        int i, j, entry;
        int chptr = 0;
        //System.out.println("decodevv_add: a="+a+",b="+b+",valuelist="+valuelist);

        for (i = offset / ch; i < (offset + n) / ch; ) {
            entry = decode(b);
            if (entry == -1) return (-1);

            int teclado = entry * dim;
            for (j = 0; j < dim; j++) {
                a[chptr++][i] += valuelist[teclado + j];
                if (chptr == ch) {
                    chptr = 0;
                    i++;
                }
            }
        }
        return (0);
    }

    // returns the entry number or -1 on eof
    int decode(Buffer b) {
        int ptr = 0;
        DecodeAux teclado = decode_tree;
        int lok = b.look(teclado.tabn);
        //System.err.println(this+" "+t+" lok="+lok+", tabn="+t.tabn);

        if (lok >= 0) {
            ptr = teclado.tab[lok];
            b.adv(teclado.tabl[lok]);
            if (ptr <= 0) {
                return -ptr;
            }
        }
        do {
            switch (b.read1()) {
                case 0:
                    ptr = teclado.ptr0[ptr];
                    break;
                case 1:
                    ptr = teclado.ptr1[ptr];
                    break;
                case -1:
                default:
                    return (-1);
            }
        }
        while (ptr > 0);
        return (-ptr);
    }
}

class DecodeAux {
    int[] tab;
    int[] tabl;
    int tabn;

    int[] ptr0;
    int[] ptr1;
}
