module degubi.pdftableextractor.lib {
	exports pdftableextractorlib;

    requires java.sql;

    requires org.apache.pdfbox;
    requires org.apache.poi.poi;
    requires transitive org.apache.poi.ooxml;
    requires tabula;
}