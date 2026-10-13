$env:GRADLE_OPTS = ''
$env:JAVA_TOOL_OPTIONS = ''
Remove-Item build_log.txt -ErrorAction SilentlyContinue
Remove-Item build_exit.txt -ErrorAction SilentlyContinue
& .\gradlew.bat --no-daemon assembleDebug *>&1 | Out-File -FilePath build_log.txt -Encoding utf8
"EXITCODE=$LASTEXITCODE" | Out-File -FilePath build_exit.txt -Encoding ascii
