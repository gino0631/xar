# XAR
[![Build Status](https://travis-ci.org/gino0631/xar.svg?branch=master)](https://travis-ci.org/gino0631/xar)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.gino0631/xar-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.gino0631/xar-core)

A Java library for working with [XAR archives](https://en.wikipedia.org/wiki/Xar_(archiver)). It supports both reading and creation of archives, with optional signing and time stamping.

# Requirements
* Java 8

# Usage
The first step is to add a dependency on `com.github.gino0631:xar-core` to your project, for example, if using Maven:
```xml
<dependency>
  <groupId>com.github.gino0631</groupId>
  <artifactId>xar-core</artifactId>
  <version>...</version>
</dependency>
```

Interfaces for public use are contained in `com.github.gino0631.xar` and its subpackages, except for `impl` and `spi`.

To read an archive, load it using `XarArchive.load()`:
```java
try (XarArchive xar = XarArchive.load(...) {
  XarArchive.Header header = xar.getHeader();
  ...

  List<XarArchive.Entry> entries = xar.getEntries();
  ...
}
```

To create an archive, use `XarBuilder`:
```java
try (XarBuilder xarBuilder = XarBuilder.getInstance()) {
  XarBuilder.Container root = xarBuilder.getRoot();
  XarBuilder.File file = root.addFile("file.txt", EncodingAlgorithm.ZLIB, Files.newInputStream(...));
  
  XarBuilder.Directory dir = root.addDirectory("dir");
  dir.addFile(...);
  ...

  try (XarArchive xar = xarBuilder.build()) {
    xar.writeTo(...);
  }
}
```
If signing is required, set private key, certificates, and provider (if required) before building:
```java
xarBuilder.setSigning(privateKey, certificates, provider);
```
or, to sign and then to time stamp the signature:
```java
xarBuilder.setSigning(privateKey, certificates, provider, tsa);
```
