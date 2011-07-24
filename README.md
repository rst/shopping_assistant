Sample app for the Positronic Net library.

The build procedure, using sbt is something like the following:

First, install sbt 0.7 per [instructions](http://code.google.com/p/simple-build-tool/wiki/Setup).  Note that sbt 0.10 will not (yet) work, pending updates to the sbt-android plugin.

Then, get a copy of [Positronic Net itself](https://github.com/rst/positronic_net), and publish to your local ivy repo:

    $ cd [your workspace]
    $ git clone https://github.com/rst/positronic_net.git
    $ cd positronic_net
    $ sbt update
    $ sbt "project PositronicNetLib" publish-local

(This does just publish a jar file, containing the classes, and nothing but
the classes --- no resources.  Fortunately, the library doesn't declare any
resources, at least not yet, so this actually works.)

Lastly compile and build this app:

    $ cd [your workspace]
    $ git clone https://github.com/rst/shopping_assistant.git
    $ cd shopping_assistant
    $ sbt update
    $ sbt package-debug

If that all worked, you should wind up with an apk in `.../shopping_assistant/target/scala_2.8.1/shoppinglists_2.8.1-0.1.apk`
