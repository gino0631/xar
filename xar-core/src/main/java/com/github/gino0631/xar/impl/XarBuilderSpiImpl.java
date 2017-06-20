package com.github.gino0631.xar.impl;

import com.github.gino0631.xar.XarBuilder;
import com.github.gino0631.xar.spi.XarBuilderSpi;

public class XarBuilderSpiImpl implements XarBuilderSpi {
    @Override
    public XarBuilder getInstance() {
        return new XarBuilderImpl();
    }
}
