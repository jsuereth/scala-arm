---
layout: default
title: ARM with Continuations
---
Scala-arm can make use of delimited continuations to simplify code.  A delimited continuation is a way of rewriting code to simplify continuation passing style.   The essence of delmited continuations is contained Scala's `ControlContext` class.

Essentially, a ControlContext is a way of storing computation that has occured and computation that *will* occur later.  Let's look at an example:

![Drawing 1](https://docs.google.com/drawings/pub?id=1sEKDu_8Xku1W6ulF_kN5OAxrjMrUZIZg3yMVYh9quPY&w=960&h=720)

In this example, the code `val input = managed(new FileInputStream('test.txt")) !` makes a call to `scala.util.continuations.shift`.  This places a portion of computation into a `ControlContext`.  The beginning of this computation opens the file `input.txt` and the end of the computation closes the file `input.txt`.   The middle portion is left empty to be filled in later via a continuations.

The result type of the `now` operator on continuations is @suspendable which is shorthand for @cpsParam[Unit,Unit].   This captures the type of each portion of computation in the `ControlContext`.   The type `InputStream @cpsParam[Unit,Unit]` denotes that the computation so far generates an `InputStream`, requires a continuation that takes the `InputStream` and returns a Unit and will eventually return a `Unit` when completed.

`ControlContext` is nestable.   When constructing a `ControlContext` inside of another one, the ending computations must line up.   Let's look at an example with nested managed resources.

![Figure 2](https://docs.google.com/drawings/pub?id=1t9s4ZeGcuIJraGgiCmdj8T61SCQBwoo_dY2eODfaLRQ&w=960&h=720)

In this example, the `val output = managed(new FileOutputStream('test2.txt")) !` expression is nested inside the outer delimited continuation expression.   This causes the opening of `test2.txt` to happen after the opening of `test.txt` and the closing of `test2.txt` to happen before the closing of `test.txt`.

The expression also creates a new nested `ControlContext` with type `FileOutputStream @cpsParam[Unit,Unit]`.   The last parameter of the computation *must* line up with the second parameter of the outer `ControlContext` so that the computations can be nested.   There is still a 'hole' left in the middle of the entire computation for the remaining block.

## Socket Example ##

The below code implements an echo server that listens on a port and echos back every full line of text it receives. 

{% highlight scala %}
    import java.io._
    import util.continuations._
    import resource._
    def each_line_from(r : BufferedReader) : String @suspendable =
      shift { k =>
        var line = r.readLine
        while(line != null) {
          k(line)
          line = r.readLine
        }
      }
    reset {
      val server = managed(new ServerSocket(8007)) !
      while(true) {
        // This reset is not needed, however the  below denotes a "flow" of execution that can be deferred.
        // One can envision an asynchronous execuction model that would support the exact same semantics as     below.
        reset {
          val connection = managed(server.accept).now
          val output = managed(connection.getOutputStream).now
          val input = managed(connection.getInputStream).now
          val writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)))
          val reader = new BufferedReader(new InputStreamReader(input))
          writer.println(each_line_from(reader))
          writer.flush()
        }
      }
    }
{% endhighlight %}

The final computation placed in the hole left over from all the nested `ControlContext`s is the simple:

{% highlight scala %}
    writer.println(_)
    writer.flush()
{% endhighlight %}

You can see how each call to `shift` (either the `now` operator or the `each_line_from` method) causes additional computation before and after the 'hole'.  You can also see in `each_line_from` how that 'hole' in the computation can be used more than once to complete the entire process.

Delimited continuations provide a lot of power.   This simple model of thinking about them helps understand the type signatures and how to use them effectively.
