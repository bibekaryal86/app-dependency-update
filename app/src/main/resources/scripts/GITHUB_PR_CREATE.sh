#!/usr/bin/bash
# The above bash location was retrieved using `which bash` in raspberry pi

# Location of the repo
# echo "Process Id--$$"
repo_loc="$1"
branch_name="$2"

# Give access to current user
current_user=$(whoami)
chown -R "$current_user" "$repo_loc"

# Go to repo location or exit with message
cd "$repo_loc" || { echo "Repo Location Not Found"; exit 1; }

# echo "Current User--$current_user"
# echo "Current Location--$PWD"
# echo "Repo Location--$repo_loc"
# echo "Branch Name--$branch_name"

# Keeping this as fallback check
if [ "$PWD" != "$repo_loc" ]; then
    echo "Current Location and Repo Location are different"
    exit 1
fi

# Create new branch for updates
echo "Creating new branch"
git checkout -b "$branch_name" 2>&1

# Update dependencies
echo "Running npm check update"
ncu -u 2>&1
npm install

echo "Creating PR"
gh pr create -a "@me" -B "main" -H "$branch_name" -t "Dependencies Updated (https://bit.ly/app-dependency-update)" -b "Dependencies Updated (https://github.com/bibekaryal86/app-dependency-update)" 2>&1

echo "Finished"
