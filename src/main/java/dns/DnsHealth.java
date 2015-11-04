package dns;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.stereotype.Component;

@Component
public class DnsHealth extends AbstractHealthIndicator {

    @Autowired
    private DnsService dnsService;

    @Override
    protected void doHealthCheck(Builder builder) throws Exception {
        boolean status = true;
        if (status) {
            builder.up();
        } else {
            builder.down();
        }
        builder.withDetail("pendQ:", dnsService.getPendingQsize()).withDetail("cache:", dnsService.getCacheSize());
    }


}
