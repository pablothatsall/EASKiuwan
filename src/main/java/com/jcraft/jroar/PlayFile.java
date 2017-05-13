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

import com.jcraft.jogg.SyncState;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

class PlayFile extends Source implements Runnable {
    static final int BUFSIZE = 4096 * 2;
    long file_lastm = 0;
    private InputStream bitStream = null;
    private SyncState oy;
    private com.jcraft.jogg.Page og;
    private byte[] buffer = null;
    private int bytes = 0;
    private Thread me = null;
    private String file = null;
    private String[] files = null;

    PlayFile(String mountpoint, String file) {
        super(mountpoint);
        HttpServer.source_connections++;
        this.source = "playlist";
        if (file.startsWith("http://") &&
                file.endsWith(".m3u")) {
            List<String> foo = JRoar.fetch_m3u(file);
            if (foo.size() > 0) {
                this.files = new String[foo.size()];
                for (int i = 0; i < foo.size(); i++) {
                    this.files[i] = foo.get(i);
                }
                this.source = file;
            } else {
                drop();
                HttpServer.source_connections--;
            }
        } else if (JRoar.running_as_applet) {
            drop();
            HttpServer.source_connections--;
        } else if (file.equals("-")) {
            this.files = new String[1];
            this.files[0] = file;
        } else if (file.endsWith(".ogg") || file.endsWith(".spx")) {
            this.files = new String[1];
            this.files[0] = file;
            if (file.startsWith("http://")) {
                this.source = file;
            }
        } else {
            this.file = file;
            try {
                updateFiles(file);
            } catch (Exception e) {
                e.printStackTrace();
                drop();
                HttpServer.source_connections--;
            }
        }
    }

    private void updateFiles(String file) throws java.io.FileNotFoundException {
        System.out.println("loadPlaylist: " + file);
        File _file = new File(file);
        file_lastm = _file.lastModified();
        BufferedReader d
                = new BufferedReader(new InputStreamReader(new FileInputStream(_file)));
        List<String> v = new ArrayList<>();
        try {
            while (true) {
                String s = d.readLine();
                if (s == null) break;
                if (s.startsWith("#")) continue;
                if (!s.startsWith("http://") &&
                        !s.endsWith(".ogg") &&
                        !s.endsWith(".spx"))
                    continue;
                System.out.println("playFile (" + s + ")");
                v.add(s);
            }
            d.close();
        } catch (Exception ee) {
        }
        this.files = new String[v.size()];
        for (int i = 0; i < v.size(); i++) {
            this.files[i] = v.get(i);
        }
    }

    void init_ogg() {
        oy = new SyncState();
        og = new com.jcraft.jogg.Page();
        buffer = null;
        bytes = 0;
    }

    public void kick() {

        if (me != null) {
    /*
      int size=listeners.size();
      Client c=null;
        c=(Client)(listeners.elementAt(1));
System.out.println(c);
        if(System.currentTimeMillis()-c.lasttime>1000){
          try{
            ((HttpClient)c).ms.close();
	  }
          catch(Exception e){}
	}
	*/
            return;
        }
        me = new Thread(this);
        me.start();
    }
//static String file="??";

    @Override
    public void run() {
        List<String> http_header = new ArrayList<>();
        http_header.add("HTTP/1.0 200 OK");
        http_header.add("Content-Type: application/x-ogg");

        int ii = -1;
        loop:
        while (me != null) {
            ii++;
            if (this.file != null &&
                    file_lastm < (new File(this.file)).lastModified()) {
                try {
                    updateFiles(file);
                } catch (Exception e) {
                    break;
                }
            }
            if (ii >= files.length) ii = 0;

            bitStream = null;
            if (files[ii].startsWith("http://")) {
                try {
                    URL url = new URL(files[ii]);
                    URLConnection urlc = url.openConnection();

                    setURLProperties(urlc);        // for jroar

                    bitStream = urlc.getInputStream();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (files[ii].equals("-")) {
                bitStream = System.in;
            }


            if (bitStream == null) {
                try {
                    bitStream = new FileInputStream(files[ii]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (bitStream == null) {
                files[ii] = null;
                String[] foo = new String[files.length - 1];
                if (ii > 0)
                    System.arraycopy(files, 0, foo, 0, ii);
                if (ii != files.length - 1)
                    System.arraycopy(files, ii + 1, foo, ii, files.length - 1 - ii);
                files = foo;
                ii--;
                if (files.length == 0) {
                    break;
                }
                continue;
            }


            file = files[ii];


            init_ogg();

            int serialno;
            long granulepos;

            long start_time = -1;
            long last_sample = 0;
            long time = 0;

            ByteArrayOutputStream _header = new ByteArrayOutputStream();
            byte[] header = null;
            com.jcraft.jogg.Page[] pages = new com.jcraft.jogg.Page[10];
            int page_count = 0;

            boolean eos = false;
            while (!eos) {
                if (me == null) break;
                int index = oy.buffer(BUFSIZE);
                buffer = oy.data;
                try {
                    bytes = bitStream.read(buffer, index, BUFSIZE);
                } catch (Exception e) {
                    e.printStackTrace();
                    eos = true;
                    continue;
                }
                if (bytes == -1) break;
                if (bytes == 0) break;
                oy.wrote(bytes);

                while (!eos) {
                    if (me == null) break;
                    int result = oy.pageout(og);

                    if (result == 0) break; // need more data
                    if (result == -1) { // missing or corrupt data at this page position
//	    System.err.println("Corrupt or missing data in bitstream; continuing...");
                    } else {
        /*
          if(serialno!=og.serialno()){
              header=null;
              serialno=og.serialno();
	    }
	    */
                        serialno = og.serialno();
                        granulepos = og.granulepos();

//System.out.println("og: "+og+", pos="+og.granulepos());

                        if ((granulepos == 0)
                                || (granulepos == -1)          // hack for Speex
                                ) {

                            if (pages.length <= page_count) {
                                com.jcraft.jogg.Page[] foo = new com.jcraft.jogg.Page[pages.length * 2];
                                System.arraycopy(pages, 0, foo, 0, pages.length);
                                pages = foo;
                            }
                            pages[page_count++] = og.copy();
                        } else {
                            if (header == null) {
                                parseHeader(pages, page_count);
//System.out.println("rate: "+current_info.rate);
                                com.jcraft.jogg.Page foo;
                                for (int i = 0; i < page_count; i++) {
                                    foo = pages[i];
                                    _header.write(foo.header_base, foo.header, foo.header_len);
                                    _header.write(foo.body_base, foo.body, foo.body_len);
                                }
                                header = _header.toByteArray();
                                _header.reset();

                                page_count = 0;
                                start_time = System.currentTimeMillis();
                                last_sample = 0;
                                time = 0;
                            }
                        }

                        //            synchronized(listeners){  // In some case, c.write will block.
                        int size = listeners.size();

                        Client c = null;
                        for (int i = 0; i < size; ) {
                            try {
                                c = (listeners.get(i));
                                c.write(http_header, header,
                                        og.header_base, og.header, og.header_len,
                                        og.body_base, og.body, og.body_len);
                            } catch (Exception e) {
                                c.close();
                                removeListener(c);
                                size--;
                                continue;
                            }
                            i++;
                        }
//	    }
                        if (granulepos != 0 &&
                                key_serialno == serialno) {
                            if (last_sample == 0) {
                                time = (System.currentTimeMillis() - start_time) * 1000;
                            } else {
                                time += ((granulepos - last_sample) * 1000000) /
                                        ((current_info.rate));
                            }
                            last_sample = granulepos;
                            long sleep = (time / 1000) - (System.currentTimeMillis() - start_time);
//System.out.println("sleep="+sleep);
//if(sleep>10000){sleep=0; time=(System.currentTimeMillis()-start_time);}
                            if (sleep > 0) {
//System.out.println("sleep: "+sleep);
                                try {
                                    Thread.sleep(sleep);
                                } catch (Exception e) {
                                }
                            }
                        }

                        // sleep for green thread.
                        try {
                            Thread.sleep(1);
                        } catch (Exception e) {
                        }

                        //	    if(og.eos()!=0)eos=true;

                    }
                }
            }
            oy.clear();
            try {
                if (bitStream != null) bitStream.close();
            } catch (Exception e) {
            }
            bitStream = null;
        }

        oy.clear();
        try {
            if (bitStream != null) bitStream.close();
        } catch (Exception e) {
        }
        bitStream = null;

        drop();
    }

    private void setURLProperties(URLConnection urlc) {
        if (HttpServer.myURL != null) {
            urlc.setRequestProperty("jroar-proxy", HttpServer.myURL + mountpoint);
            //System.out.println(HttpServer.myURL+mountpoint);
            if (JRoar.comment != null)
                urlc.setRequestProperty("jroar-comment", JRoar.comment);
        }
    }

    public void stop() {
        if (me != null) {
            if (oy != null) oy.clear();
            try {
                if (bitStream != null) bitStream.close();
            } catch (Exception e) {
            }
            bitStream = null;
            me = null;
        }
        drop_clients();
    }

    void drop_clients() {
        Client c;
        synchronized (listeners) {
            for (Client listener : listeners) {
                c = listener;
                try {
                    c.close();
                } catch (Exception e) {
                }
            }
            listeners.clear();
        }
    }

    @Override
    void drop() {
        stop();
        super.drop();
    }
}
