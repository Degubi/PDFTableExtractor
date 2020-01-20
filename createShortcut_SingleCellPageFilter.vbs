Set Shell = CreateObject("WScript.Shell")
scriptPath = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)
Set link = Shell.CreateShortcut(Shell.SpecialFolders("Desktop") & "\PDFToXLSX_EmptyPageFiltered.lnk")
link.Arguments = "-jar " & """" & scriptPath & "\PDFToXLSX.jar" & """" & " -SingleCell"
link.TargetPath = scriptPath & "\bin\java.exe"
link.IconLocation = scriptPath & "\icon.ico"
link.Description = "Exporter with Single Cell Page Filter"
link.Save