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
            System.out.println("1단계");
            String firstLine = ins.readLine();

            if(ins.readLine() == null) return;

            Map<String, String> header = new HashMap<>();
            String getHeader;
            while(!"".equals(getHeader = ins.readLine())){
                String[] data = getHeader.split(":");
                System.out.println(getHeader);
                header.put(data[0], data[1].trim());
                if("".equals(ins.readLine())) break;
            }

            //System.out.println("Accept:");
            //System.out.println(header.get("Accept"));
            //System.out.println(header.get("Sec-Fetch-Dest"));

            // 2단계
            String address = firstLine.split(" ")[1];

            // 3단계
            String dir = System.getProperty("user.dir");
            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = Files.readAllBytes(Paths.get(dir + "/webapp" + address));

            // response200Header(dos, body.length);
            response200Header(dos, body.length, header.getOrDefault("Accept", "html"));
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");

            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    // 임시로 만든 헤더
    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String type) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");

            // 여기서 css, script, html 체크 하는건가본데?
            // script 체크 하는법 확인 해야함
            if(type.contains("script")) dos.writeBytes("Content-Type: text/script;charset=utf-8\r\n");
            else if(type.contains("text/css")) dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
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
