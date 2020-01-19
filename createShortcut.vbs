Set Shell = CreateObject("WScript.Shell")
DesktopPath = Shell.SpecialFolders("Desktop")
scriptPath = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)
Set link = Shell.CreateShortcut(DesktopPath & "\PDFToXLSX.lnk")
link.Arguments = "-jar " & """" & scriptPath & "\PDFToXLSX.jar" & """"
link.TargetPath = scriptPath & "\bin\java.exe"
link.IconLocation = scriptPath & "\icon.ico"
link.Save