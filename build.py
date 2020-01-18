from subprocess import call
from shutil import copy, copytree
from os import remove, rename

jlinkCommand = (r"jlink --output ./PDFToXLSX/ "
                 "--no-man-pages "
                 "--add-modules java.base,java.desktop "
                 "--no-header-files "
                 "--compress=2")

call(jlinkCommand)
copy("icon.ico", "./PDFToXLSX/icon.ico")
copy("createShortcut.vbs", "./PDFToXLSX/createShortcut.vbs")
copytree("lib", "./PDFToXLSX/lib/app")
call("jar cfm PDFToXLSX.jar MANIFEST.MF -C target/classes module-info.class -C target/classes degubi")
rename("PDFToXLSX.jar", "./PDFToXLSX/PDFToXLSX.jar")
remove("./PDFToXLSX/release")