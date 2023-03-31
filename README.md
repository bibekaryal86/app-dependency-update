# dependencies-updater

* Things to update:
    * Currently directly `app` module is loaded for build.gradle, instead do this:
      * check settings.gradle for modules and update build.gradle from there
      * include('app') or include 'app' both works

* Example:
    * java -jar -Drepo_home=/home/pi/zava/projects -Dmongo_user=something_user -Dmongo_pwd=something_password
      app-dependency-update.jar
