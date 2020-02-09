Set Shell = CreateObject("WScript.Shell")
scriptPath = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)
Set link = Shell.CreateShortcut(Shell.SpecialFolders("Desktop") & "\PDFTableExtractor.lnk")
link.Arguments = "-jar " & """" & scriptPath & "\PDFTableExtractor.jar" & """"
link.TargetPath = scriptPath & "\bin\java.exe"
link.IconLocation = scriptPath & "\icon.ico"
link.WorkingDirectory = scriptPath
link.Save