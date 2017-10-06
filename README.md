See our site for better context of this readme. [Click here](http://atmosphereframework.com/)

# atmosphere-ui-viewer
The UI viewer of the ATMOSPHERE mobile testing framework. Helps to debug UI elements.

## Build the project
You can build the project using the included Gradle wrapper by running:
* `./gradlew build` on Linux/macOS
* `gradlew build` on Windows

## Making changes
If you make changes to this project and would like to use your new version in another ATMOSPHERE framework project that depends on this one, after a successful build also run:
* `./gradlew publishToMavenLocal` (Linux/macOS)
* `gradlew publishToMavenLocal` (Windows)

to publish the jar to your local Maven repository. The ATMOSPHERE framework projects are configured to use the artifact published in the local Maven repository first.