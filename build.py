from subprocess import call, DEVNULL
from shutil import copy, rmtree
from os import mkdir, remove, rename

# We need to generate a runtime by hand because some of the libraries are not modularized
jlinkCommand = (r'jlink --output ./runtime/ '
                 '--no-man-pages '
                 '--no-header-files '
                 '--add-modules java.base,java.desktop,java.sql,java.net.http,jdk.crypto.ec '
                 '--compress=2')

print('Generating runtime')
call(jlinkCommand)

print('Copying libraries')
call('mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=resources', shell = True, stdout = DEVNULL)

print('Creating PDFTableExtractor.jar')
call('mvn package -Dmaven.test.skip=true', shell = True, stdout = DEVNULL)
rename('target/PDFTableExtractor-1.0.jar', 'resources/PDFTableExtractor.jar')

print('Creating installer file')
call((r'"C:\Program Files\Java\jdk-14.0.2\bin\jpackage" --runtime-image runtime -i resources --main-class degubi.Main --main-jar PDFTableExtractor.jar '
      r'--name PDFTableExtractor --vendor Degubi --description PDFTableExtractor --icon icon.ico '
      r'--win-per-user-install --win-dir-chooser --win-shortcut --win-console'))

print('Cleaning up')
rename('PDFTableExtractor-1.0.exe', 'PDFTableExtractorInstaller.exe')
rmtree('resources')
rmtree('runtime')

print('\nDone!')