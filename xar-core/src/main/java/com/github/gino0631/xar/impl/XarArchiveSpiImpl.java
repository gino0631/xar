package com.github.gino0631.xar.impl;

import com.github.gino0631.xar.XarArchive;
import com.github.gino0631.xar.io.InputStreamSupplier;
import com.github.gino0631.xar.spi.XarArchiveSpi;

import java.io.IOException;
import java.nio.file.Path;

public class XarArchiveSpiImpl implements XarArchiveSpi {
    @Override
    public XarArchive load(Path file) throws IOException {
        return new XarArchiveImpl(file);
    }

    @Override
    public XarArchive load(InputStreamSupplier streamSupplier) throws IOException {
        return new XarArchiveImpl(streamSupplier);
    }
}
