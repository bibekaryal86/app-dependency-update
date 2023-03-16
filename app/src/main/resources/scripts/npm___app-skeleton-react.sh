#!/usr/bin/bash
# The above bash location was retrieved using `which bash` in raspberry pi

# Location of the repo
script_file_name=$(basename $BASH_SOURCE)
repo_name=${script_file_name%.*}
echo Repo Home from Input Parameter -- $1
repo_loc=$1$repo_name

# Give access to current user
current_user=$(whoami)
chown -R $current_user $repo_loc
cd $repo_loc

echo Current User -- $current_user
echo Current Location -- $PWD
echo Repo Location -- $repo_loc

# Create new branch for updates
echo Creating new branch
branch_name="update_dependencies_"$(date +%F)
git checkout -b $branch_name

# Update dependencies
echo Running npm update
npm update --save

# Commit and push
echo Committing and pushing
create_pr="no"
if ! git status | grep "nothing to commit" > /dev/null 2>&1; then
	git add .
	git commit -am 'Updated Dependencies'
	git push origin -u $branch_name
	create_pr="yes"
fi

# Create PR
if [ $create_pr = "yes" ]; then
	echo Creating PR
	gh pr create -a "@me" -B "main" -H $branch_name -t "Update Dependencies" --fill
fi

# Cleanup
echo Cleaning up
git checkout main
git branch -D $branch_name

echo Finished
