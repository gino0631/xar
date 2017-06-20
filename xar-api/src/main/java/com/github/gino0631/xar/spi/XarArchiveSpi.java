package com.github.gino0631.xar.spi;

import com.github.gino0631.common.io.InputStreamSupplier;
import com.github.gino0631.xar.XarArchive;

import java.io.IOException;
import java.nio.file.Path;

public interface XarArchiveSpi {
    XarArchive load(Path file) throws IOException;

    XarArchive load(InputStreamSupplier streamSupplier) throws IOException;
}
