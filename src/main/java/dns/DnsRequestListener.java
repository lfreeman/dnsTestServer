package dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DnsRequestListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DnsRequestListener.class);
    private final int bufferSize = 512;// bytes

    private ExecutorService service = Executors.newFixedThreadPool(10);
    @Value("${request_listener.port:5555}")
    private int localPort;

    @Value("${remoteHost:localhost}")
    private String remoteHost;

    @Value("${remotePort:1053}")
    private int remotePort;

    @Autowired
    private DnsService dnsService;

    private DatagramSocket responseListenerSocket;
    private Map<Integer, MyMessage> pendQ;
    private final Semaphore semaphore = new Semaphore(1);

    private final AtomicBoolean ready = new AtomicBoolean(false);
    private DatagramSocket socket;


    public DnsRequestListener() {
        this.semaphore.acquireUninterruptibly();
    }

    public void init(DatagramSocket responseListenerSocket, Map<Integer, MyMessage> pendQ) {
        this.responseListenerSocket = responseListenerSocket;
        this.pendQ = pendQ;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(localPort)) {
            this.socket = socket;
            ready.set(true);
            this.semaphore.release();
            while (true) {
                byte[] buffer = new byte[bufferSize];

                DatagramPacket packetIn = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packetIn);
                    this.processRequest(socket, packetIn);
                } catch (IOException e) {
                    logger.error(e.getStackTrace().toString());
                }
            }

        } catch (SocketException e) {
            logger.error(e.getStackTrace().toString());
        }

    }

    private void processRequest(DatagramSocket socket, DatagramPacket packetIn) {
        this.service.execute(new DnsProcessor(packetIn, responseListenerSocket, pendQ, remoteHost, remotePort, dnsService));
    }

    public DatagramSocket getSocket() {
        if (!ready.get()) {
            semaphore.acquireUninterruptibly();
            semaphore.release();
        }
        return this.socket;
    }

}
