package simple;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.Question;
import de.measite.minidns.Record;
import de.measite.minidns.Record.CLASS;
import de.measite.minidns.Record.TYPE;

public class DnsProcessor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DnsProcessor.class);

    private final DatagramPacket packetIn;
    private final DatagramSocket socket;
    private String remoteHost;
    private int remotePort;

    private final DatagramSocket responseListenerSocket;
    static private AtomicInteger messageId = new AtomicInteger(0);
    final private Map<Integer, DatagramPacket> pendingQ;


    public DnsProcessor(DatagramSocket socket, DatagramPacket packetIn, DatagramSocket responseListenerSocket, Map<Integer, DatagramPacket> pendingQ,
            String remoteHost, int remotePort) {
        this.packetIn = packetIn;
        this.socket = socket;
        this.responseListenerSocket = responseListenerSocket;
        this.pendingQ = pendingQ;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void run() {
        try {
            DNSMessage dnsMessageIn = DNSMessage.parse(packetIn.getData());
            String name = dnsMessageIn.getQuestions()[0].getName();
            final String mdn = extractMdn(dnsMessageIn);
            sendSpidLookup(mdn);
//            String spid = lookupSpid(mdn);
//
//            Record.CLASS clazz = Record.CLASS.IN;
//            Record.TYPE type = Record.TYPE.ATTR_STRING;
//            ATTRSTRING payloadData = new ATTRSTRING(spid);
//
//            DNSMessage dnsMessageOut = new DNSMessage();
//            dnsMessageOut.setQuestions(dnsMessageIn.getQuestions());
//            dnsMessageOut.setId(dnsMessageIn.getId());
//            dnsMessageOut.setQuery(true);
//            dnsMessageOut.setRecursionDesired(true);
//            dnsMessageOut.setRecursionAvailable(true);
//
//            Record answer = new Record(name, type, clazz, 60L, payloadData, false);
//            dnsMessageOut.setAnswers(new Record[] {answer});
//            byte[] buf = dnsMessageOut.toArray();
//
//            DatagramPacket packetOut = new DatagramPacket(buf, buf.length,
//                    packetIn.getAddress(), packetIn.getPort());
//
//            socket.send(packetOut);
        } catch (IOException e) {
            logger.error(e.getStackTrace().toString());
        }



    }

    private static Pattern mdnPattern = Pattern.compile("^([\\.\\d]+)\\.spid\\.e164\\.arpa$");

    private String extractMdn(String name) {
        String mdn = null;
        Matcher m = mdnPattern.matcher(name);
        if (m.matches()) {
            mdn = m.group(1).replace(".", "");
            mdn = new StringBuilder(mdn).reverse().toString();
        }
        return mdn;
    }

    private String extractMdn(DNSMessage dnsMessageIn) {
        String name = dnsMessageIn.getQuestions()[0].getName();
        return extractMdn(name);
    }


    private static HashMap<String, String> mdnSpidMap = new HashMap<>();

    private String lookupSpid(String mdn) {
        String spid = mdnSpidMap.get(mdn);
        if (spid == null) {
            spid = "V047";
        }

        return spid;
    }

    private void sendSpidLookup(String mdn) {

        DNSMessage dnsMessageOut = new DNSMessage();
        String name = nameFromMdn(mdn);
        Question q = new Question(name, TYPE.ANY, CLASS.IN);
        dnsMessageOut.setQuestions(new Question[] {q});
        dnsMessageOut.setAnswers(new Record[0]);
        dnsMessageOut.setRecursionDesired(true);
        dnsMessageOut.setId(getNewMessageId());


        try {
            byte[] buf = dnsMessageOut.toArray();
            InetAddress address = InetAddress.getByName(this.remoteHost);

            DatagramPacket packetOut = new DatagramPacket(buf, buf.length, address, this.remotePort);

            responseListenerSocket.send(packetOut);
            pendingQ.put(dnsMessageOut.getId(), packetIn);
        } catch (IOException e) {
            logger.error(e.getStackTrace().toString());
        }
    }

    private String nameFromMdn(String mdn) {
        char[] charA = mdn.toCharArray();
        ArrayUtils.reverse(charA);
        StringBuilder sb = new StringBuilder();
        for (char ch : charA) {
            sb.append(ch);
            sb.append(".");
        }
        sb.append("spid.e164.arpa.");
        return sb.toString();
    }

    public int getNewMessageId() {
        return messageId.incrementAndGet();
    }
}
