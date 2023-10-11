package pdftableextractorweb.services;

import java.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pdftableextractorlib.*;

@RestController
public final class Extractor {

    @PostMapping(value = "/extract", consumes = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> extract(@RequestBody byte[] pdfData,
                                          @RequestParam("autosizeColumns") boolean autosizeColumns,
                                          @RequestParam("pageNamingStrategy") int pageNamingStrategy,
                                          @RequestParam("emptyColumnSkipMethod") int emptyColumnSkipMethod,
                                          @RequestParam("emptyRowSkipMethod") int emptyRowSkipMethod,
                                          @RequestParam("pageRowNumberFilterMethod") int pageRowNumberFilterMethod,
                                          @RequestParam("pageRowNumberFilterValue") int pageRowNumberFilterValue,
                                          @RequestParam("pageColumnNumberFilterMethod") int pageColumnNumberFilterMethod,
                                          @RequestParam("pageColumnNumberFilterValue") int pageColumnNumberFilterValue) throws IOException {

        try(var excelOutput = PDFTableExtractor.extract(autosizeColumns, emptyColumnSkipMethod, emptyRowSkipMethod,
                                                        PDFTableExtractor.getComparisonFunction(pageRowNumberFilterMethod, pageRowNumberFilterValue),
                                                        PDFTableExtractor.getComparisonFunction(pageColumnNumberFilterMethod, pageColumnNumberFilterValue),
                                                        PDFTableExtractor.getPageNamingFunction(pageNamingStrategy), pdfData);

            var result = new ByteArrayOutputStream()) {

            excelOutput.write(result);

            return ResponseEntity.ok(result.toByteArray());
        }
    }
}