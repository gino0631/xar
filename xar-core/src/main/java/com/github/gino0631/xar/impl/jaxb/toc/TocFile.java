package com.github.gino0631.xar.impl.jaxb.toc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class TocFile implements TocFileHolder {
    @XmlAttribute(name = "id", required = true)
    protected long id;

    @XmlElement(name = "name")
    protected String name;

    @XmlElement(name = "type")
    protected FileType type;

    @XmlElement(name = "data")
    protected FileData data;

    @XmlElement(name = "file")
    protected List<TocFile> files;

    public TocFile() {
    }

    public TocFile(long id, String name, FileType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public TocFile(long id, String name, FileType type, FileData data) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public FileData getData() {
        return data;
    }

    public void setData(FileData data) {
        this.data = data;
    }

    public List<TocFile> getFiles() {
        return files;
    }

    public void setFiles(List<TocFile> files) {
        this.files = files;
    }
}
