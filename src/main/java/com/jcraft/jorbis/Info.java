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
import com.jcraft.jogg.Packet;

public class Info {
    private static final int VI_TIMEB = 1;
    //  private static final int VI_FLOORB=1;
    private static final int VI_FLOORB = 2;
    //  private static final int VI_RESB=1;
    private static final int VI_RESB = 3;
    private static final int VI_MAPB = 1;
    private static final int VI_WINDOWB = 1;
    public int version;
    public int channels;
    public int rate;

    // The below bitrate declarations are *hints*.
    // Combinations of the three values carry the following implications:
    //
    // all three set to the same value:
    // implies a fixed rate bitstream
    // only nominal set:
    // implies a VBR stream that averages the nominal bitrate.  No hard
    // upper/lower limit
    // upper and or lower set:
    // implies a VBR bitstream that obeys the bitrate limits. nominal
    // may also be set to give a nominal rate.
    // none set:
    //  the coder does not care to speculate.

    int bitrate_upper;
    int bitrate_nominal;
    int bitrate_lower;

    // Vorbis supports only short and long blocks, but allows the
    // encoder to choose the sizes

    int[] blocksizes = new int[2];

    // modes are the primary means of supporting on-the-fly different
    // blocksizes, different channel mappings (LR or mid-side),
    // different residue backends, etc.  Each mode consists of a
    // blocksize flag and a mapping (along with the mapping setup

    int modes;
    int maps;
    int times;
    int floors;
    int residues;
    int books;

    InfoMode[] mode_param = null;

    int[] map_type = null;
    Object[] map_param = null;

    int[] time_type = null;
    Object[] time_param = null;

    int[] floor_type = null;
    Object[] floor_param = null;

    int[] residue_type = null;
    Object[] residue_param = null;

    StaticCodeBook[] book_param = null;

    // used by synthesis, which has a full, alloced vi
    public void init() {
        rate = 0;
        //memset(vi,0,sizeof(vorbis_info));
    }

    public void clear() {
        for (int i = 0; i < modes; i++) {
            mode_param[i] = null;
        }
        mode_param = null;

        map_param = null;

        time_param = null;

        floor_param = null;

        residue_param = null;

        // the static codebooks *are* freed if you call info_clear, because
        // decode side does alloc a 'static' codebook. Calling clear on the
        // full codebook does not clear the static codebook (that's our
        // responsibility)
        for (int i = 0; i < books; i++) {
            // just in case the decoder pre-cleared to save space
            if (book_param[i] != null) {
                book_param[i] = null;
            }
        }
        book_param = null;

    }

    // Header packing/unpacking
    int unpack_info(Buffer opb) {
        version = opb.read(32);
        if (version != 0) return (-1);

        channels = opb.read(8);
        rate = opb.read(32);

        bitrate_upper = opb.read(32);
        bitrate_nominal = opb.read(32);
        bitrate_lower = opb.read(32);

        blocksizes[0] = 1 << opb.read(4);
        blocksizes[1] = 1 << opb.read(4);

        if ((rate < 1) ||
                (channels < 1) ||
                (blocksizes[0] < 8) ||
                (blocksizes[1] < blocksizes[0]) ||
                (opb.read(1) != 1)) {
            //goto err_out; // EOP check
            clear();
            return (-1);
        }
        return (0);

    }

    // The Vorbis header is in three packets; the initial small packet in
    // the first page that identifies basic parameters, a second packet
    // with bitstream comments and a third packet that holds the
    // codebook.

    // all of the real encoding details are here.  The modes, books,
    // everything
    int unpack_books(Buffer opb) {

        //d* codebooks
        books = opb.read(8) + 1;

        if (book_param == null || book_param.length != books)
            book_param = new StaticCodeBook[books];
        for (int i = 0; i < books; i++) {
            book_param[i] = new StaticCodeBook();
            if (book_param[i].unpack(opb) != 0) {
                //goto err_out;
                clear();
                return (-1);
            }
        }

        // time backend settings
        times = opb.read(6) + 1;
        if (time_type == null || time_type.length != times) time_type = new int[times];
        if (time_param == null || time_param.length != times)
            time_param = new Object[times];
        for (int i = 0; i < times; i++) {
            time_type[i] = opb.read(16);
            if (time_type[i] < 0 || time_type[i] >= VI_TIMEB) {
                //goto err_out;
                clear();
                return (-1);
            }
            if (time_param[i] == null) {
                //goto err_out;
                clear();
                return (-1);
            }
        }

        // floor backend settings
        floors = opb.read(6) + 1;
        if (floor_type == null || floor_type.length != floors)
            floor_type = new int[floors];
        if (floor_param == null || floor_param.length != floors)
            floor_param = new Object[floors];

        for (int i = 0; i < floors; i++) {
            floor_type[i] = opb.read(16);
            if (floor_type[i] < 0 || floor_type[i] >= VI_FLOORB) {
                //goto err_out;
                clear();
                return (-1);
            }

            floor_param[i] = FuncFloor.floor_P[floor_type[i]].unpack(this, opb);
            if (floor_param[i] == null) {
                //goto err_out;
                clear();
                return (-1);
            }
        }

        // residue backend settings
        residues = opb.read(6) + 1;

        if (residue_type == null || residue_type.length != residues)
            residue_type = new int[residues];

        if (residue_param == null || residue_param.length != residues)
            residue_param = new Object[residues];

        for (int i = 0; i < residues; i++) {
            residue_type[i] = opb.read(16);
            if (residue_type[i] < 0 || residue_type[i] >= VI_RESB) {
//	goto err_out;
                clear();
                return (-1);
            }
            residue_param[i] = FuncResidue.residue_P[residue_type[i]].unpack(this, opb);
            if (residue_param[i] == null) {
//	goto err_out;
                clear();
                return (-1);
            }
        }

        // map backend settings
        maps = opb.read(6) + 1;
        if (map_type == null || map_type.length != maps) map_type = new int[maps];
        if (map_param == null || map_param.length != maps) map_param = new Object[maps];
        for (int i = 0; i < maps; i++) {
            map_type[i] = opb.read(16);
            if (map_type[i] < 0 || map_type[i] >= VI_MAPB) {
//	goto err_out;
                clear();
                return (-1);
            }
            map_param[i] = FuncMapping.mapping_P[map_type[i]].unpack(this, opb);
            if (map_param[i] == null) {
//    goto err_out;
                clear();
                return (-1);
            }
        }

        // mode settings
        modes = opb.read(6) + 1;
        if (mode_param == null || mode_param.length != modes)
            mode_param = new InfoMode[modes];
        for (int i = 0; i < modes; i++) {
            mode_param[i] = new InfoMode();
            mode_param[i].blockflag = opb.read(1);
            mode_param[i].windowtype = opb.read(16);
            mode_param[i].transformtype = opb.read(16);
            mode_param[i].mapping = opb.read(8);

            if ((mode_param[i].windowtype >= VI_WINDOWB) ||
                    (mode_param[i].transformtype >= VI_WINDOWB) ||
                    (mode_param[i].mapping >= maps)) {
//      goto err_out;
                clear();
                return (-1);
            }
        }

        if (opb.read(1) != 1) {
            //goto err_out; // top level EOP check
            clear();
            return (-1);
        }

        return (0);
// err_out:
//  vorbis_info_clear(vi);
//  return(-1);
    }

    public int synthesis_headerin(Comment vc, Packet op) {
        Buffer opb = new Buffer();

        if (op != null) {
            opb.readinit(op.packet_base, op.packet, op.bytes);

            // Which of the three types of header is this?
            // Also verify header-ness, vorbis
            {
                byte[] buffer = new byte[6];
                int packtype = opb.read(8);
                //memset(buffer,0,6);
                opb.read(buffer, 6);
                if (buffer[0] != 'v' || buffer[1] != 'o' || buffer[2] != 'r' ||
                        buffer[3] != 'b' || buffer[4] != 'i' || buffer[5] != 's') {
                    // not a vorbis header
                    return (-1);
                }
                switch (packtype) {
                    case 0x01: // least significant *bit* is read first
                        if (op.b_o_s == 0) {
                            // Not the initial packet
                            return (-1);
                        }
                        if (rate != 0) {
                            // previously initialized info header
                            return (-1);
                        }
                        return (unpack_info(opb));
                    case 0x03: // least significant *bit* is read first
                        if (rate == 0) {
                            // um... we didn't get the initial header
                            return (-1);
                        }
                        return (vc.unpack(opb));
                    case 0x05: // least significant *bit* is read first
                        if (rate == 0 || vc.vendor == null) {
                            // um... we didn;t get the initial header or comments yet
                            return (-1);
                        }
                        return (unpack_books(opb));
                    default:
                        // Not a valid vorbis header type
                        //return(-1);
                        break;
                }
            }
        }
        return (-1);
    }

    //  static void v_writestring(Buffer o, byte[] s){
//    int i=0;
//    while(s[i]!=0){
//      o.write(s[i++],8);
//    }
//  }

//  static void v_readstring(Buffer o, byte[] buf, int bytes){
//    int i=0
//    while(bytes--!=0){
//      buf[i++]=o.read(8);
//    }
//  }

    @Override
    public String toString() {
        return "version:" + version +
                ", channels:" + channels +
                ", rate:" + rate +
                ", bitrate:" + bitrate_upper + "," +
                bitrate_nominal + "," +
                bitrate_lower;
    }
}
