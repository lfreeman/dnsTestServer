package test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.vertx.core.ServiceHelper;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VertxFactory;

@SpringBootApplication
public class TestApplication {
    @Bean
    public Vertx vertx() {
        final VertxFactory factory = ServiceHelper.loadFactory(VertxFactory.class);
        return factory.vertx();
    }
}
