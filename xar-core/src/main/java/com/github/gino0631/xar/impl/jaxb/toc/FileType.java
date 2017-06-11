package com.github.gino0631.xar.impl.jaxb.toc;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum FileType {
    @XmlEnumValue("file")
    FILE,

    @XmlEnumValue("directory")
    DIRECTORY
}
