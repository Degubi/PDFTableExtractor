Set Shell = CreateObject("WScript.Shell")
scriptPath = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)
Set link = Shell.CreateShortcut(Shell.SpecialFolders("Desktop") & "\PDFToXLSX_Unfiltered.lnk")
link.Arguments = "-jar " & """" & scriptPath & "\PDFToXLSX.jar" & """"
link.TargetPath = scriptPath & "\bin\java.exe"
link.IconLocation = scriptPath & "\icon.ico"
link.Description = "Exporter without filters"
link.Save