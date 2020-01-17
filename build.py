from subprocess import call
from shutil import copy, copytree
from os import remove, rename

jlinkCommand = (r"jlink --output ./PDFToXLSX/ "
                 "--no-man-pages "
                 "--add-modules java.base,java.desktop "
                 "--no-header-files "
                 "--compress=2")

call(jlinkCommand)
copy("PDFToXLSX.bat", "./PDFToXLSX/PDFToXLSX.bat")
copytree("lib", "./PDFToXLSX/lib/app")
call("jar cfm PDFToXLSX.jar MANIFEST.MF -C target/classes module-info.class -C target/classes degubi")
rename("PDFToXLSX.jar", "./PDFToXLSX/PDFToXLSX.jar")
remove("./PDFToXLSX/release")