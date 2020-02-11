package com.github.tncrazvan.arcano.WebSocket;

import com.github.tncrazvan.arcano.Http.HttpHeaders;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import com.github.tncrazvan.arcano.Http.HttpRequestReader;
import com.github.tncrazvan.arcano.Tool.Regex;
import com.github.tncrazvan.arcano.WebObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;


/**
 *
 * @author Razvan
 */
public abstract class WebSocketEvent extends WebSocketEventManager{
    
    public static void serveController(final HttpRequestReader reader)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, IOException {
        WebSocketController controller;
        Class<?> cls;
        try {
            final int classId = getClassnameIndex(reader.location, "WS");
            final String[] typedLocation = Stream.concat(Arrays.stream(new String[] { "WS" }), Arrays.stream(reader.location))
                    .toArray(String[]::new);
            final WebObject wo = resolveClassName(classId + 1, typedLocation);
            cls = Class.forName(wo.getClassName());
            controller = (WebSocketController) cls.getDeclaredConstructor().newInstance();
            reader.args = resolveMethodArgs(classId + 1, reader.location);
        } catch (final ClassNotFoundException ex) {
            reader.args = resolveMethodArgs(0, reader.location);
            cls = Class.forName(reader.so.config.webSocket.controllerNotFound.getClassName());
            controller = (WebSocketController) cls.getDeclaredConstructor().newInstance();
        }
        controller.init(reader);
        controller.execute();
    }

    protected abstract void onOpen();

    protected abstract void onMessage(WebSocketMessage payload);

    protected abstract void onClose();

    @Override
    protected void manageOnOpen() {
        if (so.WEB_SOCKET_EVENTS.get(this.getClass().getName()) == null) {
            final ArrayList<WebSocketEvent> tmp = new ArrayList<>();
            tmp.add(this);
            so.WEB_SOCKET_EVENTS.put(this.getClass().getName(), tmp);
        } else {
            so.WEB_SOCKET_EVENTS.get(this.getClass().getName()).add(this);
        }
        this.onOpen();
    }

    @Override
    protected void manageOnMessage(final WebSocketMessage payload) {
        this.onMessage(payload);
    }
    
    @Override
    protected void manageOnClose() {
        so.WEB_SOCKET_EVENTS.get(this.getClass().getName()).remove(this);
        this.onClose();
    }
    
}
