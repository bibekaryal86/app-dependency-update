#!/usr/bin/bash
# The above bash location was retrieved using `which bash` in raspberry pi

# Location of the repo
echo Process Id--$$
repo_loc="$1"
branch_name="$2"

# Give access to current user
current_user=$(whoami)
chown -R "$current_user" "$repo_loc"

# Go to repo location or exit with message
cd "$repo_loc" || { echo "Repo Location Not Found"; exit 1; }

echo Current User--"$current_user"
echo Current Location--"$PWD"
echo Repo Location--"$repo_loc"
echo Branch Name--"$branch_name"

# Keeping this as fallback check
if [ "$PWD" != "$repo_loc" ]; then
    echo "Current Location and Repo Location are different"
    exit 1
fi

# Check if build passed for PR of the branch
echo "Checking if all checks/workflows have completed"
pr_check=$(gh pr checks "$branch_name")
echo "$pr_check"

if [[ ("$pr_check" != *"fail"*) ]]; then
	pr_merge=$(gh pr merge "$branch_name" -s -d)
	echo "$pr_merge"
fi

echo "Pulling new changes"
git pull
