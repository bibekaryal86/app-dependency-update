@echo off

REM Location of the repo
set repo_loc=%1
set branch_name=%2
set gradle_version=%3

REM Give access to current user
set current_user=%USERNAME%
icacls "%repo_loc%" /grant "%current_user%":(F) /T

REM Go to repo location or exit with message
cd /d "%repo_loc%" || (
    echo Repo Location Not Found
    exit /b 1
)

REM Keeping this as fallback check
if not "%CD%"=="%repo_loc%" (
    echo Current Location and Repo Location are different
    exit /b 1
)

REM Create new branch for updates
echo Creating new branch
git checkout -b "%branch_name%"

if not "%gradle_version%"=="" (
    echo Running Gradle Wrapper Update --- %gradle_version%
    chmod +x gradlew
    gradlew wrapper --gradle-version="%gradle_version%"
    REM Sometimes doesn't update on the first try
    gradlew wrapper --gradle-version="%gradle_version%"
) else (
    echo Skipping Gradle Wrapper Update --- %gradle_version%
)

REM Commit and push
echo Committing and pushing
set create_pr=no
git status | findstr /C:"nothing to commit" >nul 2>&1
if not errorlevel 1 (
    git add .
    git commit -am "Dependencies Updated (https://bit.ly/app-dependency-update)"
    git push origin -u "%branch_name%"
    set create_pr=yes
)

REM Create PR
if "%create_pr%"=="yes" (
    echo Creating PR
    gh pr create -a "@me" -B "main" -H "%branch_name%" -t "Dependencies Updated (https://bit.ly/app-dependency-update)" -b "Dependencies Updated (https://github.com/bibekaryal86/app-dependency-update)"
)

REM Cleanup
echo Cleaning up
git checkout main
git branch -D "%branch_name%"

echo Finished
