package com.github.tncrazvan.arcano.Http;

import com.github.tncrazvan.arcano.InvalidControllerConstructorException;
import com.github.tncrazvan.arcano.SharedObject;
import static com.github.tncrazvan.arcano.SharedObject.LOGGER;
import static com.github.tncrazvan.arcano.SharedObject.RUNTIME;
import com.github.tncrazvan.arcano.Tool.Cluster.NoSecretFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

import com.github.tncrazvan.arcano.WebObject;
import com.github.tncrazvan.arcano.Tool.Encoding.JsonTools;
import com.github.tncrazvan.arcano.Tool.Reflect.ConstructorFinder;
import com.github.tncrazvan.arcano.Tool.Regex;
import static com.github.tncrazvan.arcano.Tool.Http.Status.STATUS_INTERNAL_SERVER_ERROR;
import static com.github.tncrazvan.arcano.Tool.Http.Status.STATUS_LOCKED;
import static com.github.tncrazvan.arcano.Tool.Http.Status.STATUS_NOT_FOUND;
import com.github.tncrazvan.arcano.Tool.Security.JwtMessage;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;


/**
 *
 * @author Razvan
 */
public class HttpEvent extends HttpEventManager implements JsonTools{
    private void sendHttpResponse(Exception e){
        final String message = e.getMessage();
        final HttpResponse response = new HttpResponse(message == null ? "" : message);
        sendHttpResponse(response,true);
    }
    private void sendHttpResponse(InvocationTargetException e){
        final String message = e.getTargetException().getMessage();
        final HttpResponse response = new HttpResponse(message == null ? "" : message);
        sendHttpResponse(response,true);
    }
    private void sendHttpResponse(HttpResponse response){
        sendHttpResponse(response, false);
    }
    private void sendHttpResponse(HttpResponse response,boolean exception){
        if (response.isRaw()) {
            final Object content = response.getContent();
            final byte[] raw = content == null?new byte[]{}:(byte[])content;
            setResponseHeaderField("Content-Length", raw.length+"");
            send(raw);
        } else {
            final Object content = response.getContent();
            String tmp = content.toString();
            if (so.config.responseWrapper) {
                if(!issetResponseHeaderField("Content-Type"))
                    setResponseHeaderField("Content-Type", "application/json");
                final JsonObject obj = new JsonObject();
                obj.addProperty(exception?"exception":"result", tmp);
                tmp = obj.toString();
            }
            setResponseHeaderField("Content-Length", tmp.length()+"");
            send(tmp);
        }
    }
    
    public final void invoke(final Method method) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, InvocationTargetException {
        try {
            Class<?> type = method.getReturnType();
            if (type == void.class || type == Void.class) {
                method.invoke(this);
                sendHttpResponse(SharedObject.EMPTY_RESPONSE);
            } else if (type == HttpResponse.class) {
                final HttpResponse response = (HttpResponse) method.invoke(this);
                response.resolve();
                final HashMap<String, String> localHeaders = response.getHashMapHeaders();
                if (localHeaders != null) {
                    localHeaders.forEach((key, header) -> {
                        setResponseHeaderField(key, header);
                    });
                }
                sendHttpResponse(response);
            } else {
                // if it's some other type of object...
                final Object data = method.invoke(this);
                sendHttpResponse(new HttpResponse(data == null ? "" : data).resolve());
            }
        } catch (final InvocationTargetException  e) {
            setResponseStatus(STATUS_INTERNAL_SERVER_ERROR);
            if (so.config.sendExceptions) {
                sendHttpResponse(e);
            } else
                sendHttpResponse(SharedObject.EMPTY_RESPONSE);
        }catch (IllegalAccessException | IllegalArgumentException e) {
            setResponseStatus(STATUS_INTERNAL_SERVER_ERROR);
            if (so.config.sendExceptions) {
                sendHttpResponse(e);
            } else
                sendHttpResponse(SharedObject.EMPTY_RESPONSE);
        }
    }

    private static HttpController serveController(final HttpRequestReader reader)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException,
            IllegalArgumentException, InvocationTargetException, UnsupportedEncodingException, IOException {

        String[] location = reader.location.toString().split("/");
        final String httpMethod = reader.request.headers.get("Method");
        final boolean abusiveUrl = Regex.match(reader.location.toString(), "w3e478tgdf8723qioiuy");
        Method method;
        String[] args;
        Class<?> cls;
        HttpController controller = null;
        Constructor<?> constructor;
        WebObject wo;
        int classId;
        args = new String[0];
        if (location.length == 0 || location.length == 1 && location[0].equals("")) {
            location = new String[] { "" };
        }
        try {
            if (location.length > 0 && !abusiveUrl) {
                classId = getClassnameIndex(location, httpMethod);
                final String[] typedLocation = Stream
                        .concat(Arrays.stream(new String[] { httpMethod }), Arrays.stream(location))
                        .toArray(String[]::new);
                wo = resolveClassName(classId + 1, typedLocation);
                if (wo.isLocked()) {
                    if (!reader.request.headers.issetCookie("JavaArcanoKey")) {
                        throw new NoSecretFoundException("An unauthorized client attempted to access a locked HttpController. No key specified.");
                    } else {
                        final String key = reader.request.headers.getCookie("JavaArcanoKey");
                        if (!JwtMessage.verify(key,reader.so.config.key,reader.so.config.charset)) {
                            throw new NoSecretFoundException("An unauthorized client attempted to access a locked HttpController. The specified key is invalid.");
                        }
                    }
                }
                
                cls = Class.forName(wo.getClassname());
                constructor = ConstructorFinder.getNoParametersConstructor(cls);
                if (constructor == null) {
                    throw new InvalidControllerConstructorException(String.format(
                            "\nController %s does not contain a valid constructor.\n"
                                    + "A valid constructor for your controller is a constructor that has no parameters.\n"
                                    + "Perhaps your class is an inner class and it's not static or public? Try make it a \"static public class\"!",
                            wo.getClassname()));
                }
                controller = (HttpController) constructor.newInstance();

                final String methodname = location.length > classId ? wo.getMethodname() : "main";
                args = resolveMethodArgs(classId + 1, location);
                try {
                    method = controller.getClass().getDeclaredMethod(methodname);
                    if(wo.getShellScript() != null){
                        try {
                            final String cmd = wo.getShellScript();
                            Process p = RUNTIME.exec(cmd, args, new File(reader.so.config.dir));
                            controller = new HttpController();
                            controller.init(reader, args);
                            controller.setResponseHttpHeaders(new HttpHeaders());
                            p.waitFor(reader.so.config.timeout,TimeUnit.MILLISECONDS);
                            p.destroyForcibly();
                            InputStream error = p.getErrorStream();
                            final byte[] errors = error.readAllBytes();
                            if(errors.length > 0){
                                controller.setResponseHeaderField("Content-Type", "text/plain");
                                controller.setResponseHeaderField("Content-Length", errors.length+"");
                                controller.send(errors);
                            }else{
                                //final InputStream input = p.getInputStream();
                                //final byte[] raw = input.readAllBytes();
                                //final String result = new String(raw,reader.so.config.charset);
                                
                                controller.send(p.getInputStream().readAllBytes(),false);
                            }
                        } catch (InterruptedException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                        }
                        return controller;
                    }
                } catch (final NoSuchMethodException ex) {
                    args = resolveMethodArgs(classId, location);
                    method = controller.getClass().getDeclaredMethod("main");
                }
                
            } else {
                try {
                    cls = Class.forName(reader.so.config.http.controllerDefault.getClassname());
                } catch (final ClassNotFoundException ex) {
                    cls = Class.forName(reader.so.config.http.controllerNotFound.getClassname());
                }
                constructor = ConstructorFinder.getNoParametersConstructor(cls);
                if (constructor == null) {
                    throw new InvalidControllerConstructorException(String.format(
                            "\nController %s does not contain a valid constructor.\n"
                                    + "A valid constructor for your controller is a constructor that has no parameters.\n"
                                    + "Perhaps your class is an inner class and it's not static or public? Try make it a \"static public class\"!",
                            cls.getName()));
                }
                controller = (HttpController) constructor.newInstance();
                method = controller.getClass().getDeclaredMethod("main");
            }
            controller.init(reader, args);
            controller.invoke(method);
        } catch (final ClassNotFoundException ex) {
            if(location.length == 1 && location[0].equals("")){
                try {
                    cls = Class.forName(reader.so.config.http.controllerDefault.getClassname());
                } catch (final ClassNotFoundException ex2) {
                    cls = Class.forName(reader.so.config.http.controllerNotFound.getClassname());
                }
            }else 
                cls = Class.forName(reader.so.config.http.controllerNotFound.getClassname());
            try {
                constructor = ConstructorFinder.getNoParametersConstructor(cls);
                if (constructor == null) {
                    throw new InvalidControllerConstructorException(String.format(
                            "\nController %s does not contain a valid constructor.\n"
                                    + "A valid constructor for your controller is a constructor that has no parameters.\n"
                                    + "Perhaps your class is an inner class and it's not static or public? Try make it a \"static public class\"!",
                            cls.getName()));
                }
                controller = (HttpController) constructor.newInstance();
                method = controller.getClass().getDeclaredMethod("main");
                controller.init(reader, location);
                controller.setResponseStatus(STATUS_NOT_FOUND);
                controller.invoke(method);
            } catch (final InvalidControllerConstructorException e) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        } catch (final NoSecretFoundException ex) {
            controller = new HttpController();
            controller.init(reader, location);
            controller.setResponseHttpHeaders(new HttpHeaders());
            controller.setResponseStatus(STATUS_LOCKED);
            controller.send("");
            LOGGER.log(Level.SEVERE, null, ex);
            return controller;
        } catch (final InvalidControllerConstructorException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return (HttpController) controller;
    }

    // public static void onControllerRequest(final StringBuilder url,String
    // httpMethod,String httpNotFoundNameOriginal,String httpNotFoundName) {
    public static final void onControllerRequest(final HttpRequestReader reader) {
        try {
            final HttpController controller = serveController(reader);
            if(controller != null ) controller.close();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException | UnsupportedEncodingException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            try {
                reader.client.close();
            } catch (IOException ex1) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            try {
                reader.client.close();
            } catch (IOException ex1) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    } 
}
