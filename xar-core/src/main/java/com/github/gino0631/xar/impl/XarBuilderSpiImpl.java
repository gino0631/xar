package com.github.gino0631.xar.impl;

import com.github.gino0631.xar.XarBuilder;
import com.github.gino0631.xar.spi.XarBuilderSpi;

import java.io.IOException;

public class XarBuilderSpiImpl implements XarBuilderSpi {
    @Override
    public XarBuilder getInstance() {
        try {
            return new XarBuilderImpl();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
