from subprocess import call
from shutil import copy, copytree, make_archive, rmtree
from os import remove, rename

jlinkCommand = (r"jlink --output ./PDFTableExtractor/ "
                 "--no-man-pages "
                 "--no-header-files "
                 "--add-modules java.base,java.desktop,java.sql,java.net.http,jdk.crypto.ec "
                 "--compress=2")

print("Generating runtime")
call(jlinkCommand)

print("Copying files")
copy("icon.ico", "./PDFTableExtractor/icon.ico")
copy("createShortcut.vbs", "./PDFTableExtractor/createShortcut.vbs")
copytree("./target/lib", "./PDFTableExtractor/lib/app")

print("Creating jar")
call("jar cfm PDFTableExtractor.jar MANIFEST.MF -C target/classes module-info.class -C target/classes degubi")

print("Cleaning up")
rename("PDFTableExtractor.jar", "./PDFTableExtractor/PDFTableExtractor.jar")
remove("./PDFTableExtractor/release")

print("Creating Archive")
make_archive("./PDFTableExtractor", "zip", "./", "./PDFTableExtractor")
rmtree("./PDFTableExtractor")

input("\nDone, press enter")