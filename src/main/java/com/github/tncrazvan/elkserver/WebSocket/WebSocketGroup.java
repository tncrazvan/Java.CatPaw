/**
 * ElkServer is a Java library that makes it easier
 * to program and manage a Java Web Server by providing different tools
 * such as:
 * 1) An MVC (Model-View-Controller) alike design pattern to manage 
 *    client requests without using any URL rewriting rules.
 * 2) A WebSocket Manager, allowing the server to accept and manage 
 *    incoming WebSocket connections.
 * 3) Direct access to every socket bound to every client application.
 * 4) Direct access to the headers of the incomming and outgoing Http messages.
 * Copyright (C) 2016-2018  Tanase Razvan Catalin
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.tncrazvan.elkserver.WebSocket;

import com.github.tncrazvan.elkserver.Elk;
import com.github.tncrazvan.elkserver.Http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author razvan
 */
public class WebSocketGroup {
    public static final int 
            PRIVATE = 0,
            PUBLIC = 1;
    private final String key;
    private final Map<String, WebSocketEvent> events = new HashMap<>();
    private WebSocketEvent master = null;
    private int visibility = PRIVATE;
    private String name;

    public WebSocketGroup(HttpSession session) {
        this.key = Elk.getBCryptString(session.id());
    }
    
    public void setGroupName(String name){
        this.name = name;
    }
    
    public String getGroupName(){
        return name;
    }
    
    public void setVisibility(int v){
        visibility = v;
    }
    
    public int getVisibility(){
        return visibility;
    }
    
    public void addClient(WebSocketEvent e) throws UnsupportedEncodingException{
        e.startSession();
        events.put(e.session.id(), e);
    }
    
    public WebSocketEvent removeClient(WebSocketEvent e){
        if(matchCreator(e)){
            master = null;
        }
        return events.remove(e.session.id());
    }
    public boolean clientExists(WebSocketEvent e){
        return events.containsKey(e.session.id());
    }
    
    public Map<String,WebSocketEvent> getMap(){
        return events;
    }
    
    public String getKey(){
        return key;
    }
    
    public WebSocketEvent getGroupMaster(){
        return master;
    }
    
    public boolean groupMasterIsset(){
        return master != null;
    }
    
    public void setGroupMaster(WebSocketEvent e){
        master = e;
    }
    
    public void unsetGroupMaster(){
        master = null;
    }
    
    public boolean matchCreator(WebSocketEvent e){
        return Elk.validateBCryptString(e.session.id(), key);
    }
}
