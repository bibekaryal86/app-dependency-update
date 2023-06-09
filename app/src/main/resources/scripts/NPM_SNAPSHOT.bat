REM @echo off

REM Location of the repo
set repo_loc=%1
set branch_name=%2

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

REM Git pull
echo Pulling latest changes
git pull

REM Checkout branch
echo Checking out branch
git checkout "%branch_name%"

REM Run NPM Tests
echo Running NPM Tests
npm run test:u

REM Commit and push
echo Committing and pushing
git status | findstr /C:"nothing to commit" >nul 2>&1
if not errorlevel 1 (
    git add .
    git commit -am "Dependencies Updated (https://bit.ly/app-dependency-update)"
    git push
)

REM Cleaning up
echo Cleaning up
git checkout main
git branch -D "%branch_name%"

echo Finished
