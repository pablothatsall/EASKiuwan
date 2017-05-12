package misc;/* JRoar -- pure Java streaming server for Ogg
 *
 * Copyright (C) 2001, 2002 ymnk, JCraft,Inc.
 *
 * Written by: 2001, 2002 ymnk<ymnk@jcraft.com>
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

import com.jcraft.jroar.JRoar;
import com.jcraft.jroar.MySocket;
import com.jcraft.jroar.UserPage;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public class PlayList extends UserPage {
    public void kick(MySocket s, Hashtable<String, String> vars, List<String> httpheader) throws IOException {
        s.println("HTTP/1.0 200 OK");
        s.println("Content-Type: text/plain");
        s.println("");

        Enumeration<String> keys = JRoar.getSources().keys();
        for (; keys.hasMoreElements(); ) {
            String mountpoint = ((keys.nextElement()));
            try {
                s.println(mountpoint);
            } catch (Exception e) {
                //s.println(e.toString());
            }
        }
        s.flush();
        s.close();
    }
}
