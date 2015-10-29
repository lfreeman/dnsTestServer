package dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

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

    @Value("${delay.slow.time:0}")
    private long delaySlowTime;

    @Value("${delay.slowest.time:0}")
    private long delaySlowestTime;

    @Value("${delay.slow.cc:}")
    private String delaySlowCc;

    @Value("${delay.slowest.cc:}")
    private String delaySlowestCc;

    private DatagramSocket socket;
    private final Semaphore semaphore = new Semaphore(1);

    private final AtomicBoolean ready = new AtomicBoolean(false);
    private DatagramSocket requestSocket;
    private Map<Integer, MyMessage> pendingQ = new ConcurrentHashMap<>();

    private final ScheduledExecutorService delayService = Executors.newScheduledThreadPool(5);
    private List<Integer> slowCountryCodes;
    private List<Integer> slowestCountryCodes;

    private Map<Integer, Long> countryCodeToDelay = new HashMap<>();

    public DnsResponseListener() {
        semaphore.acquireUninterruptibly();
    }

    @PostConstruct
    public void initialize() {
        if (delaySlowTime > 0) {
            for (int cc : toList(delaySlowCc)) {
                countryCodeToDelay.put(cc, delaySlowTime);
            }
        }
        if (delaySlowestTime > 0) {
            for (int cc : toList(delaySlowestCc)) {
                countryCodeToDelay.put(cc, delaySlowestTime);
            }
        }
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

            MyMessage userRequest = pendingQ.remove(dnsMessageIn.getId());
            if (userRequest == null) {
                logger.error("id {} is not found in pendingQ", dnsMessageIn.getId());
                return;
            }
            DNSMessage originalMessageIn = userRequest.getDnsMessage();

            DNSMessage dnsMessageOut = new DNSMessage();
            dnsMessageOut.setQuestions(originalMessageIn.getQuestions());
            dnsMessageOut.setId(originalMessageIn.getId());
            dnsMessageOut.setQuery(true);
            dnsMessageOut.setRecursionDesired(true);
            dnsMessageOut.setRecursionAvailable(true);
            dnsMessageOut.setAnswers(dnsMessageIn.getAnswers());

            byte[] buf = dnsMessageOut.toArray();

            final DatagramPacket packetOut = new DatagramPacket(buf, buf.length, userRequest.getAddress(), userRequest.getPort());
            long elapsed = System.currentTimeMillis() - originalMessageIn.getReceiveTimestamp();


            long delay = getDelay(userRequest.getCountryCode());
            long rest = delay - elapsed;
            if (rest < 0) {
                rest = 0;
            }
            this.delayService.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        requestSocket.send(packetOut);
                    } catch (IOException e) {
                        logger.error(e.getStackTrace().toString());
                    }
                }
            }, rest, TimeUnit.MILLISECONDS);

        } catch (IOException e) {
            logger.error(e.getStackTrace().toString());
        }
    }

    private long getDelay(Integer countryCode) {
        Long delay = countryCodeToDelay.get(countryCode);
        if (delay == null) {
            delay = 0L;
        }
        return delay;
    }

    public Map<Integer, MyMessage> getPendingQueue() {
        return this.pendingQ;
    }

    public void setRequestSocket(DatagramSocket requestSocket) {
        this.requestSocket = requestSocket;
    }

    private List<Integer> toList(String csv) {
        List<Integer> vlist = new ArrayList<>();

        if (csv == null) {
            return vlist;
        }
        String[] vArray = csv.split(",");
        try {
            for (String v : vArray) {
                vlist.add(Integer.valueOf(v));
            }
        } finally {
        }
        return vlist;
    }
}
