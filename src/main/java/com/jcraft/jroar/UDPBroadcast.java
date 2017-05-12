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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

class UDPBroadcast extends Client {
    boolean headerIsSent = false;
    MySocket mysocket = null;

    String srcmpoint = null;
    String dstmpoint = null;
    String baddress = null;
    int port = 0;
    String passwd = null;
    byte[] header = null;

    UDPIO io = null;
    UDPSource udp_mpoint = null;

    UDPBroadcast(String srcmpoint, String baddress, int port, String dstmpoint) {
        super();

        try {
            io = new UDPIO(baddress, port);
        } catch (Exception e) {
            close();
        }

        this.srcmpoint = srcmpoint;
        this.dstmpoint = dstmpoint;
        this.baddress = baddress;
        this.port = port;

        Source source = Source.getSource(srcmpoint);
        source.addListener(this);
        if (source instanceof Proxy)
            ((Proxy) source).kick();

        udp_mpoint = new UDPSource(this, dstmpoint);
    }

    @Override
    public void write(List<String> http_header, byte[] header,
                      byte[] foo, int foostart, int foolength,
                      byte[] bar, int barstart, int barlength) throws IOException {
        lasttime = System.currentTimeMillis();
        ready = true;
        this.header = header;
        io.write(foo, foostart, foolength);
        io.write(bar, barstart, barlength);
        io.flush();
        ready = false;
    }

    @Override
    public void close() {
        try {
            io.close();
        } catch (Exception e) {
        }
        io = null;
        super.close();
    }

    class UDPIO {
        InetAddress address;
        DatagramSocket socket = null;
        DatagramPacket sndpacket;
        DatagramPacket recpacket;
        byte[] buf = new byte[1024];
        String host;
        int port;
        byte[] inbuffer = new byte[1024];
        byte[] outbuffer = new byte[1024];
        int instart = 0, inend = 0, outindex = 0;

        UDPIO(String host, int port) {
            this.host = host;
            this.port = port;
            try {
                address = InetAddress.getByName(host);
                socket = new DatagramSocket();
            } catch (Exception e) {
                e.printStackTrace();
            }
            recpacket = new DatagramPacket(buf, 1024);
            sndpacket = new DatagramPacket(outbuffer, 0, address, port);
        }

        void write(byte[] array, int begin, int length) throws java.io.IOException {
            if (length <= 0) return;
            int i;
            while (true) {
                if ((i = (outbuffer.length - outindex)) < length) {
                    if (i != 0) {
                        System.arraycopy(array, begin, outbuffer, outindex, i);
                        begin += i;
                        length -= i;
                        outindex += i;
                    }
                    flush();
                    continue;
                }
                System.arraycopy(array, begin, outbuffer, outindex, length);
                outindex += length;
                break;
            }
        }

        synchronized void flush() throws java.io.IOException {
            if (outindex == 0) return;
            sndpacket.setLength(outindex);
            socket.send(sndpacket);
            outindex = 0;
        }

        void close() {
            socket.close();
        }
    }

}
