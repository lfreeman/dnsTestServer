package dns;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Spid implements Serializable {
    final String mdn;
    final String spid;
    final Long expirationTimeStamp;

    /**
     * 
     * @param mdn
     * @param spid
     * @param timeToLive in milliseconds
     */
    public Spid(String mdn, String spid, Long timeToLive) {
        this.mdn = mdn;
        this.spid = spid;
        this.expirationTimeStamp = System.currentTimeMillis() + timeToLive;
    }

    public String getMdn() {
        return this.mdn;
    }

    public String getSpid() {
        return this.spid;
    }

    public boolean isExpired() {
        return this.expirationTimeStamp <= System.currentTimeMillis();
    }
}
