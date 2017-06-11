package com.github.gino0631.xar.impl.jaxb;

import com.github.gino0631.xar.impl.jaxb.toc.Xar;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZoneOffset;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public final class ObjectFactory {
    private static final DatatypeFactory DATATYPE_FACTORY;
    private static final JAXBContext JAXB_CONTEXT;

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();

        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }

        try {
            JAXB_CONTEXT = JAXBContext.newInstance(Xar.class);

        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectFactory() {
    }

    public static XMLGregorianCalendar newXmlGregorianCalendar() {
        return DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar(TimeZone.getTimeZone(ZoneOffset.UTC)));
    }

    public static Xar unmarshal(InputStream is) throws IOException {
        try {
            Unmarshaller jaxbUnmarshaller = JAXB_CONTEXT.createUnmarshaller();
            return (Xar) jaxbUnmarshaller.unmarshal(is);

        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    public static void marshal(Xar xar, OutputStream os) throws IOException {
        try {
            Marshaller jaxbMarshaller = JAXB_CONTEXT.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            jaxbMarshaller.marshal(xar, os);

        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }
}
