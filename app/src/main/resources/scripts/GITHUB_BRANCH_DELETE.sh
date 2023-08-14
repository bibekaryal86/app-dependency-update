#!/usr/bin/bash
# The above bash location was retrieved using `which bash` in raspberry pi

# Location of the repo home
# echo "Process Id--$$"
repo_home="$1"
delete_update_dependencies_only="$2"

# Give access to current user
current_user=$(whoami)
chown -R "$current_user" "$repo_home"

# Go to repo location or exit with message
cd "$repo_home" || { echo "Repo Location Not Found"; exit 1; }

# echo "Current User--$current_user"
# echo "Current Location--$PWD"
# echo "Repo Home--$repo_home"

# Keeping this as fallback check
if [ "$PWD" != "$repo_home" ]; then
  echo "Current Location and Repo Home are different"
  exit 1
fi

# Iterate through all subdirectories
for dir in "$repo_home"/*
do
  # If the current directory is a directory
  if [ -d "$dir" ]
  then
    if [[ ! "$dir" == *"logs"* ]];
    then
      # Change to the subdirectory
      cd "$dir" || { echo "Error 1"; exit 1; }
      # Iterate through all subdirectories of the current subdirectory
      for sub_dir in "$dir"/*
      do
        # If the current subdirectory is a directory
        if [ -d "$sub_dir" ]
        then
          # Change to the subdirectory
          cd "$sub_dir" || { echo "Error 2"; exit 2; }
          echo "$sub_dir"
          # git checkout main and pull
          git checkout main 2>&1
          git pull 2>&1
          # get all branches
          IFS=$'\n' branches=($(git branch -a))
          # Save to arrays for remote and local branches
          remote_branches=()
          local_branches=()

          for branch in "${branches[@]}";
          do
            # Remove leading whitespace
            branch="${branch#"${branch%%[![:space:]]*}"}"
            # Remove trailing whitespace
            branch="${branch%"${branch##*[![:space:]]}"}"

            if [[ "$branch" == *"/origin/"* ]];
            then
              remote_branches+=("$branch")
            else
              # Remove '* ' prefix from local branch
              branch="${branch#\* }"
              local_branches+=("$branch")
            fi
          done

          for branch in "${remote_branches[@]}";
          do
            if [[ "$branch" != "remotes/origin/main" ]] && [[ "$branch" != *"remotes/origin/HEAD"* ]];
            then
              branch="${branch#remotes/origin/}"
			        if [[ "$delete_update_dependencies_only" == "true" ]];
			        then
				        if [[ "$branch" == *"update_dependencies"* ]];
				        then
					        git push origin -d "$branch" 2>&1
                fi
			        else
				        git push origin -d "$branch" 2>&1
			        fi
            fi
          done

          for branch in "${local_branches[@]}";
          do
            if [[ "$branch" != "main" ]];
            then
              if [[ "$delete_update_dependencies_only" == "true" ]];
              then
                if [[ "$branch" == *"update_dependencies"* ]];
                then
                  git branch -D "$branch" 2>&1
                fi
              else
                git branch -D "$branch" 2>&1
              fi
            fi
          done

		  # prune old branches and pull again
		  git fetch --prune 2>&1
		  git pull 2>&1
          # Change back to the current subdirectory
          cd "$dir" || { echo "Error 3"; exit 3; }
        fi
      done

      # Change back to the current directory
      cd "$repo_home" || { echo "Error 4"; exit 4; }
    fi
  fi
done

echo "Finished"
