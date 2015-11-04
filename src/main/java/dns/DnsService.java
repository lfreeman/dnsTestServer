package dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.mapdb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.Record;

@Service
public class DnsService {
    private static final Logger logger = LoggerFactory.getLogger(DnsService.class);

    @Value("${delay.slow.time:0}")
    private long delaySlowTime;

    @Value("${delay.slowest.time:0}")
    private long delaySlowestTime;

    @Value("${delay.slow.cc:}")
    private String delaySlowCc;

    @Value("${delay.slowest.cc:}")
    private String delaySlowestCc;

    @Value("${cache.ttl:24}")
    private int cacheTtl;

    @Autowired
    private DB mapdb;

    private Map<String, Spid> spidMap;

    private final Map<Integer, MyMessage> pendingQ = new ConcurrentHashMap<>();

    private Map<Integer, Long> countryCodeToDelay = new HashMap<>();
    private final ScheduledExecutorService delayService = Executors.newScheduledThreadPool(5);
    private DatagramSocket requestSocket;

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

        spidMap = mapdb.treeMap("spid");

    }

    public void sendResponse(DNSMessage originalMessageIn, InetAddress address, int port, Record[] answers, int countryCode) {
        try {
            DNSMessage dnsMessageOut = new DNSMessage();
            dnsMessageOut.setQuestions(originalMessageIn.getQuestions());
            dnsMessageOut.setId(originalMessageIn.getId());
            dnsMessageOut.setQuery(true);
            dnsMessageOut.setRecursionDesired(true);
            dnsMessageOut.setRecursionAvailable(true);
            dnsMessageOut.setAnswers(answers);

            byte[] buf = dnsMessageOut.toArray();

            final DatagramPacket packetOut = new DatagramPacket(buf, buf.length, address, port);
            long elapsed = System.currentTimeMillis() - originalMessageIn.getReceiveTimestamp();

            long delay = getDelay(countryCode);
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

    public void setRequestSocket(DatagramSocket requestSocket) {
        this.requestSocket = requestSocket;
    }

    public Spid getCachedSpid(String mdn) {
        return spidMap.get(mdn);
    }

    public void setCachedSpid(String mdn, String spidStr) {
        long timeToLive = TimeUnit.HOURS.toMillis(this.cacheTtl);
        Spid spid = new Spid(mdn, spidStr, timeToLive);
        spidMap.put(mdn, spid);
        this.mapdb.commit();
    }

    public Map<Integer, MyMessage> getPendingQ() {
        return this.pendingQ;
    }
    
    public int getPendingQsize() {
        return this.pendingQ.size();
    }
    
    public int getCacheSize() {
        return this.spidMap.size();
    }
}
