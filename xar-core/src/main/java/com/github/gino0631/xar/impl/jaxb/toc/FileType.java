package com.github.gino0631.xar.impl.jaxb.toc;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum FileType {
    @XmlEnumValue("file")
    FILE,

    @XmlEnumValue("directory")
    DIRECTORY
}
