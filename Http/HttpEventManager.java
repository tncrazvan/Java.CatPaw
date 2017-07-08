/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javahttpserver.Http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javahttpserver.JHS;

/**
 *
 * @author Razvan
 */
public abstract class HttpEventManager {
    private BufferedWriter writer;
    private HttpHeader clientHeader,header;
    private boolean defaultHeaders=true;
    private boolean alive=true;
    private boolean alreadyExecuted = false;
    private Map<String,String> userLanguages = new HashMap<String,String>();
    protected final Socket client;
    public HttpEventManager(BufferedWriter writer, HttpHeader clientHeader,Socket client) {
        this.writer=writer;
        this.clientHeader=clientHeader;
        this.client=client;
    }
    
    public Socket getClient(){
        return client;
    }
    public void setHeaderField(String fieldName,String fieldContent){
        header.set(fieldName, fieldContent);
    }
    public String getHeaderField(String fieldName){
        return header.get(fieldName);
    }
    public HttpHeader getHeader(){
        return header;
    }
    public HttpHeader getClientHeader(){
        return clientHeader;
    }
    
    public boolean isAlive(){
        return alive;
    }
    
    public boolean execute() throws IOException{
        findUserLanguages();
        /*if(alreadyExecuted)
            return false;
        
        alreadyExecuted = true;*/
        String location=clientHeader.get("Resource");
        header = new HttpHeader();
        
        
        
        
        File f = new File(java.net.URLDecoder.decode(JHS.PUBLIC_WWW+location, "UTF-8"));
        header.set("Content-Type", JHS.processContentType(location));
        if(f.exists() && !location.equals(JHS.INDEX_FILE)){
            if(!f.isDirectory()){
                sendFileContents(f);
                client.close();
            }else{
                header.set("Content-Type", "text/plain");
                onControllerRequest(location);
            }
        }else{
            if((header.get("Content-Type").equals("") || location.substring(1,2).equals("@")) && !location.equals(JHS.INDEX_FILE)){
                header.set("Content-Type", "text/plain");
                onControllerRequest(location);
            }else{
                header.set("Content-Type", "text/html");
                header.set("Status", "HTTP/1.1 404 Not Found");
                System.out.println("sending file:"+JHS.PUBLIC_WWW+location);
                sendFileContents(JHS.RESOURCE_NOT_FOUND_FILE);
                client.close();
            }
        }
        return true;
    }
    
    
    
    abstract void onControllerRequest(String location);
    
    private void findUserLanguages(){
        String[] tmp = new String[2];
        String[] languages = clientHeader.get("Accept-Language").split(",");
        userLanguages.put("DEFAULT-LANGUAGE", languages[0]);
        for(int i=1;i<languages.length;i++){
            tmp=languages[i].split(";");
            userLanguages.put(tmp[0], tmp[1]);
        }
    }
    
    public Map<String,String> getUserLanguages(){
        return userLanguages;
    }
    
    public String getUserDefaultLanguage(){
        return userLanguages.get("DEFAULT-LANGUAGE");
    }
    
    public String getUserAgent(){
        return clientHeader.get("User-Agent");
    }
    
    public void unsetCookie(String key, String path, String domain){
        header.setCookie(key,"deleted",path,domain,"Thu, 01 Jan 1970 00:00:00 GMT");
    }
    
    public void unsetCookie(String key, String path){
        unsetCookie(key, path, JHS.DOMAIN_NAME);
    }
    
    public void unsetCookie(String key){
        unsetCookie(key, "/", JHS.DOMAIN_NAME);
    }
    
    public void setCookie(String name,String value, String path, String domain, String expire){
        header.setCookie(name, value, path, domain, expire);
    }
    public void setCookie(String name,String value, String path, String domain){
        header.setCookie(name, value, path, domain);
    }
    public void setCookie(String name,String value, String path){
        header.setCookie(name, value, path);
    }
    public void setCookie(String name,String value){
        header.setCookie(name, value);
    }
    
    public String getCookie(String name){
        return clientHeader.getCookie(name);
    }
    
    public void setUserObject(String name, Object o) throws IOException{
        send("<script>window."+name+"="+JHS.JSON_PARSER.toJson(o)+";</script>");
    }
    
    public void setUserObject(String name, JsonObject o){
        send("<script>window."+name+"="+o.toString()+";</script>");
    }
    
    public void setUserArray(String name, JsonArray a){
        send("<script>window."+name+"="+a.toString()+";</script>");
    }
    
    public boolean cookieIsset(String key){
        return clientHeader.cookieIsset(key);
    }

    
    
    private boolean firstMessage = true;
    
    public void send(String data) {
        if(alive){
            if(firstMessage && defaultHeaders){
                firstMessage = false;
                try {
                    writer.write(header.toString()+"\r\n");
                    writer.flush();
                    alive = true;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    alive=false;
                    try {
                        client.close();
                    } catch (IOException ex1) {
                        Logger.getLogger(HttpEventManager.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }
            try {
                writer.write(data.toString());
                writer.flush();
                alive = true;
            } catch (IOException ex) {
                ex.printStackTrace();
                alive=false;
                try {
                    client.close();
                } catch (IOException ex1) {
                    Logger.getLogger(HttpEventManager.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
    }
    
    public void flushHeaders(){
        send();
    }
    
    public void send(byte[] data){
        send(new String(data));
    }
    public void send(){
        send("");
    }
    public void send(int data){
        send(""+data);
    }
    public void setContentType(String type){
        header.set("Content-Type", type);
    }
    
    public void sendFileContents(String filename) throws IOException{
        sendFileContents(new File(JHS.PUBLIC_WWW+filename));
    }
    
    public void disableDefaultHeaders(){
        defaultHeaders = false;
    }
    
    public void enableDefaultHeaders(){
        defaultHeaders = true;
    }
    
    private void sendFileContents(File f){
        FileInputStream fis = null;
        try {
            int BUFF_SIZE = 65000;
            byte[] buffer = new byte[BUFF_SIZE];
            fis = new FileInputStream(f);
            OutputStream os = client.getOutputStream();
            if(os != null){
                if(firstMessage && defaultHeaders){
                    firstMessage = false;
                    os.write((header.toString()+"\r\n").getBytes());
                }
                int byteRead = 0;
                int counter = 0;
                while ((byteRead = fis.read(buffer)) != -1 && counter < f.length()) {
                    counter += byteRead;
                    os.write(buffer, 0, byteRead);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(HttpEventManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
    }
    
}
