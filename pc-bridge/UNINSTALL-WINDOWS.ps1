$ErrorActionPreference = 'SilentlyContinue'
$dest = Join-Path $env:LOCALAPPDATA 'JARVIS-PC-Bridge'
$startup = [Environment]::GetFolderPath('Startup')
Remove-Item (Join-Path $startup 'JARVIS PC Bridge.lnk') -Force
Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*jarvis_pc_bridge.py*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
Remove-Item $dest -Recurse -Force
Write-Host 'JARVIS PC Bridge removed.'
