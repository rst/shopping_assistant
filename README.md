Sample app for the Positronic Net library.

The build procedure, using sbt is something like the following:

First, install sbt 0.10.0 per [instructions](https://github.com/harrah/xsbt/wiki/Setup).  

Next, install a current version of the [sbt-android-plugin](https://github.com/jberkel/android-plugin).  (Building and installing a snapshot may be required.) 

Then, get a copy of [Positronic Net itself](https://github.com/rst/positronic_net), and publish to your local ivy repo:

    $ cd [your workspace]
    $ git clone https://github.com/rst/positronic_net.git
    $ cd positronic_net
    $ git checkout -b sbt10 origin/sbt10
    $ sbt "project PositronicNetLib" publish-local

(This does just publish a jar file, containing the classes, and nothing but
the classes --- no resources.  Fortunately, the library doesn't declare any
resources, at least not yet, so this actually works.)

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
