package simple;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.measite.minidns.DNSMessage;

@Component
public class DnsResponseListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DnsResponseListener.class);
    private final int bufferSize = 512;// bytes

    @Value("${response_listener.port:5555}")
    private int localPort;

    private DatagramSocket socket;
    private final Semaphore semaphore = new Semaphore(1);

    private final AtomicBoolean ready = new AtomicBoolean(false);
    private DatagramSocket requestSocket;
    private Map<Integer, DatagramPacket> pendingQ = new ConcurrentHashMap<>();


    public DnsResponseListener() {
        semaphore.acquireUninterruptibly();
    }

    @Override
    public void run() {
        try (DatagramSocket s = new DatagramSocket(localPort)) {
            ready.set(true);
            semaphore.release();
            this.socket = s;
            while (true) {
                byte[] buffer = new byte[bufferSize];

                DatagramPacket packetIn = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packetIn);
                    this.processResponse(packetIn);
                } catch (IOException e) {
                    logger.error(e.getStackTrace().toString());
                }
            }

        } catch (SocketException e) {
            logger.error(e.getStackTrace().toString());
        }

    }

    public DatagramSocket getSocket() {
        if (!ready.get()) {
            semaphore.acquireUninterruptibly();
            semaphore.release();
        }
        return this.socket;
    }

    private void processResponse(DatagramPacket packetIn) {
        try {
            DNSMessage dnsMessageIn = DNSMessage.parse(packetIn.getData());
            DatagramPacket originalPacket = pendingQ.remove(dnsMessageIn.getId());
            DNSMessage originalMessageIn = DNSMessage.parse(originalPacket.getData());


            DNSMessage dnsMessageOut = new DNSMessage();
            dnsMessageOut.setQuestions(originalMessageIn.getQuestions());
            dnsMessageOut.setId(originalMessageIn.getId());
            dnsMessageOut.setQuery(true);
            dnsMessageOut.setRecursionDesired(true);
            dnsMessageOut.setRecursionAvailable(true);

            dnsMessageOut.setAnswers(dnsMessageIn.getAnswers());
            byte[] buf = dnsMessageOut.toArray();

            DatagramPacket packetOut = new DatagramPacket(buf, buf.length,
                    originalPacket.getAddress(), originalPacket.getPort());

            requestSocket.send(packetOut);
        } catch (IOException e) {
            logger.error(e.getStackTrace().toString());
        }
    }

    public Map<Integer, DatagramPacket> getPendingQueue() {
        return this.pendingQ;
    }

    public void setRequestSocket(DatagramSocket requestSocket) {
        this.requestSocket = requestSocket;
    }

}
