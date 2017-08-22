
package edu.ucsf.valelab.dispimfusionui;

import ij.Macro;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;


import net.miginfocom.swing.MigLayout;



/**
 *
 * @author nico
 */

public class DiSpimFusion implements PlugIn {
   // inputs are grouped by type.
   // they are identified by a key of type String
   // They keys double as label in the UI
   
   // used as String inputs (TextFields)
   public static final String SPIMADIRECTORY = "Spim A Directory";
   public static final String SPIMBDIRECTORY = "Spim B Directory";
   public static final String IMAGEANAME = "Image A Name";
   public static final String IMAGEBNAME = "Image B Name";
   public static final String OUTPUTDIRECTORY = "Output Directory";
   // used as int inputs
   public static final String START = "Start #";
   public static final String END = "End #";
   public static final String INTERVAL = "Interval";
   public static final String TEST = "Test#";
   // used as double inputs
   public static final String XPIXELSIZEA = "ImageA x";
   public static final String YPIXELSIZEA = "ImageA y";
   public static final String ZPIXELSIZEA = "ImageA z";
   public static final String XPIXELSIZEB = "ImageB x";
   public static final String YPIXELSIZEB = "ImageB y";
   public static final String ZPIXELSIZEB = "ImageB z";
   public static final String CONVERGENCETHRESHOLD = "Convergence Threshold";
   // ComboBoxes
   public static final String REGISTRATIONOPTIONS = "Registration Options";
   public static final String IMAGEBROTATION = "Image B Rotation"; 
   // CheckBoxes
   public static final String DO2DREGISTRATION = "Do 2D Registration";
   public static final String CUSTOMIZEMATRIX = "Customize initial transformation matrix";
   public static final String SAVEREGISTEREDIMAGES = "Save Registered Images";
   public static final String SHOWGPUINFO = "Show GPU Device Information";
   
   final private Preferences prefs_;

   private final JComboBox registrationOptions_;
   private final String[] regOptionsStr_ = {
         "All images dependently",
         "All images indenpendently",
         "One image only",
         "No registration" };
   private final JComboBox imageBRotation_;
   private final String[] imageBRotationsStr_ = {
         "No rotation",
         "90 deg (Y-axis)",
         "-90 deg (Y-axis)" };
   
   private boolean recorderOn_ = false;
   
   private final String[] textFields_ = {
         SPIMADIRECTORY, SPIMBDIRECTORY, IMAGEANAME,  IMAGEBNAME, 
         OUTPUTDIRECTORY };
   private final Map jTextFields_ = 
           new HashMap<String, JTextField>(textFields_.length);
   
   private final String[] intSpinnerLabels_ = {
         START, END, INTERVAL, TEST };
   private final Map intSpinners_ =
           new HashMap<String, JSpinner>(intSpinnerLabels_.length);
   
   private final String[] doubleSpinnerLabels_ = {
         XPIXELSIZEA, YPIXELSIZEA, ZPIXELSIZEA, 
         XPIXELSIZEB, YPIXELSIZEB, ZPIXELSIZEB,
         CONVERGENCETHRESHOLD };
   private final Map  doubleSpinners_ = 
           new HashMap<String, JSpinner>(doubleSpinnerLabels_.length);
   
   private final String[] checkBoxLabels_ = {
      DO2DREGISTRATION , CUSTOMIZEMATRIX, 
      SAVEREGISTEREDIMAGES, SHOWGPUINFO };
   private final Map checkBoxes_ = 
           new HashMap<String, JCheckBox>(checkBoxLabels_.length);
   
   public DiSpimFusion () {
      
      prefs_ = Preferences.userNodeForPackage(this.getClass());
      
      for (String jt : textFields_) {
         jTextFields_.put(jt, new JTextField());
      }
      
      for (String isp : intSpinnerLabels_) {
         intSpinners_.put(isp, new JSpinner(
                 new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1)) );
      }
      ((JSpinner) intSpinners_.get(END)).setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
      ((JSpinner) intSpinners_.get(INTERVAL)).setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
      
      for (String dsp : doubleSpinnerLabels_) {
         doubleSpinners_.put(dsp, new JSpinner(
                 new SpinnerNumberModel(0.165, 0.010, 10.0, 0.001)));
      }
      ((JSpinner) doubleSpinners_.get(CONVERGENCETHRESHOLD)).setModel(
              new SpinnerNumberModel(0.001, 0.0, 1.0, 0.00001));
      
      for (String cb : checkBoxLabels_) {
         checkBoxes_.put(cb, new JCheckBox(cb));
      }
      
      registrationOptions_ = new JComboBox(regOptionsStr_);
      imageBRotation_ = new JComboBox(imageBRotationsStr_);
      
      
   }
           

   @Override
   public void run(String string) {
      // create the dialog
      
      recorderOn_ = Recorder.record;
      
      String optionsString = Macro.getOptions();
      if (optionsString == null) {
         optionsString = "";
      }
      
      int xPos = 100;
      int yPos = 100;
      int width = 400;
      int height = 300;
      final JFrame frame = new JFrame ();
      frame.setTitle("diSPIM Fusion using CUDA");
      frame.setBounds(xPos, yPos, width, height);
      frame.setLayout( new MigLayout("",   // layout constraints
      "[right][][][]", // column constraints
      ""  // row constraints
      ));
      
      addDirectoryChoice(frame, optionsString, SPIMADIRECTORY);
      addDirectoryChoice(frame, optionsString, SPIMBDIRECTORY);
      
      frame.add(new Label(IMAGEANAME));
      JTextField tmp = (JTextField) jTextFields_.get(IMAGEANAME);
      tmp.setText(Macro.getValue(optionsString, IMAGEANAME.replaceAll("\\s",""), 
              prefs_.get(IMAGEANAME, "SPIMA_")));
      frame.add(tmp, "growx");
      frame.add(new Label(IMAGEBNAME), "gapleft push");
      JTextField tmp2 = (JTextField) jTextFields_.get(IMAGEBNAME);
      tmp2.setText(Macro.getValue(optionsString, IMAGEBNAME.replaceAll("\\s",""), 
              prefs_.get(IMAGEBNAME, "SPIMB_")));
      frame.add(tmp2, "growx, wrap");
      
      addDirectoryChoice(frame, optionsString, OUTPUTDIRECTORY);
      
      frame.add(new Label(START));
      ((JSpinner) intSpinners_.get(START)).setValue(Integer.parseInt( Macro.getValue(optionsString, START.replaceAll("\\s", ""), 
               Integer.toString(prefs_.getInt(START, 0))) ));
      frame.add(((JSpinner) intSpinners_.get(START)), "w 50:50:50");
      frame.add(new Label(INTERVAL), "right");
      ((JSpinner) intSpinners_.get(INTERVAL)).setValue(Integer.parseInt( Macro.getValue(optionsString, INTERVAL.replaceAll("\\s", ""), 
               Integer.toString(prefs_.getInt(INTERVAL, 1))) ));
      frame.add(((JSpinner) intSpinners_.get(INTERVAL)), "w 50:50:50, wrap");
      frame.add(new Label(END));
      ((JSpinner) intSpinners_.get(END)).setValue(Integer.parseInt( Macro.getValue(optionsString, END.replaceAll("\\s", ""), 
               Integer.toString(prefs_.getInt(END, 1))) ));
      frame.add(((JSpinner) intSpinners_.get(END)), "w 50:50:50");
      String testToolTip = "When registration option is set to \"One image only\", " +
                           "this image will be used. ";
      frame.add(new Label(TEST), "right");
      ((JSpinner) intSpinners_.get(TEST)).setValue(Integer.parseInt( Macro.getValue(optionsString, TEST.replaceAll("\\s", ""), 
               Integer.toString(prefs_.getInt(TEST, 0))) ));
      ((JSpinner) intSpinners_.get(TEST)).setToolTipText(testToolTip);
      frame.add(((JSpinner) intSpinners_.get(TEST)), "w 50:50:50, wrap 15px");
      
      frame.add(new Label("Initial Pixel Sizes (in microns)"), "span 4, align left, wrap");
      
      frame.add(new Label (XPIXELSIZEA));
      ((JSpinner) doubleSpinners_.get(XPIXELSIZEA)).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              XPIXELSIZEA.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(XPIXELSIZEA, 0.165)))));
      frame.add(((JSpinner) doubleSpinners_.get(XPIXELSIZEA)), "span 3, split 5, w 50");
      frame.add(new Label("y"), "gapleft push");
      ((JSpinner) doubleSpinners_.get(YPIXELSIZEA)).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              YPIXELSIZEA.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(YPIXELSIZEA, 0.165)))));
      frame.add(((JSpinner) doubleSpinners_.get(YPIXELSIZEA)));
      frame.add(new Label("z"), "gapleft push");
      ((JSpinner) doubleSpinners_.get(ZPIXELSIZEA)).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              ZPIXELSIZEA.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(ZPIXELSIZEA, 1.0)))));
      frame.add(((JSpinner) doubleSpinners_.get(ZPIXELSIZEA)), "wrap");
      
      frame.add(new Label (XPIXELSIZEB));
      ((JSpinner) doubleSpinners_.get(XPIXELSIZEB)).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              XPIXELSIZEB.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(XPIXELSIZEB, 0.165)))));
      frame.add(((JSpinner) doubleSpinners_.get(XPIXELSIZEB)), "span 3, split 5, w 50");
      frame.add(new Label("y"), "gapleft push");
      ((JSpinner) doubleSpinners_.get(YPIXELSIZEB)).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              YPIXELSIZEB.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(YPIXELSIZEB, 0.165)))));
      frame.add(((JSpinner) doubleSpinners_.get(YPIXELSIZEB)));
      frame.add(new Label("z"), "gapleft push");
      ((JSpinner) doubleSpinners_.get(ZPIXELSIZEB)).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              ZPIXELSIZEB.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(ZPIXELSIZEB, 1.0)))));
      frame.add(((JSpinner) doubleSpinners_.get(ZPIXELSIZEB)), "wrap 15px");
      
      frame.add(new Label("Registration Options"));
      registrationOptions_.setSelectedItem(Macro.getValue(optionsString,
              REGISTRATIONOPTIONS.replaceAll("\\s", ""),
              prefs_.get(REGISTRATIONOPTIONS, regOptionsStr_[0])));
      frame.add(registrationOptions_, "span 3, align left, wrap");
      
      frame.add(new Label("ImageB Rotation"));
      imageBRotation_.setSelectedItem(Macro.getValue(optionsString,
              IMAGEBROTATION.replaceAll("\\s", ""),
              prefs_.get(IMAGEBROTATION, imageBRotationsStr_[2])));
      frame.add(imageBRotation_, "span 3, alig left, wrap 15px");
      
      ((JCheckBox) checkBoxes_.get(DO2DREGISTRATION)).setSelected(
               Boolean.parseBoolean(
                  Macro.getValue(optionsString, DO2DREGISTRATION.replaceAll("\\s", ""),
                  String.valueOf(prefs_.getBoolean(DO2DREGISTRATION, false)))));
      frame.add(((JCheckBox) checkBoxes_.get(DO2DREGISTRATION)), "span 2, left");
       
      ((JCheckBox) checkBoxes_.get(SAVEREGISTEREDIMAGES)).setSelected(
               Boolean.parseBoolean(
                  Macro.getValue(optionsString, SAVEREGISTEREDIMAGES.replaceAll("\\s", ""),
                  String.valueOf(prefs_.getBoolean(SAVEREGISTEREDIMAGES, false)))));
      frame.add(((JCheckBox) checkBoxes_.get(SAVEREGISTEREDIMAGES)), "span 2, left, wrap");
        
      ((JCheckBox) checkBoxes_.get(CUSTOMIZEMATRIX)).setSelected(
               Boolean.parseBoolean(
                  Macro.getValue(optionsString, CUSTOMIZEMATRIX.replaceAll("\\s", ""),
                  String.valueOf(prefs_.getBoolean(CUSTOMIZEMATRIX, false)))));
      frame.add(((JCheckBox) checkBoxes_.get(CUSTOMIZEMATRIX)), "span3, left, wrap");

      frame.add(new Label(CONVERGENCETHRESHOLD));
     // ((JSpinner) doubleSpinners_.get(CONVERGENCETHRESHOLD)).setValue(Double.parseDouble(Macro.getValue(optionsString, 
     //         CONVERGENCETHRESHOLD.replaceAll("\\s", ""),
     //         Double.toString(prefs_.getDouble(CONVERGENCETHRESHOLD, 0.00001)))));
      frame.add(((JSpinner) doubleSpinners_.get(CONVERGENCETHRESHOLD)), "span 2, growx, wrap 15px");
      
      ((JCheckBox) checkBoxes_.get(SHOWGPUINFO)).setSelected(
               Boolean.parseBoolean(
                  Macro.getValue(optionsString, SHOWGPUINFO.replaceAll("\\s", ""),
                  String.valueOf(prefs_.getBoolean(SHOWGPUINFO, false)))));
      frame.add(((JCheckBox) checkBoxes_.get(SHOWGPUINFO)), "span3, left, wrap");
      
      JButton okButton = new JButton("OK");
      okButton.addActionListener(new ActionListener(){
         @Override
         public void actionPerformed(ActionEvent e) {
            storeSettings();
            frame.setVisible(false);
            // TODO: do the real work
         }
      });
      frame.add(okButton, "span 4, split 4, tag ok");
      
      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(new ActionListener(){
         @Override
         public void actionPerformed(ActionEvent e) {
            frame.setVisible(false);
         }
      });
      frame.add(cancelButton, "tag cancel, wrap");
      frame.pack();
      
      frame.setVisible(true);
              
   }
   
   private JButton chooserButton(final String msg, final JTextField pathField, final boolean dir) {
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
   
   private void addDirectoryChoice(JFrame frame, String optionsString, String identifier) {        
      frame.add(new Label(identifier));
      JTextField tmp = (JTextField)jTextFields_.get(identifier);
      frame.add(tmp, "span 3, split 2, width 250, growx");
      tmp.setText(Macro.getValue(optionsString, identifier.replaceAll("\\s",""), 
              prefs_.get(identifier, "")));
      frame.add(chooserButton(identifier, tmp, true), "wrap");   
   }
 
   /**
    * Write settings to usernode preferences, and the ImageJ macro recorder
    */
   private void storeSettings () {
      if (recorderOn_) {
         Recorder.setCommand("diSPIM Fusion");
      }
            
      for (final String tf : textFields_) {
         String value = ((JTextField) jTextFields_.get(tf)).getText();
         prefs_.put(tf, value);
         if (recorderOn_ && value != null && !value.equals("")) {
            String key = tf.replaceAll("\\s","");
            value = value.replace("\\", "\\\\");
            Recorder.recordOption(key, value);
         }
      }
      
      for (final String isp : intSpinnerLabels_) {
         Integer value = (Integer)
                 ((JSpinner) intSpinners_.get(isp)).getValue();
         prefs_.putInt(isp, value);
         if (recorderOn_  && value != null)
         {
            String key = isp.replaceAll("\\s", "");
            Recorder.recordOption(key, String.valueOf(value));
         }
      }
      
      for (final String dsp : doubleSpinnerLabels_) {
         Double value = (Double)
                 ((JSpinner) doubleSpinners_.get(dsp)).getValue();
         prefs_.putDouble(dsp, value);
         if (recorderOn_ && value != null && value != Double.NaN) {
            String key = dsp.replaceAll("\\s", "");
            Recorder.recordOption(key, String.valueOf(value));
         }
      }
      
      for (String cb : checkBoxLabels_) {
         Boolean value = (Boolean) 
                 ((JCheckBox) checkBoxes_.get(cb)).isSelected();
         prefs_.putBoolean(cb, value);
         if (recorderOn_ && value) {
            String key = cb.replaceAll("\\s", "");
            Recorder.recordOption(key, String.valueOf(value));
         }
      }
      
      if (recorderOn_) {
         Recorder.saveCommand();
      }
   }
}
