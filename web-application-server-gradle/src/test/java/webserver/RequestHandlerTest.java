package webserver;

import model.User;
import org.junit.jupiter.api.Test;
import util.HttpRequestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestHandlerTest {
    String url = "GET /user/create?userId=1&password=2&name=3&email=4 HTTP/1.1";
    @Test
    void firstUrlParse(){

        // url을 체크 하는데, 수동적으로 체크를 해야하게 일단 만들어서 진행을 해야하나 ?
        // RequestHandler에서 구현을 하면 결합성이 높아지지 않나 ?

        String[] urlData = url.split(" ");
        String[] url = urlData[1].split("\\?");

        System.out.println(url[0]);

        // 이름=값 맵으로 바꿔줬는데
        Map temp = HttpRequestUtils.parseQueryString(url[1]);

        // 이제 user클래스에 담아야하는데 흠..

        // 1. 그냥 객체 생성 후 강제로 때려 넣는다.
        User user = new User(temp.get("userId").toString(), temp.get("password").toString(), temp.get("name").toString(), temp.get("email").toString());
        System.out.println(user.toString());

        // 2. user클래스를 읽어서 때려 넣는다.
        // - 이게 setter를 읽어서 넣는건가 ?

        // 2번이 spring에서 reqeust 메소드에 class 넣으면 맵핑되는 구조인듯 ?
    }
}