open module degubi.pdftableextractor.web {
    requires java.instrument;

    requires spring.beans;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.core;
    requires spring.web;

    requires degubi.pdftableextractor.lib;
}