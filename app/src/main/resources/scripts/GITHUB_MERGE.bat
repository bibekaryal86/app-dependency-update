@echo off

REM Location of the repo
set repo_home=%1
set branch_name=%2

REM Give access to current user
set current_user=%USERNAME%
icacls "%repo_home%" /grant "%current_user%":(F) /T

REM Go to repo location or exit with message
cd /d "%repo_home%" || (
    echo Repo Home Not Found
    exit /b 1
)

REM Keeping this as fallback check
if not "%CD%"=="%repo_home%" (
    echo Current Location and Repo Home are different
    exit /b 1
)

REM Iterate through all subdirectories
for /d %%D in ("%repo_home%\*") do (
    REM If the current directory is a directory
    if exist "%%D" (
        if not "%%D"=="%repo_home%\logs" (
            REM Change to the subdirectory
            cd /d "%%D" || (
                echo Error 1
                exit /b 1
            )

            REM Iterate through all subdirectories of the current subdirectory
            for /d %%SD in ("%%D\*") do (
                REM If the current subdirectory is a directory
                if exist "%%SD" (
                    REM Change to the subdirectory
                    cd /d "%%SD" || (
                        echo Error 2
                        exit /b 2
                    )
                    echo %%SD

                    REM Check if build passed for PR of the branch
                    set pr_check=
                    for /f "delims=" %%I in ('gh pr checks "%branch_name%"') do set pr_check=%%I
                    echo %pr_check%

                    REM Merge pull request if checks passed
                    if not "%pr_check%"=="fail" (
                        set pr_merge=
                        for /f "delims=" %%I in ('gh pr merge "%branch_name%" -s -d') do set pr_merge=%%I
                        echo %pr_merge%
                    )

                    REM Change back to the current subdirectory
                    cd /d "%%D" || (
                        echo Error 3
                        exit /b 3
                    )
                )
            )

            REM Change back to the current directory
            cd /d "%repo_home%" || (
                echo Error 4
                exit /b 4
            )
        )
    )
)

echo GitHub Merge Finished
