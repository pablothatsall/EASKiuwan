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

class Drft {
    static int[] ntryh = {4, 2, 3, 5};
    static float tpi = 6.28318530717958647692528676655900577f;
    float[] trigcache;
    int[] splitcache;

    static void drfti1(int n, float[] wa, int index, int[] ifac) {
        float arg, argh, argld, fi;
        int ntry = 0, i, j = -1;
        int k1, l1, l2, ib;
        int ld, ii, ip, is, nq, nr;
        int ido, ipm, nfm1;
        int nl = n;
        int nf = 0;

        int state = 101;

        loop:
        while (true) {
            switch (state) {
                case 101:
                    j++;
                    if (j < 4)
                        ntry = ntryh[j];
                    else
                        ntry += 2;
                case 104:
                    nq = nl / ntry;
                    nr = nl - ntry * nq;
                    if (nr != 0) {
                        state = 101;
                        break;
                    }
                    nf++;
                    ifac[nf + 1] = ntry;
                    nl = nq;
                    if (ntry != 2) {
                        state = 107;
                        break;
                    }
                    if (nf == 1) {
                        state = 107;
                        break;
                    }

                    for (i = 1; i < nf; i++) {
                        ib = nf - i + 1;
                        ifac[ib + 1] = ifac[ib];
                    }
                    ifac[2] = 2;
                case 107:
                    if (nl != 1) {
                        state = 104;
                        break;
                    }
                    ifac[0] = n;
                    ifac[1] = nf;
                    argh = tpi / n;
                    is = 0;
                    nfm1 = nf - 1;
                    l1 = 1;

                    if (nfm1 == 0) return;

                    for (k1 = 0; k1 < nfm1; k1++) {
                        ip = ifac[k1 + 2];
                        ld = 0;
                        l2 = l1 * ip;
                        ido = n / l2;
                        ipm = ip - 1;

                        for (j = 0; j < ipm; j++) {
                            ld += l1;
                            i = is;
                            argld = (float) ld * argh;
                            fi = 0.f;
                            for (ii = 2; ii < ido; ii += 2) {
                                fi += 1.f;
                                arg = fi * argld;
                                wa[index + i++] = (float) Math.cos(arg);
                                wa[index + i++] = (float) Math.sin(arg);
                            }
                            is += ido;
                        }
                        l1 = l2;
                    }
                    break loop;
            }
        }
    }

    static void fdrffti(int n, float[] wsave, int[] ifac) {
        if (n == 1) return;
        drfti1(n, wsave, n, ifac);
    }


    void init(int n) {
        //System.err.println("Drft.init");
        trigcache = new float[3 * n];
        splitcache = new int[32];
        fdrffti(n, trigcache, splitcache);
    }
}
