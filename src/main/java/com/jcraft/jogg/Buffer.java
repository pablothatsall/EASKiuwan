/* -*-mode:java; c-basic-offset:2; -*- */
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

package com.jcraft.jogg;

public class Buffer {
    private static final int BUFFER_INCREMENT = 256;

    private static final int[] mask = {
            0x00000000, 0x00000001, 0x00000003, 0x00000007, 0x0000000f,
            0x0000001f, 0x0000003f, 0x0000007f, 0x000000ff, 0x000001ff,
            0x000003ff, 0x000007ff, 0x00000fff, 0x00001fff, 0x00003fff,
            0x00007fff, 0x0000ffff, 0x0001ffff, 0x0003ffff, 0x0007ffff,
            0x000fffff, 0x001fffff, 0x003fffff, 0x007fffff, 0x00ffffff,
            0x01ffffff, 0x03ffffff, 0x07ffffff, 0x0fffffff, 0x1fffffff,
            0x3fffffff, 0x7fffffff, 0xffffffff
    };

    int ptr = 0;
    byte[] buffer = null;
    int endbit = 0;
    int endbyte = 0;
    int storage = 0;


    public void writeclear() {
        buffer = null;
    }

    public void writeinit() {
        buffer = new byte[BUFFER_INCREMENT];
        ptr = 0;
        buffer[0] = (byte) '\0';
        storage = BUFFER_INCREMENT;
    }

    public void write(byte[] s) {
        for (byte value : s) {
            if (value == 0) break;
            write(value, 8);
        }
    }

    public void read(byte[] s, int bytes) {
        int i = 0;
        while (bytes-- != 0) {
            s[i++] = (byte) (read(8));
        }
    }


    public void readinit(byte[] buf, int start, int bytes) {

        ptr = start;
        buffer = buf;
        endbit = endbyte = 0;
        storage = bytes;
    }

    public void write(int value, int bits) {
        if (endbyte + 4 >= storage) {
            byte[] foo = new byte[storage + BUFFER_INCREMENT];
            System.arraycopy(buffer, 0, foo, 0, storage);
            buffer = foo;
            storage += BUFFER_INCREMENT;
        }

        value &= mask[bits];
        bits += endbit;
        buffer[ptr] |= (byte) (value << endbit);

        if (bits >= 8) {
            buffer[ptr + 1] = (byte) (value >>> (8 - endbit));
            if (bits >= 16) {
                buffer[ptr + 2] = (byte) (value >>> (16 - endbit));
                if (bits >= 24) {
                    buffer[ptr + 3] = (byte) (value >>> (24 - endbit));
                    if (bits >= 32) {
                        if (endbit > 0)
                            buffer[ptr + 4] = (byte) (value >>> (32 - endbit));
                        else
                            buffer[ptr + 4] = 0;
                    }
                }
            }
        }

        endbyte += bits / 8;
        ptr += bits / 8;
        endbit = bits & 7;
    }

    public int look(int bits) {
        int ret;
        int m = mask[bits];

        bits += endbit;

//System.err.println("look ptr:"+ptr+", bits="+bits+", endbit="+endbit+", storage="+storage);

        if (endbyte + 4 >= storage) {
            if (endbyte + (bits - 1) / 8 >= storage) return (-1);
        }

        ret = ((buffer[ptr]) & 0xff) >>> endbit;
//  ret=((byte)(buffer[ptr]))>>>endbit;
        if (bits > 8) {
            ret |= ((buffer[ptr + 1]) & 0xff) << (8 - endbit);
//      ret|=((byte)(buffer[ptr+1]))<<(8-endbit);
            if (bits > 16) {
                ret |= ((buffer[ptr + 2]) & 0xff) << (16 - endbit);
//        ret|=((byte)(buffer[ptr+2]))<<(16-endbit);
                if (bits > 24) {
                    ret |= ((buffer[ptr + 3]) & 0xff) << (24 - endbit);
//System.err.print("ret="+Integer.toHexString(ret)+", ((byte)(buffer[ptr+3]))="+Integer.toHexString(((buffer[ptr+3])&0xff)));
//	  ret|=((byte)(buffer[ptr+3]))<<(24-endbit);
//System.err.println(" ->ret="+Integer.toHexString(ret));
                    if (bits > 32 && endbit != 0) {
                        ret |= ((buffer[ptr + 4]) & 0xff) << (32 - endbit);
//	    ret|=((byte)(buffer[ptr+4]))<<(32-endbit);
                    }
                }
            }
        }
        return (m & ret);
    }


    public void adv(int bits) {
        bits += endbit;
        ptr += bits / 8;
        endbyte += bits / 8;
        endbit = bits & 7;
    }

    public int read(int bits) {

        int ret;
        int m = mask[bits];

        bits += endbit;

        if (endbyte + 4 >= storage) {
            ret = -1;
            if (endbyte + (bits - 1) / 8 >= storage) {
                ptr += bits / 8;
                endbyte += bits / 8;
                endbit = bits & 7;
                return (ret);
            }
        }


        ret = ((buffer[ptr]) & 0xff) >>> endbit;
        if (bits > 8) {
            ret |= ((buffer[ptr + 1]) & 0xff) << (8 - endbit);
//      ret|=((byte)(buffer[ptr+1]))<<(8-endbit);
            if (bits > 16) {
                ret |= ((buffer[ptr + 2]) & 0xff) << (16 - endbit);
//        ret|=((byte)(buffer[ptr+2]))<<(16-endbit);
                if (bits > 24) {
                    ret |= ((buffer[ptr + 3]) & 0xff) << (24 - endbit);
//	  ret|=((byte)(buffer[ptr+3]))<<(24-endbit);
                    if (bits > 32 && endbit != 0) {
                        ret |= ((buffer[ptr + 4]) & 0xff) << (32 - endbit);
//	    ret|=((byte)(buffer[ptr+4]))<<(32-endbit);
                    }
                }
            }
        }

        ret &= m;

        ptr += bits / 8;
//    ptr=bits/8;
        endbyte += bits / 8;
//    endbyte=bits/8;
        endbit = bits & 7;
        return (ret);
    }

    public int read1() {
        int ret;
        if (endbyte >= storage) {
            ret = -1;
            endbit++;
            if (endbit > 7) {
                endbit = 0;
                ptr++;
                endbyte++;
            }
            return (ret);
        }

        ret = (buffer[ptr] >> endbit) & 1;

        endbit++;
        if (endbit > 7) {
            endbit = 0;
            ptr++;
            endbyte++;
        }
        return (ret);
    }

    public int bytes() {
        return (endbyte + (endbit + 7) / 8);
    }

    public byte[] buffer() {
        return (buffer);
    }

}





