package dns;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.Record;
import de.measite.minidns.record.ATTRSTRING;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.ServiceHelper;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.net.impl.SocketAddressImpl;
import io.vertx.core.spi.BufferFactory;

@Component
public class DnsServer {
    private static final Logger logger = LoggerFactory.getLogger(DnsServer.class);

    @Value("${server.por:6666}")
    private int port;

    private Vertx vertx;
    private DnsLookupService dnsLookupService;

    @Autowired
    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    @Autowired
    public void setDnsLookupService(DnsLookupService service) {
        this.dnsLookupService = service;
    }


    @PostConstruct
    public void createServer() {
        DatagramSocketOptions options = new DatagramSocketOptions();
        options.setReceiveBufferSize(512);
        options.setReceiveBufferSize(512);
        final DatagramSocket socket = vertx.createDatagramSocket(options);
        socket.listen(port, "0.0.0.0", new Handler<AsyncResult<DatagramSocket>>() {

            @Override
            public void handle(AsyncResult<DatagramSocket> asyncResult) {
                if (asyncResult.succeeded()) {
                    socket.handler(new Handler<DatagramPacket>() {

                        @Override
                        public void handle(DatagramPacket packet) {
                            final SocketAddressImpl sender = (SocketAddressImpl) packet.sender();

                            //DNSMessage dnsMessageIn;
                            try {
                                final DNSMessage dnsMessageIn = DNSMessage.parse(packet.data().getBytes());
                                final String mdn = extractMdn(dnsMessageIn);

                                vertx.executeBlocking(new Handler<Future<String>>() {

                                    @Override
                                    public void handle(Future<String> future) {
                                        String spid = dnsLookupService.lookupSpid(mdn);
                                        future.complete(spid);
                                    }

                                }, new Handler<AsyncResult<String>>() {

                                    @Override
                                    public void handle(AsyncResult<String> result) {
                                        DNSMessage dnsMessageOut = buildDnsMessageOut(dnsMessageIn, result.result());

                                        try {
                                            Buffer b = ServiceHelper.loadFactory(BufferFactory.class).buffer(dnsMessageOut.toArray());
                                            socket.send(b, sender.port(), sender.host(), new Handler<AsyncResult<DatagramSocket>>() {

                                                @Override
                                                public void handle(AsyncResult<DatagramSocket> event) {
                                                    // TODO Auto-generated method stub

                                                }
                                            });
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                });
                            } catch (IOException e) {
                                logger.error(e.getStackTrace().toString());
                            }
                        }
                    });
                } else {
                    logger.error("Listen failed", asyncResult.cause());
                }
            }
        });
    }

    DNSMessage buildDnsMessageOut(DNSMessage dnsMessageIn, String spid) {
        String name = dnsMessageIn.getQuestions()[0].getName();
        DNSMessage dnsMessageOut = new DNSMessage();
        dnsMessageOut.setQuestions(dnsMessageIn.getQuestions());
        dnsMessageOut.setId(dnsMessageIn.getId());
        dnsMessageOut.setQuery(true);
        dnsMessageOut.setRecursionDesired(true);
        dnsMessageOut.setRecursionAvailable(true);

        Record answer = new Record(name, Record.TYPE.ATTR_STRING, Record.CLASS.IN, 60L, new ATTRSTRING(spid), false);
        dnsMessageOut.setAnswers(new Record[] {answer});
        return dnsMessageOut;
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
}
