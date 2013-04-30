lightrefactor
=============

Refactoring logic for JavaScript, based on lightweight type inference. Currently **rename identifier** is the only supported refactoring, but this refactoring can rename property names in addition to local and global variables.

Uses the fast JavaScript parser from Rhino.

**Future work**
* Support for `with` statement. In theory it is simple to support, but it complicates the interactive part of the refactoring, so I decided against supporting it.
* Future JavaScript features. JavaScript is evolving, and sooner or later we need to support the new features.
