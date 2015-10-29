package dns;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DnsTestServerApplication.class)
public class DnsTestServerApplicationTests {

    @Test
    public void contextLoads() {}

    @Bean
    public DB mapdb() {
        DB db = DBMaker.fileDB(new File("testdb"))
                .closeOnJvmShutdown()
                .fileMmapEnableIfSupported()
                .cacheHashTableEnable()
                .make();
        return db;
    }

}
