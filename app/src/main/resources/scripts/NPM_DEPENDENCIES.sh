#!/usr/bin/bash
# The above bash location was retrieved using `which bash` in raspberry pi

# Location of the repo
# echo "Process Id--$$"
repo_loc="$1"
branch_name="$2"
npm_skips="$3"

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

echo "Checkout main branch"
git checkout main 2>&1
# Create new branch for updates
echo "Creating new branch"
git checkout -b "$branch_name" 2>&1

# Update dependencies
echo "Running npm check update"
ncu -u -x "$npm_skips" 2>&1
npm install --package-lock-only  2>&1

# Commit and push
echo "Committing and pushing"
create_pr="no"
if ! git status | grep "nothing to commit" > /dev/null 2>&1; then
	git add . 2>&1
	git commit -am 'Dependencies Updated (https://bit.ly/app-dependency-update)' 2>&1
	git push origin -u "$branch_name" 2>&1
	create_pr="yes"
fi

# Create PR
if [ $create_pr = "yes" ]; then
	echo "Creating PR"
	gh pr create -a "@me" -B "main" -H "$branch_name" -t "Dependencies Updated (https://bit.ly/app-dependency-update)" -b "Dependencies Updated (https://github.com/bibekaryal86/app-dependency-update)" 2>&1
fi

# Cleanup
echo "Cleaning up"
git checkout main 2>&1
git branch -D "$branch_name" 2>&1

echo "Finished"
