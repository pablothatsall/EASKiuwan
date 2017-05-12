/* -*-mode:java; c-basic-offset:2; -*- */
/* JRoar -- pure Java streaming server for Ogg 
 *
 * Copyright (C) 2001,2002 ymnk, JCraft,Inc.
 *
 * Written by: 2001,2002 ymnk<ymnk@jcraft.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.jcraft.jroar;

import java.io.IOException;
import java.util.List;

class HttpClient extends Client {
    boolean headerIsSent = false;
    MySocket ms = null;
    String file = null;

    String touched = "not yet";

    HttpClient(MySocket ms, List<String> httpheader, String file) {
        super();
        this.ms = ms;
        this.file = file;
        String foo;
        for (String aHttpheader : httpheader) {
            foo = aHttpheader;
            if (foo.startsWith("jroar-proxy: ")) {
                proxy = foo.substring(foo.indexOf(' ') + 1);
            }
        }
    }

    @Override
    public void write(List<String> http_header, byte[] header,
                      byte[] foo, int foostart, int foolength,
                      byte[] bar, int barstart, int barlength) throws IOException {
        touched = "done";
        lasttime = System.currentTimeMillis();
        ready = true;
        if (!headerIsSent) {
            if (header == null) {
                ready = false;
                return;
            }
            for (String aHttp_header : http_header) {
                ms.println(aHttp_header);
            }
            ms.println("");
            ms.flush();

            ms.write(header, 0, header.length);
            headerIsSent = true;
        }
        ms.write(foo, foostart, foolength);
        ms.write(bar, barstart, barlength);
        ms.flush();
        ready = false;
    }

    @Override
    public void close() {
        if (!headerIsSent) {
            try {
                Page.unknown(ms, file);
            } catch (Exception e) {
            }
        }
        try {
            ms.close();
        } catch (Exception e) {
        }
        ms = null;
        super.close();
    }


    @Override
    public String toString() {
        return super.toString() + ",hederIsSent=" + headerIsSent + ",touched=" + touched + ",lasttime=" + lasttime + ",ready=" + ready + (ms != null ? ",from=" + ms.socket.getInetAddress() : ",ms=null") + "<br>";
    }
}
