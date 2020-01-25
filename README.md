### Downloading/Installation:
 - Go to releases tab & download the zip
 - Extract it somewhere
 - Run createShortcut.vbs
 - To bring up settings menu launch the shortcut on it's own
 - To extract the tables from pdf-s drag & drop the pdf files onto the shortcut

### Settings:
 <img src = images/settings.jpg width = 450 height = 375/>
 
 - <ins>Keep pages with rows/columns</ins>: Skip exporting all pages/sheets that doesn't meet the criteria
 - <ins>Skip empty columns</ins>: Different options for choosing column skipping methods
 - <ins>Page naming strategy</ins>: How to name pages/sheets in the excel file
 - <ins>Autosize columns</ins>: Resizes created columns before saving
 - <ins>Parallel file processing</ins>: Enables processing multiple pdf-s at the same time

### Building:
 - Need JDK 13+ installed
 - Need Python installed
 - Run ExportLibraries.launch in eclipse or run 'mvn dependency:copy-dependencies -DoutputDirectory=${project.build.directory}/lib'
 - Run build.py
 - PDFTableExtractor.zip is built