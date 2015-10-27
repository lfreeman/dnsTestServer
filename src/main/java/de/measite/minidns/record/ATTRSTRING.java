package de.measite.minidns.record;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.measite.minidns.Record.TYPE;

public class ATTRSTRING implements Data {

    private String spid;

    public ATTRSTRING() {}

    public ATTRSTRING(String spid) {
        this.setSpid(spid);
    }


    public void setSpid(String spid) {
        this.spid = spid;
        try {
            spid.getBytes("UTF-8");
        } catch (Exception e) {
            /* Can't happen, UTF-8 IS supported */
            throw new RuntimeException("UTF-8 not supported", e);
        }
    }

    public String getSpid() {
        return spid;
    }

    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        byte[] buffer1;
        byte[] buffer2;
        try {
            buffer1 = "(VSPID".getBytes("UTF-8");
            buffer2 = spid.getBytes("UTF-8");
        } catch (Exception e) {
            /* Can't happen, UTF-8 IS supported */
            throw new RuntimeException("UTF-8 not supported", e);
        }
        try {
            dos.write(buffer1);
            dos.writeByte(buffer2.length);
            dos.write(buffer2);
            dos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    @Override
    public void parse(DataInputStream dis, byte[] data, int length)
            throws IOException {

        byte[] b = new byte[length];
        dis.readFully(b);
        String result = new String(b);
        spid = result.substring(6);

    }

    @Override
    public TYPE getType() {
        return TYPE.ATTR_STRING;
    }

    @Override
    public String toString() {
        return "\"" + spid + "\"";
    }
}
