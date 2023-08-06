package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            // 1단계
            String firstLine = ins.readLine();

            if(ins.readLine() == null) return;

            Map<String, String> header = new HashMap<>();
            String getHeader;
            while(!"".equals(getHeader = ins.readLine())){
                String[] data = getHeader.split(":");
                header.put(data[0], data[1].trim());
                if("".equals(ins.readLine())) break;
            }

            // 2단계
            String address = firstLine.split(" ")[1];

            // 3단계
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

            // 파일의 확장자로 체크하여 응답
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
