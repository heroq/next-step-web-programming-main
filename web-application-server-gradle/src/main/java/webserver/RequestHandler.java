package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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

            if("GET".equals(method)){
                int addressParameterLength = address.indexOf('?');
                if(addressParameterLength == -1) {
                    response(out, address);
                    return;
                }

                if("/user/create".equals(address.substring(0, addressParameterLength))){
                    String[] getParameter = address.split("\\?");
                    Map temp = HttpRequestUtils.parseQueryString(getParameter[1]);
                    User user = new User(temp.get("userId").toString(), temp.get("password").toString(), temp.get("name").toString(), temp.get("email").toString());
                }
            } else if("POST".equals(method)){
                if("/user/create".equals(address)){
                    String getParameter = IOUtils.readData(ins, Integer.parseInt(header.get("Content-Length")));
                    Map temp = HttpRequestUtils.parseQueryString(getParameter);
                    User user = new User(temp.get("userId").toString(), temp.get("password").toString(), temp.get("name").toString(), temp.get("email").toString());
                    System.out.println(user);
                }
            }
            response(out, address);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response(OutputStream out, String address) throws IOException {
        String dir = System.getProperty("user.dir");
        DataOutputStream dos = new DataOutputStream(out);
        byte[] body = Files.readAllBytes(Paths.get(dir + "/webapp" + address));

        response200Header(dos, body.length, address);
        responseBody(dos, body);
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String type) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");

            if(type.contains(".js")) dos.writeBytes("Content-Type: text/script;charset=utf-8\r\n");
            else if(type.contains(".css")) dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            else dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");

            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
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
