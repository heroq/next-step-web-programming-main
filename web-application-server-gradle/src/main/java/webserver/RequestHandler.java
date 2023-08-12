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

            // 요구사항(1) 1단계
            String firstLine = ins.readLine();
            if(ins.readLine() == null) return;

            Map<String, String> header = new HashMap<>();
            String getHeader;
            while(!"".equals(getHeader = ins.readLine())){
                String[] data = getHeader.split(":");
                header.put(data[0], data[1].trim());
                if("".equals(ins.readLine())) break;
            }

            // 요구사항(1) 2단계
            String address = firstLine.split(" ")[1];
            String method = firstLine.split(" ")[0];

            // 요구사항(2)
            int addressLength = address.indexOf('?');
            if(addressLength != -1 && "/user/create".equals(address.substring(0, addressLength))) {
                // 1. URL 체크를 해야함
                String[] firstLineParameter = firstLine.split(" ");
                String[] getParameter = firstLineParameter[1].split("\\?");

                // 2. GET 체크
                if ("GET".equals(method)) {
                    // 3. 데이터 추출
                    Map temp = HttpRequestUtils.parseQueryString(getParameter[1]);
                    // 4. user 클래스에 입력
                    User user = new User(temp.get("userId").toString(), temp.get("password").toString(), temp.get("name").toString(), temp.get("email").toString());
                    System.out.println(user.toString());
                }
            }

            // 요구사항(1) 3단계
            String dir = System.getProperty("user.dir");
            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = Files.readAllBytes(Paths.get(dir + "/webapp" + address));

            response200Header(dos, body.length, address);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String type) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");

            // 요구사항(1) 파일의 확장자로 체크하여 응답
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
