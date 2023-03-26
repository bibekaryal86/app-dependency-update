# dependencies-updater

* Things to update:
  * Add scheduled tasks
    * (DONE) Task 1, check mongo for updated list of gradle plugins (once a week?)
    * Task 2, check mongo and maven for any updates required in mongo (once a week?)
  * Update log configurations
    * only warn/error on stdout
      * update ThreadMonitor.java log level
    * everything on file log
  * Rename `execute` folder to `helpers`

* Example:
  * java -jar -Drepo_home=/home/pi/zava/projects -Dmongo_user=something_user -Dmongo_pwd=something_password app-dependency-update.jar
