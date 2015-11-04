package dns;

import java.net.DatagramSocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
public class Main implements CommandLineRunner {

    @Autowired
    DnsRequestListener dnsRequestListener;

    @Autowired
    DnsResponseListener dnsResponseListener;

    @Override
    public void run(String... args) throws Exception {

        new Thread(dnsResponseListener).start();
        DatagramSocket responseSocket = dnsResponseListener.getSocket();
        dnsRequestListener.init(responseSocket);
        new Thread(dnsRequestListener).start();
        DatagramSocket requestSocket = dnsRequestListener.getSocket();
        dnsResponseListener.setRequestSocket(requestSocket);
    }
}
