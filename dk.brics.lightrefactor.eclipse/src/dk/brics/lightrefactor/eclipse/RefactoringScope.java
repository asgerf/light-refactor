package dk.brics.lightrefactor.eclipse;

public enum RefactoringScope {
  File,
  Project,
  Workspace;
  
  public static RefactoringScope fromString(String x) {
    if (x == null)
      return null;
    return RefactoringScope.valueOf(x);
  }
}
