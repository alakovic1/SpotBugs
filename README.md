# SpotBugs

> [SpotBugs](https://spotbugs.github.io/) is a static analysis tool that looks for potential bugs in Java code. It can be used in both pure Java, and Android Java projects.
> In this project, SpotBugs is used to analyse Android Java projects at once.

## Prerequisites

1. Java 8 installed (not sure how it works on higher Java versions)
2. Android Studio installed, or Gradle (must be version 5+)
3. have projects you want to test

## Running SpotBugs

1. git clone <url>
2. delete project "NOVAapp" if you don't want to test it
3. move all your projects you want to test in this folder, or move .java script to directory your projects are in
4. make sure no other directories are there
5. open Terminal in this/that directory and run commands:
    1. javac SpotBugsSkripta.java
    2. java SpotBugsSkripta
6. wait to see if BUILDs are SUCCESSFUL (for all of your projects)
7. html reports will be in every project in project -> app -> build -> SpotBugsReports!!

## Contributing to SpotBugs

To contribute to Payment server, follow these steps:

1. Clone this repository (or fork and clone)
2. Create a new branch: `git checkout -b <branch_name>`.
3. Make your changes and commit them: `git commit -m '<commit_message>'`
4. Push to your branch: `git push origin <branch_name>`
5. Create a pull request.

Alternatively see the GitHub documentation on [creating a pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request).
