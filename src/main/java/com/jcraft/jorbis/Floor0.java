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

class Floor0 extends FuncFloor {

    static float toBARK(float f) {
        return (float) (13.1 * Math.atan(.00074 * (f)) + 2.24 * Math.atan((f) * (f) * 1.85e-8) + 1e-4 * (f));
    }

    private static int ilog(int v) {
        int ret = 0;
        while (v != 0) {
            ret++;
            v >>>= 1;
        }
        return (ret);
    }

    @Override
    void pack(Object i, Buffer opb) {
        InfoFloor0 info = (InfoFloor0) i;
        opb.write(info.order, 8);
        opb.write(info.rate, 16);
        opb.write(info.barkmap, 16);
        opb.write(info.ampbits, 6);
        opb.write(info.ampdB, 8);
        opb.write(info.numbooks - 1, 4);
        for (int j = 0; j < info.numbooks; j++)
            opb.write(info.books[j], 8);
    }

    @Override
    InfoFloor0 unpack(Info vi, Buffer opb) {
        InfoFloor0 info = new InfoFloor0();
        info.order = opb.read(8);
        info.rate = opb.read(16);
        info.barkmap = opb.read(16);
        info.ampbits = opb.read(6);
        info.ampdB = opb.read(8);
        info.numbooks = opb.read(4) + 1;

        if ((info.order < 1) ||
                (info.rate < 1) ||
                (info.barkmap < 1) ||
                (info.numbooks < 1)) {
            //free_info(info);
            return (null);
        }

        for (int j = 0; j < info.numbooks; j++) {
            info.books[j] = opb.read(8);
            if (info.books[j] < 0 || info.books[j] >= vi.books) {
                //free_info(info);
                return (null);
            }
        }
        return (info);
//  err_out:
//    free_info(info);
//    return(NULL);
    }

    @Override
    LookFloor0 look(DspState vd, InfoMode mi, Object i) {
        float scale;
        Info vi = vd.vi;
        InfoFloor0 info = (InfoFloor0) i;
        LookFloor0 look = new LookFloor0();
        look.m = info.order;
        look.n = vi.blocksizes[mi.blockflag] / 2;
        look.ln = info.barkmap;
        look.vi = info;
        look.lpclook.init(look.ln);

        // we choose a scaling constant so that:
        //  floor(bark(rate/2-1)*C)=mapped-1
        // floor(bark(rate/2)*C)=mapped
        scale = look.ln / toBARK((float) (info.rate / 2.));

        // the mapping from a linear scale to a smaller bark scale is
        // straightforward.  We do *not* make sure that the linear mapping
        // does not skip bark-scale bins; the decoder simply skips them and
        // the encoder may do what it wishes in filling them.  They're
        // necessary in some mapping combinations to keep the scale spacing
        // accurate
        look.linearmap = new int[look.n];
        for (int j = 0; j < look.n; j++) {
            int val = (int) Math.floor(toBARK((float) ((info.rate / 2.) / look.n * j))
                    * scale); // bark numbers represent band edges
            if (val >= look.ln) val = look.ln; // guard against the approximation
            look.linearmap[j] = val;
        }
        return look;
    }


    @Override
    Object inverse1(Block vb, Object i, Object memo) {
        //System.err.println("Floor0.inverse "+i.getClass()+"]");
        LookFloor0 look = (LookFloor0) i;
        InfoFloor0 info = look.vi;
        float[] lisp = null;
        if (memo instanceof float[]) {
            lisp = (float[]) memo;
        }

        int ampraw = vb.opb.read(info.ampbits);
        if (ampraw > 0) { // also handles the -1 out of data case
            int maxval = (1 << info.ampbits) - 1;
            float amp = (float) ampraw / maxval * info.ampdB;
            int booknum = vb.opb.read(ilog(info.numbooks));

            if (booknum != -1 && booknum < info.numbooks) {
                CodeBook b = vb.vd.fullbooks[info.books[booknum]];
                float last = 0.f;

                if (lisp == null || lisp.length < look.m + 1) {
                    lisp = new float[look.m + 1];
                } else {
                    for (int j = 0; j < lisp.length; j++) lisp[j] = 0.f;
                }

                for (int j = 0; j < look.m; j += b.dim) {
                    if (b.decodev_set(lisp, j, vb.opb, b.dim) == -1) {
                        //goto eop;
                        return (null);
                    }
                }

                for (int j = 0; j < look.m; ) {
                    for (int k = 0; k < b.dim; k++, j++) lisp[j] += last;
                    last = lisp[j - 1];
                }
                lisp[look.m] = amp;
                return (lisp);
            }
        }
//  eop:
        return (null);
    }

    @Override
    void inverse2(Block vb, Object i, Object memo, float[] out) {
        LookFloor0 look = (LookFloor0) i;
        InfoFloor0 info = look.vi;

        if (memo != null) {
            float[] lisp;
            lisp = (float[]) memo;
            float amp = lisp[look.m];

            Lsp.lsp_to_curve(out, look.linearmap, look.n, look.ln,
                    lisp, look.m, amp, info.ampdB);
            return;
        }

        for (int j = 0; j < look.n; j++) {
            out[j] = 0.f;
        }
    }
}

class InfoFloor0 {
    int order;
    int rate;
    int barkmap;

    int ampbits;
    int ampdB;

    int numbooks; // <= 16
    int[] books = new int[16];
}

class LookFloor0 {
    int n;
    int ln;
    int m;
    int[] linearmap;

    InfoFloor0 vi;
    Lpc lpclook = new Lpc();
}
