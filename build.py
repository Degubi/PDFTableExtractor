from subprocess import call, DEVNULL
from shutil import rmtree
from os import rename

def get_app_version():
    with open('src/main/java/degubi/Main.java') as main_file:
        main_file_content = main_file.read()
        version_field_begin = main_file_content.index('VERSION')
        version_begin = main_file_content.index('"', version_field_begin) + 1
        version_end = main_file_content.index(';', version_begin) - 1

        return main_file_content[version_begin : version_end]


app_version = get_app_version()
jlinkCommand = ('jlink --output ./runtime/ '
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
call(('jpackage --runtime-image runtime -i resources --main-class degubi.Main --main-jar PDFTableExtractor.jar '
      f'--name PDFTableExtractor --vendor Degubi --app-version {app_version} --description PDFTableExtractor --icon icon.ico '
      '--win-per-user-install --win-dir-chooser --win-shortcut --win-console'))

print('Cleaning up')
rename(f'PDFTableExtractor-{app_version}.exe', 'PDFTableExtractorInstaller.exe')
rmtree('resources')
rmtree('runtime')

print('\nDone!')