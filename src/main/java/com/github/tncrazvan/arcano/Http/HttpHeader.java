package com.github.tncrazvan.arcano.Http;

import com.github.tncrazvan.arcano.SharedObject;
import static com.github.tncrazvan.arcano.Tool.Time.time;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author Razvan
 */
public class HttpHeader extends SharedObject{
    private final Map<String, String> header = new HashMap();
    public Map<String, String[]> cookies = new HashMap();
    public HttpHeader(final boolean createSuccessHeader) {
        if (createSuccessHeader) {
            header.put("@Status", "HTTP/1.1 200 OK");
            header.put("Date", formatHttpDefaultDate.format(time()));
            header.put("Cache-Control", "no-store");
        }
    }

    public HttpHeader() {
        this(true);
    }

    public String fieldToString(final String key) {
        final String value = header.get(key);

        if (key.equals("@Resource") || key.equals("@Status") || value.equalsIgnoreCase(key)) {
            return value + "\r\n";
        }
        return key + ": " + value + "\r\n";
    }

    public String cookieToString(final String key) {
        final String[] c = cookies.get(key);
        final LocalDateTime time = (c[3] == null ? null : time(Integer.parseInt(c[3]) * 1000L));
        // Thu, 01 Jan 1970 00:00:00 GMT
        return c[4] + ": " + key + "=" + c[0] + (c[1] == null ? "" : "; path=" + c[1])
                + (c[2] == null ? "" : "; domain=" + c[2])
                + (c[3] == null ? "" : "; expires=" + formatHttpDefaultDate.format(time)) + "\r\n";
    }

    @Override
    public String toString() {
        String str = "";
        str = header.keySet().stream().map((key) -> this.fieldToString(key)).reduce(str, String::concat);

        str = cookies.keySet().stream().map((key) -> this.cookieToString(key)).reduce(str, String::concat);
        return str;
    }

    public boolean isDefined(final String key) {
        return header.get(key) != null;
    }

    public void set(final String a, String b) {
        if (a.equals("@Status"))
            b = "HTTP/1.1 " + b;
        header.put(a, b);
    }

    public String get(final String key) {
        if (!header.containsKey(key)) {
            return null;
        }
        return header.get(key).trim();
    }

    public boolean issetCookie(final String key) {
        final Iterator i = cookies.entrySet().iterator();
        Map.Entry pair;
        String tmp = "";
        while (i.hasNext()) {
            pair = (Map.Entry) i.next();
            tmp = (String) pair.getKey();
            if (tmp.trim().equals(key.trim())) {
                return true;
            }
        }
        return false;
    }

    public String getCookie(final String key) throws UnsupportedEncodingException {
        final String[] cookie = cookies.get(key);
        if (cookie == null)
            return null;
        return URLDecoder.decode(cookie[0], charset);
    }

    public void setCookie(final String key, final String v, final String path, final String domain, final int expire)
            throws UnsupportedEncodingException {
        setCookie(key, v, path, domain, formatHttpDefaultDate.format(time(expire)));
    }

    public void setCookie(final String key, final String v, String path, final String domain, final String expire)
            throws UnsupportedEncodingException {
        if (path == null)
            path = "/";
        final String[] b = new String[5];
        b[0] = URLEncoder.encode(v, charset);
        b[1] = path;
        b[2] = domain;
        b[3] = expire;
        b[4] = "Set-Cookie";
        cookies.put(key.trim(), b);
    }

    public void setCookie(final String key, final String v, final String path, final String domain)
            throws UnsupportedEncodingException {
        setCookie(key, v, path, domain, null);
    }

    public void setCookie(final String key, final String v, final String path) throws UnsupportedEncodingException {
        setCookie(key, v, path, null, null);
    }

    public void setCookie(final String key, final String v) throws UnsupportedEncodingException {
        setCookie(key, v, "/", null, null);
    }

    public Map<String, String> getMap() {
        return header;
    }

    public static HttpHeader fromString(final String string) {
        final HttpHeader header = new HttpHeader(false);
        final String[] lines = string.split("\\r\\n");
        final boolean end = false;
        for (final String line : lines) {
            if (line.equals("")) {
                continue;
            }
            final String[] item = line.split(":\\s*", 2);
            if (item.length > 1) {
                if (item[0].equals("Cookie")) {
                    final String[] c = item[1].split(";");
                    for (final String c1 : c) {
                        final String[] cookieInfo = c1.split("=(?!\\s|\\s|$)");
                        if (cookieInfo.length > 1) {
                            final String[] b = new String[5];
                            b[0] = cookieInfo[1];
                            b[1] = cookieInfo.length > 2 ? cookieInfo[2] : null;
                            b[2] = cookieInfo.length > 3 ? cookieInfo[3] : null;
                            b[3] = cookieInfo.length > 3 ? cookieInfo[3] : null;
                            b[4] = "Cookie";
                            header.cookies.put(cookieInfo[0], b);
                        }
                    }
                } else {
                    header.set(item[0], item[1]);
                }
            } else {
                if (line.matches("^.+(?=\\s\\/).*HTTPS?\\/.*$")) {
                    final String[] parts = line.split("\\s+");
                    header.set("Method",parts[0]);
                    header.set("@Resource",parts[1]);
                    header.set("Version",parts[2]);
                } else {
                    header.set(line, null);
                }
            }
        }
        return header;
    }
}
