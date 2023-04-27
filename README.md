# app-dependency-update

* Things to add:
  * Scheduler to check and try to update snapshots 
    * take input date for branch name, optional if not provided LocalDate.now()
  * Tests
  * Documentation

* System Requirements:
  * Java 17
  * NPM
  * git
  * github cli
* Example:
    * java -jar -Drepo_home=/home/pi/zava/projects -Dmongo_user=something_user -Dmongo_pwd=something_password app-dependency-update.jar
