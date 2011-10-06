Sample app for the Positronic Net library.

The build procedure, using sbt is something like the following:

First, install the Android SDK, sbt, and the Positronic Net library
itself following instructions [here](http://rst.github.com/tut_sections/2001/01/01/installation.html).

You'll also need to get your maps API key into the resources, perhaps by putting
a file named `apiKey.xml` into `src/main/res/values`, with contents like so:

    <resources>
        <string name="mapsKey">0xdeadbeef_your_key_goes_here_443543</string>
    </resources>

except replacing the content of the `mapsKey` tag with your actual key.
(Which requires you to have a maps API key; see their docs for details.)

Lastly compile and build this app:

    $ cd [your workspace]
    $ git clone https://github.com/rst/shopping_assistant.git
    $ cd shopping_assistant
    $ sbt android:package-debug

If that all worked, you should wind up with an apk in `.../shopping_assistant/target/scala_2.8.1/shoppinglists_2.8.1-0.1.apk`
