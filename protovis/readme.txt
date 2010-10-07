PROTOVIS JAVA README

This project contains a research prototype implementation of the Protovis
visualization language in Java. The package contains the following structure:

src -- Protovis source files.
doc -- Documentation files.
lib -- Required 3rd party libraries.

For OpenGL rendering, Protovis uses version 1.1 of the JOGL (Java Bindings for
OpenGL) library. The files provided in the "lib" directory are currently
configured for use on computers running Mac OS X. Windows and Linux users will
need to introduce platform specific versions of compiled JOGL files.

For JOGL downloads for non-Mac platforms, please see:
https://jogl.dev.java.net/servlets/ProjectDocumentList?folderID=9260&expandFolder=9260&folderID=0

Applications which use OpenGL rendering must include the following:
- jogl.jar and gluegen-rt.jar must be on the classpath
- Compiled JOGL JNI libraries must be accessible to Java. For example, one can
  use the "-Djava.library.path=lib" directive to point the JVM to the directory
  containing the necessary files. (Replace "lib" with the path to the correct 
  directory.)

By default, Protovis is configured to use single-threaded execution of
dynamically compiled anonymous property definitions and single-threaded
rendering. If any of this sounds confusing, be sure to read this paper first:
http://vis.stanford.edu/papers/protovis-design

For examples of using Protovis, see the protovis-examples project.