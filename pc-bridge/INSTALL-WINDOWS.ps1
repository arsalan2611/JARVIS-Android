$ErrorActionPreference = 'Stop'
$src = Split-Path -Parent $MyInvocation.MyCommand.Path
$dest = Join-Path $env:LOCALAPPDATA 'JARVIS-PC-Bridge'
New-Item -ItemType Directory -Force -Path $dest | Out-Null
Copy-Item (Join-Path $src 'jarvis_pc_bridge.py') (Join-Path $dest 'jarvis_pc_bridge.py') -Force
$token = Read-Host 'Enter a strong JARVIS PC token'
if ([string]::IsNullOrWhiteSpace($token)) { throw 'Token cannot be empty.' }
$launcher = @"
@echo off
set JARVIS_PC_HOST=0.0.0.0
set JARVIS_PC_PORT=8765
set JARVIS_PC_TOKEN=$token
python "$dest\jarvis_pc_bridge.py"
"@
Set-Content -Path (Join-Path $dest 'start-bridge.cmd') -Value $launcher -Encoding ASCII
$startup = [Environment]::GetFolderPath('Startup')
$shortcutPath = Join-Path $startup 'JARVIS PC Bridge.lnk'
$ws = New-Object -ComObject WScript.Shell
$sc = $ws.CreateShortcut($shortcutPath)
$sc.TargetPath = (Join-Path $dest 'start-bridge.cmd')
$sc.WorkingDirectory = $dest
$sc.WindowStyle = 7
$sc.Save()
Write-Host "Installed to $dest"
Write-Host 'The bridge will start automatically when you sign in.'
Write-Host 'Windows Firewall may ask for network permission the first time.'
