/**
 * class ToggleMethodBreakpointAction
 * @author Jeka
 */
package com.intellij.debugger.actions;

import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.debugger.ui.breakpoints.MethodBreakpoint;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.util.text.CharArrayUtil;

public class ToggleMethodBreakpointAction extends AnAction {
  public void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Project project = (Project)dataContext.getData(DataConstants.PROJECT);
    if (project == null) return;
    DebuggerManagerEx debugManager = DebuggerManagerEx.getInstanceEx(project);
    if (debugManager == null) return;
    BreakpointManager manager = debugManager.getBreakpointManager();

    PlaceInDocument place = getPlace(e);

    if(place != null) {
      Breakpoint breakpoint = manager.findMethodBreakpoint(place.getDocument(), place.getOffset());

      if(breakpoint == null) {
        int methodLine = place.getDocument().getLineNumber(place.getOffset());
        MethodBreakpoint methodBreakpoint = manager.addMethodBreakpoint(place.getDocument(), methodLine);
        if(methodBreakpoint != null) {
          RequestManagerImpl.createRequests(methodBreakpoint);
        }
      } else {
        manager.removeBreakpoint(breakpoint);
      }
    }
  }


  private static PlaceInDocument getPlace(AnActionEvent event) {
    DataContext dataContext = event.getDataContext();
    Project project = (Project)dataContext.getData(DataConstants.PROJECT);

    if(project == null) return null;

    PsiElement method = null;
    Document document = null;

    if (ActionPlaces.PROJECT_VIEW_POPUP.equals(event.getPlace()) ||
        ActionPlaces.STRUCTURE_VIEW_POPUP.equals(event.getPlace()) ||
        ActionPlaces.FAVORITES_VIEW_POPUP.equals(event.getPlace())) {
      final PsiElement psiElement = (PsiElement)dataContext.getData(DataConstants.PSI_ELEMENT);
      if(psiElement instanceof PsiMethod) {
        method = psiElement;
        document = PsiDocumentManager.getInstance(project).getDocument(method.getContainingFile());
      }
    } else {
      Editor editor = (Editor)dataContext.getData(DataConstants.EDITOR);
      if(editor == null) {
        editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
      }
      if (editor != null) {
        document = editor.getDocument();
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (file != null) {
          FileTypeManager fileTypeManager = FileTypeManager.getInstance();
          FileType fileType = fileTypeManager.getFileTypeByFile(file.getVirtualFile());
          if (StdFileTypes.JAVA == fileType || StdFileTypes.CLASS  == fileType) {
            method = findMethod(project, editor);
          }
        }
      }
    }

    if(method != null) {
      final PsiElement method1 = method;
      final Document document1 = document;

      return new PlaceInDocument() {
        public Document getDocument() {
          return document1;
        }

        public int getOffset() {
          return method1.getTextOffset();
        }
      };
    }
    return null;
  }

  private static PsiMethod findMethod(Project project, Editor editor) {
    if (editor == null) return null;
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if(psiFile == null) return null;
    final int offset = CharArrayUtil.shiftForward(editor.getDocument().getCharsSequence(), editor.getCaretModel().getOffset(), " \t");
    return DebuggerUtilsEx.findPsiMethod(psiFile, offset);
  }

  public void update(AnActionEvent event){
    boolean toEnable = getPlace(event) != null;

    if (ActionPlaces.EDITOR_POPUP.equals(event.getPlace()) ||
        ActionPlaces.PROJECT_VIEW_POPUP.equals(event.getPlace()) ||
        ActionPlaces.STRUCTURE_VIEW_POPUP.equals(event.getPlace()) ||
        ActionPlaces.FAVORITES_VIEW_POPUP.equals(event.getPlace())) {
      event.getPresentation().setVisible(toEnable);
    }
    else {
      event.getPresentation().setEnabled(toEnable);
    }
  }
}