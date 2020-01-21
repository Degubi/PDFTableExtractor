from subprocess import call
from shutil import copy, copytree
from os import remove, rename

jlinkCommand = (r"jlink --output ./PDFToXLSX/ "
                 "--no-man-pages "
                 "--no-header-files "
                 "--add-modules java.base,java.desktop,java.sql "
                 "--compress=2")

print("Generating runtime")
call(jlinkCommand)
print("Copying files")
copy("icon.ico", "./PDFToXLSX/icon.ico")
copy("createShortcut.vbs", "./PDFToXLSX/createShortcut.vbs")
copytree("./target/lib", "./PDFToXLSX/lib/app")
print("Creating jar")
call("jar cfm PDFToXLSX.jar MANIFEST.MF -C target/classes module-info.class -C target/classes degubi")
print("Cleaning up")
rename("PDFToXLSX.jar", "./PDFToXLSX/PDFToXLSX.jar")
remove("./PDFToXLSX/release")
input("\nDone, press enter")