package com.github.tncrazvan.arcano.Controller.Http;

import com.github.tncrazvan.arcano.Http.HttpController;
import static com.github.tncrazvan.arcano.Tool.Http.Status.STATUS_NOT_FOUND;

import com.github.tncrazvan.arcano.Bean.Http.HttpServiceNotFound;

/**
 *
 * @author razvan
 */
public class ControllerNotFound extends HttpController {  
    @HttpServiceNotFound
    public String main() {
        setResponseStatus(STATUS_NOT_FOUND);
        return "";
    }
}
