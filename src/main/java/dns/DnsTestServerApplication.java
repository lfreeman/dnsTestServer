package dns;

import java.io.File;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DnsTestServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DnsTestServerApplication.class, args);
    }

    @Bean
    public DB getMapdb() {
        DB db = DBMaker.fileDB(new File("testdb"))
                .closeOnJvmShutdown()
                .fileMmapEnableIfSupported()
                .cacheHashTableEnable()
                .make();
        return db;
    }
}
