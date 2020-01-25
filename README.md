### Downloading/Installation:
 - Go to releases tab & download the zip
 - Extract it somewhere
 - Run createShortcut.vbs
 - To bring up settings menu launch the shortcut on it's own
 - To extract the tables from pdf-s drag & drop the pdf files onto the shortcut

### Settings:
 <img src = images/settings.jpg width = 450 height = 375/>
 
 - Keep pages with rows/columns: Skip exporting all pages/sheets that doesn't meet the criteria
 - Skip empty columns: Different options for choosing column skipping methods
 - Page naming strategy: How to name pages/sheets in the excel file
 - Autosize columns: Resizes created columns before saving
 - Parallel file processing: Enables processing multiple pdf-s at the same time

### Building:
 - Need JDK 13+ installed
 - Need Python installed
 - Run ExportLibraries.launch in eclipse or run 'mvn dependency:copy-dependencies -DoutputDirectory=${project.build.directory}/lib'
 - Run build.py
 - PDFTableExtractor folder is built as in the release zip files