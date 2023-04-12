#!/usr/bin/bash
# The above bash location was retrieved using `which bash` in raspberry pi

# Location of the repo
echo Process Id--$$
repo_loc="$1"
branch_name="$2"
gradle_version="$3"

# Give access to current user
current_user=$(whoami)
chown -R "$current_user" "$repo_loc"

# Go to repo location or exit with message
cd "$repo_loc" || { echo "Repo Location Not Found"; exit 1; }

echo Current User--"$current_user"
echo Current Location--"$PWD"
echo Repo Location--"$repo_loc"
echo Branch Name--"$branch_name"
echo Gradle Version--"$gradle_version"

# Keeping this as fallback check
if [ "$PWD" != "$repo_loc" ]; then
    echo "Current Location and Repo Location are different"
    exit 1
fi

echo "Pulling new changes 1"
git pull

# Create new branch for updates
echo "Creating new branch"
git checkout -b "$branch_name"
echo "Running Gradle Wrapper Update"
chmod +x gradlew
./gradlew wrapper --gradle-version="$gradle_version"
# Sometimes doesn't update on the first try
./gradlew wrapper --gradle-version="$gradle_version"

# Commit and push
echo "Committing and pushing"
create_pr="no"
if ! git status | grep "nothing to commit" > /dev/null 2>&1; then
	git add .
	git commit -am 'Gradle Wrapper Updated (Auto)'
	git push origin -u "$branch_name"
	create_pr="yes"
fi

# Create PR
if [ $create_pr = "yes" ]; then
	echo "Creating PR"
	gh pr create -a "@me" -B "main" -H "$branch_name" -t "Gradle Wrapper Updated (Auto)" -b "Gradle Wrapper Updated (Auto)"
fi

# Cleanup
echo "Cleaning up"
git checkout main
git branch -D "$branch_name"

echo "Pulling new changes 2"
git pull

echo "Finished"
