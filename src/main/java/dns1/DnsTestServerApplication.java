package dns1;

import org.springframework.context.annotation.Bean;

import io.vertx.core.ServiceHelper;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VertxFactory;

//@SpringBootApplication
public class DnsTestServerApplication {

//    public static void main(String[] args) {
//        SpringApplication.run(DnsTestServerApplication.class, args);
//    }

    @Bean
    public Vertx vertx() {
        final VertxFactory factory = ServiceHelper.loadFactory(VertxFactory.class);
        return factory.vertx();
    }
}
