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

// the comments are not part of vorbis_info so that vorbis_info can be
// static storage
public class Comment {
    // unlimited user comment fields.  libvorbis writes 'libvorbis'
    // whatever vendor is set to in encode
    public byte[][] user_comments;
    public int[] comment_lengths;
    public int comments;
    public byte[] vendor;

    public void init() {
        user_comments = null;
        comments = 0;
        vendor = null;
    }

    /*
  private void add_tag(byte[] tag, byte[] contents){
    byte[] foo=new byte[tag.length+contents.length+1];
    int j=0; 
    for(int i=0; i<tag.length; i++){foo[j++]=tag[i];}
    foo[j++]=(byte)'='; j++;
    for(int i=0; i<contents.length; i++){foo[j++]=tag[i];}
    add(foo);
  }
*/

    int unpack(Buffer opb) {
        int vendorlen = opb.read(32);
        if (vendorlen < 0) {
            //goto err_out;
            clear();
            return (-1);
        }
        vendor = new byte[vendorlen + 1];
        opb.read(vendor, vendorlen);
        comments = opb.read(32);
        if (comments < 0) {
            //goto err_out;
            clear();
            return (-1);
        }
        user_comments = new byte[comments + 1][];
        comment_lengths = new int[comments + 1];

        for (int i = 0; i < comments; i++) {
            int len = opb.read(32);
            if (len < 0) {
                //goto err_out;
                clear();
                return (-1);
            }
            comment_lengths[i] = len;
            user_comments[i] = new byte[len + 1];
            opb.read(user_comments[i], len);
        }
        if (opb.read(1) != 1) {
            //goto err_out; // EOP check
            clear();
            return (-1);

        }
        return (0);
//  err_out:
//    comment_clear(vc);
//    return(-1);
    }

    void clear() {
        for (int i = 0; i < comments; i++)
            user_comments[i] = null;
        user_comments = null;
        vendor = null;
    }

    @Override
    public String toString() {
        StringBuilder foo = new StringBuilder("Vendor: " + new String(vendor, 0, vendor.length - 1));
        for (int i = 0; i < comments; i++) {
            foo.append("\nComment: ").append(new String(user_comments[i], 0, user_comments[i].length - 1));
        }
        foo.append("\n");
        return foo.toString();
    }
}
