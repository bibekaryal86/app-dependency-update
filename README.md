# dependencies-updater

* Things to add:
  * Add scheduled tasks
    * Task 1, check mongo for updated list of gradle plugins (once a week?)
    * Task 2, check mongo and maven for any updates required in mongo (once a week?)

* Example:
  * java -jar app-dependency-update.jar repo_home=/home/pi/zava/projects -Dmongo_user=something_user -Dmongo_pwd=something_password
