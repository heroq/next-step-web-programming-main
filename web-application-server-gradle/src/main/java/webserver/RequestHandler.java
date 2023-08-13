package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    /**
     * 생성자 주입
     */
    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

        try (InputStream in = connection.getInputStream();
             OutputStream out = connection.getOutputStream()) {

            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            BufferedReader ins = new BufferedReader(new InputStreamReader(in));

            String firstLine = ins.readLine();
            if(ins.readLine() == null) return;

            Map<String, String> header = new HashMap<>();
            String getHeader;
            while(!"".equals(getHeader = ins.readLine())){
                String[] data = getHeader.split(":");
                header.put(data[0], data[1].trim());
            }

            String address = firstLine.split(" ")[1];
            String method = firstLine.split(" ")[0];
            String cookie = "";

            // 1. 코드 중복 너무 많다.
            // 2. 유지보수 박살났다. (다형성 없음)
            if("GET".equals(method)){
                int addressParameterLength = address.indexOf('?');
                if(addressParameterLength == -1) {
                    if("/user/list".equals(address)){
                        Map<String, String> cookieList = HttpRequestUtils.parseCookies(header.get("Cookie"));
                        Boolean logined = Boolean.parseBoolean(cookieList.get("logined"));
                        StringBuilder stringBuilder = new StringBuilder();
                        if(logined) {
                            Collection<User> userList = DataBase.findAll();
                            for (User user : userList) {
                                stringBuilder.append(user);
                            }
                            responseHtml(out, stringBuilder.toString().getBytes(), cookie);
                        }else{
                            String script = "<script>location.href = '/index.html';</script>";
                            stringBuilder.append(script);
                        }
                        responseHtml(out, stringBuilder.toString().getBytes(), null);
                        return;
                    }
                    response(out, address, null);
                    return;
                }

                if("/user/create".equals(address.substring(0, addressParameterLength))){
                    String[] getParameter = address.split("\\?");
                    Map temp = HttpRequestUtils.parseQueryString(getParameter[1]);
                    User user = new User(temp.get("userId").toString(), temp.get("password").toString(), temp.get("name").toString(), temp.get("email").toString());
                    DataBase.addUser(user);
                    response302Header(out, "/index.html");
                    return;
                }
            } else if("POST".equals(method)){
                String getParameter = IOUtils.readData(ins, Integer.parseInt(header.get("Content-Length")));
                Map temp = HttpRequestUtils.parseQueryString(getParameter);
                if("/user/create".equals(address)){
                    User user = new User(temp.get("userId").toString(), temp.get("password").toString(), temp.get("name").toString(), temp.get("email").toString());
                    DataBase.addUser(user);
                    response302Header(out, "/index.html");
                    return;

                } else if("/user/login".equals(address)){
                    User user = DataBase.findUserById(temp.getOrDefault("userId", "").toString());
                    if(user == null) {
                        cookie = "logined=false";
                        address = "/user/login_failed.html";
                        response(out, address, cookie);
                        System.out.println("null check");
                        return;
                    }

                    if(user.getUserId().equals(temp.get("userId")) && user.getPassword().equals(temp.get("password"))){
                        // 로그인 성공
                        cookie = "logined=true";
                        String script = "<script>location.href = '/index.html';</script>";
                        responseHtml(out, script.getBytes(), cookie);
                        return;
                    } else {
                        // 로그인 실패
                        cookie = "logined=false";
                        address = "/user/login_failed.html";
                    }
                }
            }
            response(out, address, cookie);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response(OutputStream out, String address, String cookie) throws IOException {
        String dir = System.getProperty("user.dir");
        DataOutputStream dos = new DataOutputStream(out);
        byte[] body = Files.readAllBytes(Paths.get(dir + "/webapp" + address));

        response200Header(dos, body.length, address, cookie);
        responseBody(dos, body);
    }

    private void responseHtml(OutputStream out, byte[] body, String cookie) throws IOException {
        String dir = System.getProperty("user.dir");
        DataOutputStream dos = new DataOutputStream(out);
        response200Header(dos, body.length, "", cookie);
        responseBody(dos, body);
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String address, String cookie) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");

            if(address.contains(".js")) dos.writeBytes("Content-Type: text/script;charset=utf-8\r\n");
            else if(address.contains(".css")) dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            else dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");

            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");

            if(!"".equals(cookie) && cookie != null){
                dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
                dos.writeBytes("\r\n");
            } else {
                dos.writeBytes("\r\n");
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(OutputStream out, String location) {
        DataOutputStream dos = new DataOutputStream(out);
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + location + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
