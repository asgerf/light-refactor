lightrefactor.eclipse
=====================

Eclipse plugin for refactoring JavaScript code. Integrates with the JSDT JavaScript editor and WST HTML editor.

Only ''rename identifier'' refactoring is supported at the moment.

'''Eclipse features''':
* Refactor code in multiple files
* Refactor code in HTML files
* Undo refactoring
* Preview refactoring

'''Room for improvement''':
* Better UI: Eclipse's refactoring framework (LTK) was not designed for semi-automatic refactoring. The interactive part of the refactoring uses dialog windows, which are probably not be most user-friendly way to do it.
* Also see future work in `lightrefactor` project.
