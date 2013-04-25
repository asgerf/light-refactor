package dk.brics.lightrefactor.eclipse.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Utility class for using LTK's {@link Change} objects and preview screen.
 * 
 * @author asger
 */
public class RefactoringUtil {
  
  /** Refactoring wizard without any pages */
  public static class PreviewOnlyWizard extends RefactoringWizard {
    public PreviewOnlyWizard(Refactoring refactoring) {
      super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
    }
    @Override
    protected void addUserInputPages() {
      // add no pages
    }
  }
  
  /** Refactoring that performs a precomputed set of changes */
  public static class SimpleRefactoring extends Refactoring {
    private Change change;
    private String name;
    /** Creates a refactoring that will perform the given set of changes */
    public SimpleRefactoring(Change change, String name) {
      this.change = change;
      this.name = name;
    }
    @Override
    public String getName() {
      return name;
    }
    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
      return new RefactoringStatus();
    }
    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
      return new RefactoringStatus();
    }
    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
      return change;
    }
  }
  
  /**
   * Shows the preview screen for the given refactoring. Blocks until preview is closed.
   * If accepted, the refactoring will also be performed.
   * @param refactoring the refactoring to preview (and possibly perform)
   * @param parent the window to become parent of the preview dialog
   * @param dialogTitle title of the preview window
   * @return true if accepted, false if cancelled.
   */
  public static boolean previewAndPerform(Refactoring refactoring, Shell parent, String dialogTitle) {
    try {
      return IDialogConstants.OK_ID == new RefactoringWizardOpenOperation(new PreviewOnlyWizard(refactoring)).run(parent, dialogTitle);
    } catch (InterruptedException ex) {
      return false;
    }
  }

  /**
   * Shows the preview screen for the given refactoring. Blocks until preview is closed.
   * If accepted, the refactoring will also be performed.
   * @param changes changes to perform
   * @param refactoringName human-readable name of the refactoring; maybe used for undo purposes (who knows?)
   * @param parent the window to become parent of the preview dialog
   * @param dialogTitle title of the preview window
   * @return true if accepted, false if cancelled.
   */
  public static boolean previewAndPerform(Change changes, String refactoringName, Shell parent, String dialogTitle) {
    return previewAndPerform(new SimpleRefactoring(changes, refactoringName), parent, dialogTitle);
  }
  
  /**
   * Performs the given set of changes, and then calls {@link Change#dispose dispose} on the change object.
   * @param change the changes to perform
   * @param window window whose UI should block while performing the operation
   * @param multiFileUndo if true, the undo will be regarded as a multi-file undo operation, otherwise a single-file operation
   */
  public static void perform(final Change change, IWorkbenchWindow window, final boolean multiFileUndo) {
    try {
      window.run(true, true, new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          try {
            change.initializeValidationData(monitor);
            PerformChangeOperation op = new PerformChangeOperation(change);
            if (multiFileUndo) {
              op.setUndoManager(RefactoringCore.getUndoManager(), change.getName());
            }
            ResourcesPlugin.getWorkspace().run(op, monitor);
          } catch (CoreException e) {
            throw new RuntimeException(e);
          }
        }
      });
    } catch (InvocationTargetException e1) {
      throw new RuntimeException(e1);
    } catch (InterruptedException e1) {
      throw new RuntimeException(e1);
    }
    /*IProgressMonitor pm = new NullProgressMonitor();
    try {
      change.initializeValidationData(pm);
      if (!change.isEnabled())
          return;
      RefactoringStatus valid= change.isValid(pm);
      if (valid.hasFatalError())
          return;
      change.perform(pm);
    } catch (OperationCanceledException e) {
      throw new RuntimeException(e);
    } catch (CoreException e) {
      throw new RuntimeException(e);
    } finally {
      change.dispose();
    }*/
  }
  
}
