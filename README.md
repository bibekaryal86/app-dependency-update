# app-dependency-update

* Things to add:
  * Tests
  * Documentation

* System Requirements:
  * Java 21 (https://adoptium.net/temurin/releases/)
  * NPM (https://nodejs.org/en/download)
  * npm-check-updates (https://www.npmjs.com/package/npm-check-updates)
  * git (https://git-scm.com/downloads)
  * github cli (https://cli.github.com/)

* App Arguments
  * Required
    * repo_home: Hard disk location where repos are cloned
    * mongo_user: User name of mongo database where plugins and dependencies are stored
    * mongo_pwd: Password of the mongo database
  * Optional
    * send_email: Flag to send email of current log file at the end of scheduled update
    * mj_public: Public API Key of MailJet Email Service
      * Required if send_email is `true`
    * mj_private: Private API Key of MailJet Email Service
      * Required if send_email is `true`
    * mj_email: Email address that the email will be sent from
      * Required if send_email is `true`
  
* Example:
    * java -jar -Drepo_home=/home/pi/zava/projects -Dmongo_user=something_user -Dmongo_pwd=something_password app-dependency-update.jar
