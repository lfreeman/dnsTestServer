package dns;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.Record;
import de.measite.minidns.record.ATTRSTRING;
import ru.kitsu.dnsproxy.parser.DNSParseException;

@Component
public class UdpServer extends Thread {

    @Value("${port:5555}")
    private int port;
    private SocketAddress client;
    private ByteBuffer byteBufferWrite;
    static final int MAX_PACKET_SIZE = 512;

    private void handleRead(SelectionKey selectionKey) throws IOException, DNSParseException {
        System.out.println("handleRead");
        DatagramChannel channel = (DatagramChannel) selectionKey.channel();
        ByteBuffer buffer = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);
        buffer.clear();
        client = channel.receive(buffer);
        buffer.flip();
        // final DNSMessage message = DNSMessage.parse(buffer, false);
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes, 0, bytes.length);
        DNSMessage dnsMessageIn = DNSMessage.parse(bytes);

        String name = dnsMessageIn.getQuestions()[0].getName();
        String mdn = extractMdn(name);
        String spid = getSpid(mdn);

        Record.CLASS clazz = Record.CLASS.IN;
        Record.TYPE type = Record.TYPE.ATTR_STRING;
        ATTRSTRING payloadData = new ATTRSTRING(spid);

        DNSMessage dnsMessageOut = new DNSMessage();
        dnsMessageOut.setQuestions(dnsMessageIn.getQuestions());
        dnsMessageOut.setId(dnsMessageIn.getId());
        dnsMessageOut.setQuery(true);
        dnsMessageOut.setRecursionDesired(true);
        dnsMessageOut.setRecursionAvailable(true);

        Record answer = new Record(name, type, clazz, 60L, payloadData, false);
        dnsMessageOut.setAnswers(new Record[] {answer});
        byte[] buf = dnsMessageOut.toArray();
        byteBufferWrite = ByteBuffer.wrap(buf);


        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    private static HashMap<String, String> mdnSpidMap = new HashMap<>();


    private static String getSpid(String mdn) {
        String mdnPattern = mdn.substring(0, 8);

        // String spid = mdnSpidMap.get(mdn);
        String spid = mdnSpidMap.get(mdnPattern);
        if (spid == null) {
            spid = "V047";
        }


        return spid;
    }

    @Override
    public void run() {

        SocketAddress socetAddress = new InetSocketAddress("localhost", port);
        try (DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)) {
            channel.configureBlocking(false);
            channel.bind(socetAddress);

            final Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
            doSelect(channel, selector);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doSelect(DatagramChannel channel, Selector selector) throws IOException {
        while (true) {
            int selectionCount = selector.select();
            if (selectionCount != 0) {
                processNioSelections(selectionCount, channel, selector);
            }
        }
    }

    private void processNioSelections(int selectionCount, DatagramChannel channel, Selector selector) {
        Iterator<SelectionKey> channelsIterator = selector.selectedKeys().iterator();

        while (channelsIterator.hasNext()) {
            SelectionKey selectionKey = channelsIterator.next();
            channelsIterator.remove();
            if (!selectionKey.isValid()) {
                continue;
            }

            try {
                if (selectionKey.isReadable()) {
                    try {
                        handleRead(selectionKey);
                    } catch (IOException | DNSParseException e) {
                        e.printStackTrace();
                    }
                } else if (selectionKey.isWritable()) {
                    try {
                        handleWrite(selectionKey);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (CancelledKeyException e) {
                e.printStackTrace();
            }

        }
    }

    private void handleWrite(SelectionKey selectionKey) throws IOException {
        DatagramChannel channel = (DatagramChannel) selectionKey.channel();
        channel.send(byteBufferWrite, client);
        selectionKey.interestOps(SelectionKey.OP_READ);
    }

    private static Pattern mdnPattern = Pattern.compile("^([\\.\\d]+)\\.spid\\.e164\\.arpa$");

    private static String extractMdn(String name) {
        String mdn = null;
        Matcher m = mdnPattern.matcher(name);
        if (m.matches()) {
            mdn = m.group(1).replace(".", "");
            mdn = new StringBuilder(mdn).reverse().toString();
        }
        return mdn;
    }

}
