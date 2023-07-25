@echo off

rem Get the location of the repo home
set repo_home=%1
set delete_update_dependencies_only=%2

rem Give access to current user
rem This is equivalent to `chown -R "$current_user" "$repo_home"` in bash
rem echo Current user: %USERNAME%
rem echo Repo home: %repo_home%

rem Go to repo location or exit with message
cd %repo_home% || exit 1

rem Iterate through all subdirectories
for /d %%dir in (%repo_home%\*^) do (

  rem If the current directory is a directory
  if exist %%dir (

    rem If the current directory is not `logs`
    if not %%dir == logs (

      rem Change to the subdirectory
      cd %%dir

      rem Iterate through all subdirectories of the current subdirectory
      for /d %%sub_dir in (%%dir\*^) do (

        rem If the current directory is a directory
        if exist %%sub_dir (

          rem Change to the subdirectory
          cd %%sub_dir

          echo %%sub_dir

          rem Check out main and pull
          git checkout main
          git pull

          rem Get all branches
          set branches=
          for /f "delims=" %%b in ('git branch -a') do (
            set branches=!branches! %%b
          )

          rem Save to arrays for remote and local branches
          set remote_branches=
          set local_branches=
          for /f "delims=" %%b in ('%branches%') do (
            if "%%b" == *"/origin/"* (
              set remote_branches=!remote_branches! %%b
            ) else (
              set local_branches=!local_branches! %%b
            )
          )

          rem Delete remote branches
          for /f "delims=" %%b in ('%remote_branches%') do (
            if "%%b" != "remotes/origin/main" && "%%b" != *"remotes/origin/HEAD"* (
              git push origin -d %%b
            )
          )

          rem Delete local branches
          for /f "delims=" %%b in ('%local_branches%') do (
            if "%%b" != "main" (
              git branch -D %%b
            )
          )

          rem Prune old branches and pull again
          git fetch --prune
          git pull

          rem Change back to the current subdirectory
          cd ..
        )
      )

      rem Change back to the current directory
      cd ..
    )
  )
)

echo "Finished"
