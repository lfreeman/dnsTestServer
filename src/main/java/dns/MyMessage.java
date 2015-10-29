package dns;

import java.net.InetAddress;

import de.measite.minidns.DNSMessage;

public class MyMessage {
    private final DNSMessage dnsMessage;
    private final int port;
    private final InetAddress address;
    private final String mdn;
    private final Integer cc;

    public MyMessage(DNSMessage dnsMessage, InetAddress address, int port, String mdn, Integer cc) {
        this.dnsMessage = dnsMessage;
        this.address = address;
        this.port = port;
        this.mdn = mdn;
        this.cc = cc;
    }

    public DNSMessage getDnsMessage() {
        return this.dnsMessage;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public Integer getCountryCode() {
        return this.cc;
    }

}
