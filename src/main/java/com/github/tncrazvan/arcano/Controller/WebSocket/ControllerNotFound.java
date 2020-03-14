package com.github.tncrazvan.arcano.Controller.WebSocket;

import com.github.tncrazvan.arcano.WebSocket.WebSocketController;
import com.github.tncrazvan.arcano.WebSocket.WebSocketCommit;
import com.github.tncrazvan.arcano.Bean.WebSocket.WebSocketNotFound;

/**
 *
 * @author razvan
 */
@WebSocketNotFound
public class ControllerNotFound extends WebSocketController{
    @Override
    public void onOpen() {
        close();
    }

    @Override
    public void onMessage(final WebSocketCommit message) {}

    @Override
    public void onClose() {}
}