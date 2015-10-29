package dns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DnsTestServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DnsTestServerApplication.class, args);
    }

//    @Bean
//    public Vertx vertx() {
//        final VertxFactory factory = ServiceHelper.loadFactory(VertxFactory.class);
//        return factory.vertx();
//    }
}
