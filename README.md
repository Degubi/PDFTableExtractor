# Downloading & Installing:
- Go to <a href = "https://github.com/Degubi/PDFTableExtractor/releases">releases menu</a> & download the latest installer.exe
- Run the installer (don't install to ProgramFiles).
- PDFTableExtractor shortcut appears on the Desktop.

# Usage with example:
- a, Drag & drop 1 or more pdf-s onto the Desktop shortcut
- b, Right click on the pdf and select the extract option (must enable in settings)
- Cmd appears, printing information about the processing
- XLSX files are getting created in the same directory where the pdf-s were located
- For customizing output check out the Settings wiki page
- Example input & output:

![image2](https://user-images.githubusercontent.com/13366932/73137187-c59cb200-4055-11ea-9f99-7e75cbb47449.jpg)

# Settings:
To bring up the settings menu start the desktop icon normally

 - <ins>Keep pages with rows/columns</ins>: Skip exporting all pages/sheets that doesn't meet the criteria
 - <ins>Skip empty rows/columns</ins>: Different options for choosing row/column skipping methods
 - <ins>Page naming strategy</ins>: How to name pages/sheets in the excel file
 - <ins>Autosize columns</ins>: Resizes created columns before saving
 - <ins>Parallel file processing</ins>: Enables processing multiple pdf-s at the same time
 - <ins>Context menu</ins>: When turned on an extraction option appers in the right click menu of pdf files

![image2](https://user-images.githubusercontent.com/13366932/89641512-1b2e0300-d8b2-11ea-863b-ff01afd3067c.png)

# Updating:
- When a new version is available, a message appears in the console saying that the local version is out of date.
- To update go to the releases
- Download the new installer
- Uninstall the old one
- Install the new one

# Reporting bugs:
- Create a new issue with a descriptive title
- Try to include more information. e.g: the pdf you tried to extract (if you're allowed to), your settings, error.txt.
- If the expected output is wrong, demonstrate what the expected output would be and what the output of the app was
- When a program error occurs, a file named 'error.txt' gets created in the directory of the application

# Requesting new features:
- Create an issue what feature/filter you need, give it a descriptive name
- Write a short description what the feature/filter would do
- Post screenshots of the input and the expected output