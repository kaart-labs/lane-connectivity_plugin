// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity.gui;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DialogExample {
    private static Dialog d;

    DialogExample() {
        Frame f = new Frame();
        d = new Dialog(f, "Dialog Example", true);
        d.setLayout(new FlowLayout());
        Button b = new Button("OK");
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DialogExample.d.setVisible(false);
            }
        });
        d.add(new Label("Click button to continue."));
        d.add(b);
        d.setSize(300, 300);
        d.setVisible(true);
    }

    public static void yuh(String args[]) {
        new DialogExample();
    }
}
