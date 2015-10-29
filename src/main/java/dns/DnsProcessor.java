package dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.Question;
import de.measite.minidns.Record;
import de.measite.minidns.Record.CLASS;
import de.measite.minidns.Record.TYPE;

public class DnsProcessor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DnsProcessor.class);

    private final DatagramPacket packetIn;
    private String remoteHost;
    private int remotePort;

    private final DatagramSocket responseListenerSocket;
    static private AtomicInteger messageId = new AtomicInteger(0);
    final private Map<Integer, MyMessage> pendingQ;


    public DnsProcessor(DatagramPacket packetIn, DatagramSocket responseListenerSocket, Map<Integer, MyMessage> pendingQ,
            String remoteHost, int remotePort) {
        this.packetIn = packetIn;
        this.responseListenerSocket = responseListenerSocket;
        this.pendingQ = pendingQ;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    @Override
    public void run() {
        try {
            DNSMessage dnsMessageIn = DNSMessage.parse(packetIn.getData());
            sendSpidLookup(dnsMessageIn);
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

    private Integer extractCountryCode(String mdn) {
        
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        PhoneNumber numberProto;
        try {
            numberProto = phoneUtil.parse("+" + mdn, "");
            return numberProto.getCountryCode();
        } catch (NumberParseException e) {

        }
        return null;
    }

    private void sendSpidLookup(DNSMessage dnsMessageIn) {
        String mdn = extractMdn(dnsMessageIn);
        Integer cc = extractCountryCode(mdn);

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
            MyMessage message = new MyMessage(dnsMessageIn, packetIn.getAddress(), packetIn.getPort(), mdn, cc);
            pendingQ.put(dnsMessageOut.getId(), message);
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
