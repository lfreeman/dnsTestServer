package test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import dns1.DnsTestServerApplication;
import io.vertx.core.ServiceHelper;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VertxFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DnsTestServerApplication.class)
public class DnsTestServerApplicationTests {

    @Test
    public void contextLoads() {}

    @Bean
    public Vertx vertx() {
        final VertxFactory factory = ServiceHelper.loadFactory(VertxFactory.class);
        return factory.vertx();
    }
}
