package dns;

import java.net.DatagramSocket;
import java.util.Map;

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
        Map<Integer, MyMessage> pendQ = dnsResponseListener.getPendingQueue();
        dnsRequestListener.init(responseSocket, pendQ);
        new Thread(dnsRequestListener).start();
        DatagramSocket requestSocket = dnsRequestListener.getSocket();
        dnsResponseListener.setRequestSocket(requestSocket);
    }

}
