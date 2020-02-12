package textTheSpire;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.lwjgl.Sys;

public class Window {

    Shell shell;
    Label label;

    public Window(Display d, String header, int w, int h){
        shell = new Shell(d);
        shell.setSize(w,h);
        shell.setLocation(200,400);
        shell.setText(header);
        label = new Label(shell, SWT.NONE);
        shell.setVisible(true);
        shell.open();

    }

    public void setText(String s){
        label.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if(!label.isDisposed() && !s.equals(label.getText())) {
                    label.setText(s);
                    System.out.println("Set Text: " + s);
                }
            }
        });

    }

    /*public void visible(){
        shell.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if(!shell.isDisposed()){
                    shell.setVisible(true);
                }
            }
        });
    }
    public void invisible(){
        shell.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if(!shell.isDisposed()){
                    shell.setVisible(false);
                }
            }
        });
    }*/

}