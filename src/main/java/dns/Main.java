package dns;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Main implements CommandLineRunner {

    @Autowired
    UdpServer udpServer;
    
    @Override
    public void run(String... args) throws Exception {
        udpServer.start();
    }

}
