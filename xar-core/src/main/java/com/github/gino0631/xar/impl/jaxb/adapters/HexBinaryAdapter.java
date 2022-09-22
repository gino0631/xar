package com.github.gino0631.xar.impl.jaxb.adapters;

import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public final class HexBinaryAdapter extends XmlAdapter<String, byte[]> {
    private static final char[] HEX_CODE = "0123456789abcdef".toCharArray();

    @Override
    public byte[] unmarshal(String s) throws Exception {
        return (s != null) ? DatatypeConverter.parseHexBinary(s) : null;
    }

    @Override
    public String marshal(byte[] data) throws Exception {
        if (data != null) {
            StringBuilder r = new StringBuilder(data.length * 2);
            for (byte b : data) {
                r.append(HEX_CODE[(b >> 4) & 0xf]);
                r.append(HEX_CODE[(b & 0xf)]);
            }

            return r.toString();

        } else {
            return null;
        }
    }
}
