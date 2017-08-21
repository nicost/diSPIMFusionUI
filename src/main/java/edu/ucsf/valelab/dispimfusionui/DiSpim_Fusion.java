
package edu.ucsf.valelab.dispimfusionui;

import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;


import net.miginfocom.swing.MigLayout;



/**
 *
 * @author nico
 */

public class DiSpim_Fusion implements PlugIn {
   private final JTextField spimADirectory_;
   private final JTextField spimBDirectory_;
   
   public DiSpim_Fusion () {
      
      spimADirectory_ = new JTextField();
      spimBDirectory_ = new JTextField();
      
   }
           

   @Override
   public void run(String string) {
      // create the dialog
      
      int xPos = 100;
      int yPos = 100;
      int width = 400;
      int height = 300;
      JFrame frame = new JFrame ();
      frame.setBounds(xPos, yPos, width, height);
      frame.setLayout( new MigLayout());
      
      frame.add(new Label("SPIMA Directory"));
      frame.add(spimADirectory_);
      frame.add(chooserButton("SPIM A Directory", spimADirectory_, true));
      
      
              
   }
   
   public JButton chooserButton(final String msg, final JTextField pathField, final boolean dir) {
      JButton button = new JButton("...");
      button.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            if (dir) {
               DirectoryChooser dc = new DirectoryChooser(msg);
               pathField.setText(dc.getDirectory());
            }
            else {
               OpenDialog od = new OpenDialog(msg);
               pathField.setText(od.getPath());
            }
         }
      });
      
      return button;
   }

 
}
