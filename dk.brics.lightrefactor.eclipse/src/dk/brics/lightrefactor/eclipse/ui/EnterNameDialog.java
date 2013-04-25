package dk.brics.lightrefactor.eclipse.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import dk.brics.lightrefactor.eclipse.RefactoringPlugin;
import dk.brics.lightrefactor.eclipse.RefactoringScope;

/**
 * The first dialog window opened during the rename refactoring.
 */
public class EnterNameDialog extends TitleAreaDialog {
  
  private Text textbox;
  private String oldName;
  private String thingType;
  private Map<RefactoringScope, Button> scopeBtns = new HashMap<RefactoringScope, Button>();
  
  private boolean showScope = true;

  private String newName;
  private RefactoringScope selectedScope;
  private boolean previewSelected;
  
  private Button previewBtn;
  
  private Label notifySave;
  
  private ProgressIndicator progressIndicator;
  
  public EnterNameDialog(Shell parentShell, String thingType, String oldName) {
    super(parentShell);
    this.oldName = oldName;
    this.thingType = thingType;
  }
  
  public void setShowScope(boolean showScope) {
    this.showScope = showScope;
  }
  
  @Override
  public void create() {
    super.create();
    setTitle("Rename " + thingType);
    setMessage("Enter a name to replace " + oldName);
  }
  
  private IDialogSettings getSettings() {
    return RefactoringPlugin.getDefault().getSettings("EnterNameDialog");
  }
  
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite panel = (Composite) super.createDialogArea(parent);
    
    // load relevant settings
    IDialogSettings settings = getSettings();
    RefactoringScope defaultScope = RefactoringScope.fromString(settings.get("scope"));
    if (defaultScope == null)
      defaultScope = RefactoringScope.File;
    boolean isPreview = settings.getBoolean("preview");
    
    panel.setLayout(new GridLayout());
    
    textbox = new Text(panel, SWT.BORDER);
    textbox.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    textbox.setText(oldName);
    textbox.selectAll();
    
    Group scopeGroup = new Group(parent, SWT.NONE);
    scopeGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    scopeGroup.setLayout(new GridLayout());
    scopeGroup.setText("Apply to");
    scopeBtns.put(RefactoringScope.File, mkScopeButton(scopeGroup, "File"));
    scopeBtns.put(RefactoringScope.Project, mkScopeButton(scopeGroup, "Project"));
    scopeBtns.put(RefactoringScope.Workspace, mkScopeButton(scopeGroup, "Workspace"));
    
    scopeBtns.get(defaultScope).setSelection(true);
    
    scopeGroup.setVisible(showScope);
    
    previewBtn = new Button(panel, SWT.CHECK);
    previewBtn.setText("Show preview");
    previewBtn.setSelection(isPreview);
    
    notifySave = new Label(scopeGroup, SWT.NONE);
    notifySave.setText("Files will be saved automatically");
    Color warnColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
    notifySave.setForeground(warnColor);
    notifySave.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    
    notifySave.setVisible(defaultScope != RefactoringScope.File);
    
    SelectionListener notifySaveListener = new SelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifySave.setVisible(!scopeBtns.get(RefactoringScope.File).getSelection());
      }
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        notifySave.setVisible(!scopeBtns.get(RefactoringScope.File).getSelection());
      }
    };
    for (Button btn : scopeBtns.values()) {
      btn.addSelectionListener(notifySaveListener);
    }
    
    return panel;
  }
  
  @Override
  protected Control createHelpControl(Composite parent) {
    return parent; // no help control
  }
  
  @Override
  protected Control createButtonBar(Composite parent) {
    Control btnBar = super.createButtonBar(parent);
    if (btnBar instanceof Composite) {
      parent = (Composite) btnBar;
    }
    // insert progress indicator below button bar
//    progressIndicator = new ProgressIndicator(parent);
//    progressIndicator.setVisible(true);
//    GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
//    progressIndicator.setLayoutData(gd);
//    progressIndicator.showNormal();
//    progressIndicator.beginTask(100);
//    progressIndicator.worked(25);
    return btnBar;
  }
  
  private Button mkScopeButton(Composite parent, String text) {
    Button btn = new Button(parent, SWT.RADIO);
    btn.setText(text);
    btn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    return btn;
  }
  
  @Override
  protected void okPressed() {
    this.newName = textbox.getText();
    this.selectedScope = RefactoringScope.File;
    for (Map.Entry<RefactoringScope,Button> en : scopeBtns.entrySet()) {
      if (en.getValue().getSelection()) {
        selectedScope = en.getKey();
      }
    }
    this.previewSelected = previewBtn.getSelection();
    
    // store settings
    IDialogSettings settings = getSettings();
    if (showScope) {
      settings.put("scope", selectedScope.toString());
    }
    settings.put("preview", previewSelected);
    
    super.okPressed();
  }
  
  public String getNewName() {
    return newName;
  }
  public RefactoringScope getSelectedScope() {
    return selectedScope;
  }
  public boolean isPreviewSelected() {
    return previewSelected;
  }
  
}
