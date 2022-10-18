# SpotBugs

> [SpotBugs](https://spotbugs.github.io/) is a static analysis tool that looks for potential bugs in Java code. It can be used in both pure Java, and Android Java projects. 

> In this project, SpotBugs is used to analyse several Android Java projects at once.

## Prerequisites

1. Java 8 installed (it should work on higher versions too)
2. Android Studio installed, with Gradle
3. have Android projects you want to test (all projects need to be created as Android Studio projects and built at least once)
4. Android projects need to have path to your local Android SDK and use Gradle 5+ because SpotBugs doesn't support lower versions

## Running SpotBugs

1. Clone this repository (or fork and clone)
2. Add paths to all the projects you want to test in paths.txt
3. paths.txt and script need to be in the same directory
4. Open Terminal in that directory and run commands:
    1. javac SpotBugs.java
    2. java SpotBugs
5. Wait to see if BUILDs are SUCCESSFUL (for all of your projects)
6. HTML reports will be in every project in project -> app -> build -> SpotBugsReports!!
7. Script creates and opens (when it's finished) FinalReport.html where you can see total amount of warnings for every project and also access all the individual project reports 
8. If you prefer report to open in Microsoft Excel, script also creates FinalReport.csv file that you can access through your current directory, or you can download it from html report that was mentioned in statement 7.

> You can run it as many times as you want to, no need to delete code by hand because it is not going to insert the same code several times. It should be fine every time you try.

## Contributing to SpotBugs

1. Clone this repository (or fork and clone)
2. Create a new branch: `git checkout -b <branch_name>`.
3. Make your changes and commit them: `git commit -m '<commit_message>'`
4. Push to your branch: `git push origin <branch_name>`
5. Create a pull request.

Alternatively see the GitHub documentation on [creating a pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request).
