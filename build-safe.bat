@echo off
setlocal
call .\gradle-9.4.1\bin\gradle.bat clean build -PhaVariant=safe
