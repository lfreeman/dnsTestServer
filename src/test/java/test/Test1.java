package test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.dns.DnsClient;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class Test1 {

    private Vertx vertx;

    @Autowired
    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    @Test
    public void test() {
        DnsClient client = vertx.createDnsClient(6666, "10.69.4.240");


        client.lookup("sss", new Handler<AsyncResult<String>>() {

            @Override
            public void handle(AsyncResult<String> event) {
                if (event.succeeded()) {
                    System.out.println(event.result());
                } else {
                    System.out.println("Failed to resolve entry" + event.cause());
                }
            }
        });

    }

}
