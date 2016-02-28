[Popcorn Time for Android](https://github.com/popcorn-official/popcorn-android)
----

Allow any Android user to watch movies easily streaming from torrents, without any particular knowledge.

Visit the project's website at <https://popcorntime.sh/>.

* [Issue Tracker](https://github.com/popcorn-official/popcorn-desktop/issues)

## Community

Keep track of Popcorn Time development and community activity.

* Follow Popcorn Time on [Twitter](https://twitter.com/popcorntimetv), [Facebook](https://www.facebook.com/getpopcorntime.org/) and [Google+](https://plus.google.com/communities/111003619134556931561).
* Read and subscribe to [The Official Popcorn Time Blog](https://blog.popcorntime.sh/).
* Join in discussions on the [Popcorn Time Forum](https://discuss.popcorntime.sh/).
* Check out what the community is up to on [The Official Popcorn Time Subreddit](https://www.reddit.com/r/PopCornTime/).
* Connect with us on IRC at `#Popcorn Time` on freenode ([web access](http://webchat.freenode.net/?channels=popcorntime))

## Getting Involved

Want to report a bug, request a feature, contribute or translate Popcorn Time? Check out our in-depth guide to [Contributing to Butter](CONTRIBUTING.md#contributing-to-butter).

## Build Instructions ##

The [gradle build system](http://tools.android.com/tech-docs/new-build-system/user-guide) will fetch all dependencies and generate
files you need to build the project. You first need to generate the
local.properties (replace YOUR_SDK_DIR by your actual android sdk dir)
file:

    $ echo "sdk.dir=YOUR_SDK_DIR" > local.properties

You can now sync, build and install the project:

    $ ./gradlew assembleDebug # assemble the debug .apk
    $ ./gradlew installDebug  # install the debug .apk if you have an
                              # emulator or an Android device connected

You can use [Android Studio](http://developer.android.com/sdk/installing/studio.html) by importing the project as a Gradle project.

## Directory structure ##

    `|-- base                            # base module (contains providers and streamer)
     |    |-- build.gradle               # base build script
     |    `-- src
     |          |-- main
     |                |-- assets         # base module assets
     |                |-- java           # base module java code
     |                `-- res            # base module resources
    `|-- mobile                          # mobile module (smartphone/tablet application)
     |    |-- build.gradle               # mobile build script
     |    `-- src
     |          |-- main
     |                |-- java           # mobile module java code
     |                `-- res            # mobile module resources
    `|-- tv                              # tv module (Android TV application)
     |    |-- build.gradle               # tv build script
     |    `-- src
     |          |-- main
     |                |-- java           # tv module java code
     |                `-- res            # tv module resources
    `|-- vlc                             # vlc module (VLC mediaplayer library)
     |    |-- build.gradle               # vlc module build script
     |    `-- src
     |          |-- main
     |                |-- jniLibs        # native LibVLC libraries
     |                |-- java           # LibVLC Java code
    `|-- connectsdk                      # connectsdk module
          |-- build.gradle               # connectsdk build script
          `-- src
          |     |-- java                 # connectsdk module java code
          `-- core
          |     |-- src                  # connectsdk module core java code
          `-- modules
                |-- google_cast
                      |-- src            # connectsdk module google cast java code
                |-- firetv
                      |-- src            # connectsdk module google cast java code

## Versioning

For transparency and insight into our release cycle, and for striving to maintain backward compatibility, Popcorn Time will be maintained according to the [Semantic Versioning](http://semver.org/) guidelines as much as possible.

###Beta versions

Beta releases will be numbered with the following format:

`0.<major>.<minor>-<patch>`

###Stable versions

Releases will be numbered with the following format:

`<major>.<minor>.<patch>`


Constructed with the following guidelines:
* A new *major* release indicates a large change where backwards compatibility is broken.
* A new *minor* release indicates a normal change that maintains backwards compatibility.
* A new *patch* release indicates a bugfix or small change which does not affect compatibility.

## License

If you distribute a copy or make a fork of the project, you have to credit this project as source.

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.

Note: some dependencies are external libraries, which might be covered by a different license compatible with the GPLv3. They are mentioned in NOTICE.md.

***

Copyright (c) 2016 Popcorn Time - Released under the [GPL v3 license](LICENSE.txt).development/LICENSE.md).
