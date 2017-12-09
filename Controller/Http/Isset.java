/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package elkserver.Controller.Http;

import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import elkserver.Http.Cookie;
import elkserver.Http.HttpEvent;
import elkserver.Http.HttpInterface;
import elkserver.ELK;

/**
 *
 * @author Razvan
 */
public class Isset implements HttpInterface{
    @Override
    public void main(HttpEvent e, ArrayList<String> get_data,JsonObject post_data){}
    
    @Override
    public void onClose() {}
    
    public void file(HttpEvent e, ArrayList<String> get_data,JsonObject post_data) throws FileNotFoundException, IOException{
        if(get_data.size() >= 0){
            File f = new File(ELK.PUBLIC_WWW+"/"+get_data.get(0));
            if(f.exists()){
                e.send(0);
            }else{
                e.send(-2);
            }
        }else{
            e.send(-1);
        }
    }
    
    public void cookie(HttpEvent e, ArrayList<String> get_data,JsonObject post_data){
        if(e.getClientHeader().get("Method").equals("POST")){
           if(post_data.has("name")){
                String name = post_data.get("name").getAsString();
                if(e.cookieIsset(name)){
                    e.send(0);
                }else{
                    e.send(-2);
                }
            }else{
                e.send(-1);
            } 
        }else{
            String jsonCookie = ELK.JSON_PARSER.toJson(new Cookie("Error", "-1"));
            e.send(jsonCookie);
        }
    }
}
