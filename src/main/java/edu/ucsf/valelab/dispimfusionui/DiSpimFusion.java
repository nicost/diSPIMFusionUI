
package edu.ucsf.valelab.dispimfusionui;

import ij.IJ;
import ij.Macro;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import java.awt.EventQueue;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;


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
   public static final String OUTPUTDIRECTORY = "Output Directory";
   public static final String PSFA = "PSFA";
   public static final String PSFB = "PSFB";
   // used as int inputs
   public static final String START = "Start #";
   public static final String END = "End #";
   public static final String INTERVAL = "Interval";
   public static final String TEST = "Test#";
   public static final String DECONVOLUTIONITERATIONS = "Iterations";
   public static final String OUTPUTBITS = "Output BitDepth";
   public static final String GPUDEVICE = "GPU Device #";
   // used as double inputs
   public static final String XPIXELSIZEA = "ImageA x";
   public static final String YPIXELSIZEA = "ImageA y";
   public static final String ZPIXELSIZEA = "ImageA z";
   public static final String XPIXELSIZEB = "ImageB x";
   public static final String YPIXELSIZEB = "ImageB y";
   public static final String ZPIXELSIZEB = "ImageB z";
   public static final String CONVERGENCETHRESHOLD = "Convergence Threshold";
   // ComboBoxes
   public static final String CUDAEXECUTABLE = "Cuda Executable";
   public static final String REGISTRATIONOPTIONS = "Registration Options";
   public static final String IMAGEBROTATION = "Image B Rotation"; 
   // CheckBoxes
   public static final String DO2DREGISTRATION = "Do 2D Registration";
   public static final String CUSTOMIZEMATRIX = "Customize initial transformation matrix";
   public static final String SAVEREGISTEREDIMAGES = "Save Registered Images";
   public static final String SHOWGPUINFO = "Show GPU Device Information";
   // Used only as Preference keys
   public static final String IMAGEANAME = "Image A Name";
   public static final String IMAGEBNAME = "Image B Name";
   
   // miglayout options
   private final String paragraphSpacing_ = "wrap 15px";
   private final String spinnerWidth_ = "w 60:60:60";
   
   private final Preferences prefs_;
   private final DecimalFormat df_;

   private final JComboBox cudaExecutables_;
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
   
   private final String[] textFields_ = {
         SPIMADIRECTORY, SPIMBDIRECTORY, 
         OUTPUTDIRECTORY, PSFA, PSFB };
   private final Map<String, JTextField> jTextFields_ = 
           new HashMap<String, JTextField>(textFields_.length);
   
   private final String[] intSpinnerLabels_ = {
         START, END, INTERVAL, TEST, DECONVOLUTIONITERATIONS, 
         OUTPUTBITS, GPUDEVICE };
   private final Map<String, JSpinner> intSpinners_ =
           new HashMap<String, JSpinner>(intSpinnerLabels_.length);
   
   private final String[] doubleSpinnerLabels_ = {
         XPIXELSIZEA, YPIXELSIZEA, ZPIXELSIZEA, 
         XPIXELSIZEB, YPIXELSIZEB, ZPIXELSIZEB,
         CONVERGENCETHRESHOLD };
   private final Map<String, JSpinner>  doubleSpinners_ = 
           new HashMap<String, JSpinner>(doubleSpinnerLabels_.length);
   
   private final String[] checkBoxLabels_ = {
      DO2DREGISTRATION , CUSTOMIZEMATRIX, 
      SAVEREGISTEREDIMAGES, SHOWGPUINFO };
   private final Map<String, JCheckBox> checkBoxes_ = 
           new HashMap<String, JCheckBox>(checkBoxLabels_.length);
   
   private final File cudaDir_;    
   
   private boolean recorderOn_ = false;
   private File cudaExe_ = null;
   
   private String imgAToken_ = "";
   private String imgBToken_ = "";
   
   
   public DiSpimFusion () {
      
      prefs_ = Preferences.userNodeForPackage(this.getClass());
      
      df_ = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
      df_.setMaximumFractionDigits(12); 
      
      for (String jt : textFields_) {
         jTextFields_.put(jt, new JTextField());
      }
      
      for (String isp : intSpinnerLabels_) {
         intSpinners_.put(isp, new JSpinner(
                 new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1)) );
      }
      ((JSpinner) intSpinners_.get(END)).setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
      ((JSpinner) intSpinners_.get(INTERVAL)).setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
      ((JSpinner) intSpinners_.get(OUTPUTBITS)).setModel(new SpinnerNumberModel(16, 16, 32, 16));
      
      for (String dsp : doubleSpinnerLabels_) {
         doubleSpinners_.put(dsp, new JSpinner(
                 new SpinnerNumberModel(0.165, 0.010, 10.0, 0.001)));
      }
      ((JSpinner) doubleSpinners_.get(CONVERGENCETHRESHOLD)).setModel(
              new SpinnerNumberModel(0.0001, 0.0, 1.0, 0.00001));
      // There seems to be a bug or feature in JSpinner that does not like to show a large number of digits
      // work around that by explicitly setting the max Fraction digits in the editor
      JSpinner.NumberEditor editor = 
              (JSpinner.NumberEditor)((JSpinner) doubleSpinners_.get(CONVERGENCETHRESHOLD)).getEditor();
      editor.getFormat().setMaximumFractionDigits(7);
      
      for (String cb : checkBoxLabels_) {
         checkBoxes_.put(cb, new JCheckBox(cb));
      }
      
      cudaExecutables_ = new JComboBox();
      registrationOptions_ = new JComboBox(regOptionsStr_);
      imageBRotation_ = new JComboBox(imageBRotationsStr_);
      
      String imageJDir = IJ.getDirectory("imagej");
      cudaDir_ = new File(imageJDir + File.separator + "CudaApp");
      
   }
           

   @Override
   public void run(String string) {
      
      if (!IJ.isWindows()) {
         IJ.showMessage("Sorry, this code currently only works on Windows");
         return;
      }
      
      File[] cudaExes = null;
      if (cudaDir_.exists()) {
         cudaExes = cudaDir_.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathName) {
               // TODO: provide platform specific tests
               return pathName.canExecute() && pathName.getName().endsWith(".exe");
            }
         }); 
      }
      if (cudaExes == null || cudaExes.length == 0) {
            IJ.showMessage("Please copy the CudaApp directory into the ImageJ directory");
            return;
      }
      

      recorderOn_ = Recorder.record;
      
      String optionsString = Macro.getOptions();
      if (optionsString == null) {
         optionsString = "";
      }

      // create the dialog
      // TODO: remember window position and size in preferences
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
            
      frame.add(new Label("Cuda executable"));
      for (File cudaExe : cudaExes) {
         cudaExecutables_.addItem(cudaExe.getName());
      }
      cudaExecutables_.setSelectedItem(Macro.getValue(optionsString, 
              CUDAEXECUTABLE.replaceAll("\\s", ""),
              prefs_.get(CUDAEXECUTABLE, cudaExes[0].getName())));
      frame.add(cudaExecutables_, "wrap");
    
      
      addFDChoice(frame, optionsString, SPIMADIRECTORY, true);
      addFDChoice(frame, optionsString, SPIMBDIRECTORY, true);
      addFDChoice(frame, optionsString, OUTPUTDIRECTORY, true);
      
      frame.add(new Label(START));
      intSpinners_.get(START).setValue(Integer.parseInt( Macro.getValue(optionsString, START.replaceAll("\\s", ""), 
               Integer.toString(prefs_.getInt(START, 0))) ));
      frame.add(intSpinners_.get(START), spinnerWidth_);
      frame.add(new Label(INTERVAL), "right");
      intSpinners_.get(INTERVAL).setValue(Integer.parseInt( Macro.getValue(optionsString, INTERVAL.replaceAll("\\s", ""), 
               Integer.toString(prefs_.getInt(INTERVAL, 1))) ));
      frame.add(intSpinners_.get(INTERVAL), spinnerWidth_ +", wrap");
      frame.add(new Label(END));
      intSpinners_.get(END).setValue(Integer.parseInt( Macro.getValue(optionsString, END.replaceAll("\\s", ""), 
               Integer.toString(prefs_.getInt(END, 1))) ));
      frame.add(intSpinners_.get(END), spinnerWidth_);
      String testToolTip = "When registration option is set to \"One image only\", " +
                           "this image will be used. ";
      frame.add(new Label(TEST), "right");
      intSpinners_.get(TEST).setValue(Integer.parseInt( Macro.getValue(optionsString, TEST.replaceAll("\\s", ""), 
               Integer.toString(prefs_.getInt(TEST, 0))) ));
      intSpinners_.get(TEST).setToolTipText(testToolTip);
      frame.add(((JSpinner) intSpinners_.get(TEST)), spinnerWidth_ + ", " + paragraphSpacing_);
      
      frame.add(new Label("Initial Pixel Sizes (in microns)"), "span 4, align left, wrap");
      
      frame.add(new Label (XPIXELSIZEA));
      doubleSpinners_.get(XPIXELSIZEA).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              XPIXELSIZEA.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(XPIXELSIZEA, 0.165)))));
      frame.add(((JSpinner) doubleSpinners_.get(XPIXELSIZEA)), "span 3, split 5, w 50");
      frame.add(new Label("y"), "gapleft push");
      doubleSpinners_.get(YPIXELSIZEA).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              YPIXELSIZEA.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(YPIXELSIZEA, 0.165)))));
      frame.add(doubleSpinners_.get(YPIXELSIZEA));
      frame.add(new Label("z"), "gapleft push");
      doubleSpinners_.get(ZPIXELSIZEA).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              ZPIXELSIZEA.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(ZPIXELSIZEA, 1.0)))));
      frame.add(doubleSpinners_.get(ZPIXELSIZEA), "wrap");
      
      frame.add(new Label (XPIXELSIZEB));
      doubleSpinners_.get(XPIXELSIZEB).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              XPIXELSIZEB.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(XPIXELSIZEB, 0.165)))));
      frame.add(doubleSpinners_.get(XPIXELSIZEB), "span 3, split 5, w 50");
      frame.add(new Label("y"), "gapleft push");
      doubleSpinners_.get(YPIXELSIZEB).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              YPIXELSIZEB.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(YPIXELSIZEB, 0.165)))));
      frame.add(doubleSpinners_.get(YPIXELSIZEB));
      frame.add(new Label("z"), "gapleft push");
      doubleSpinners_.get(ZPIXELSIZEB).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              ZPIXELSIZEB.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(ZPIXELSIZEB, 1.0)))));
      frame.add(doubleSpinners_.get(ZPIXELSIZEB), paragraphSpacing_);
      
      frame.add(new Label("Registration Options"));
      registrationOptions_.setSelectedItem(Macro.getValue(optionsString,
              REGISTRATIONOPTIONS.replaceAll("\\s", ""),
              prefs_.get(REGISTRATIONOPTIONS, regOptionsStr_[0])));
      frame.add(registrationOptions_, "span 3, align left, wrap");
      
      frame.add(new Label("ImageB Rotation"));
      imageBRotation_.setSelectedItem(Macro.getValue(optionsString,
              IMAGEBROTATION.replaceAll("\\s", ""),
              prefs_.get(IMAGEBROTATION, imageBRotationsStr_[2])));
      frame.add(imageBRotation_, "span 3, alig left, " + paragraphSpacing_);
      
      checkBoxes_.get(DO2DREGISTRATION).setSelected(
               Boolean.parseBoolean(
                  Macro.getValue(optionsString, DO2DREGISTRATION.replaceAll("\\s", ""),
                  String.valueOf(prefs_.getBoolean(DO2DREGISTRATION, false)))));
      frame.add(checkBoxes_.get(DO2DREGISTRATION), "span 2, left");
       
      checkBoxes_.get(SAVEREGISTEREDIMAGES).setSelected(
               Boolean.parseBoolean(
                  Macro.getValue(optionsString, SAVEREGISTEREDIMAGES.replaceAll("\\s", ""),
                  String.valueOf(prefs_.getBoolean(SAVEREGISTEREDIMAGES, false)))));
      frame.add(checkBoxes_.get(SAVEREGISTEREDIMAGES), "span 2, left, wrap");
        
      ((JCheckBox) checkBoxes_.get(CUSTOMIZEMATRIX)).setSelected(
               Boolean.parseBoolean(
                  Macro.getValue(optionsString, CUSTOMIZEMATRIX.replaceAll("\\s", ""),
                  String.valueOf(prefs_.getBoolean(CUSTOMIZEMATRIX, false)))));
      frame.add(((JCheckBox) checkBoxes_.get(CUSTOMIZEMATRIX)), "span3, left, wrap");

      frame.add(new Label(CONVERGENCETHRESHOLD), "span 2, left");
      doubleSpinners_.get(CONVERGENCETHRESHOLD).setValue(Double.parseDouble(Macro.getValue(optionsString, 
              CONVERGENCETHRESHOLD.replaceAll("\\s", ""),
              Double.toString(prefs_.getDouble(CONVERGENCETHRESHOLD, 0.00001)))));
      frame.add(doubleSpinners_.get(CONVERGENCETHRESHOLD), "growx, " + paragraphSpacing_);
      
      frame.add(new Label("Deconvolution Options"), "span 4, left, wrap");
      addFDChoice(frame, optionsString, PSFA, false);
      addFDChoice(frame, optionsString, PSFB, false);
      intSpinners_.get(DECONVOLUTIONITERATIONS).setValue(Integer.parseInt(Macro.getValue(
               optionsString, DECONVOLUTIONITERATIONS.replaceAll("\\s", ""),
               Integer.toString(prefs_.getInt(DECONVOLUTIONITERATIONS, 10)))));
      frame.add(new Label(DECONVOLUTIONITERATIONS));
      frame.add(intSpinners_.get(DECONVOLUTIONITERATIONS), spinnerWidth_);
      intSpinners_.get(OUTPUTBITS).setValue(Integer.parseInt(Macro.getValue(
              optionsString, OUTPUTBITS.replaceAll("\\s", ""), 
              Integer.toString(prefs_.getInt(OUTPUTBITS, 16)))));
      frame.add(new Label(OUTPUTBITS));
      frame.add(intSpinners_.get(OUTPUTBITS), spinnerWidth_ + ", " + paragraphSpacing_);

      frame.add(new Label("GPU Options"), "span 4, left, wrap");
      checkBoxes_.get(SHOWGPUINFO).setSelected(
               Boolean.parseBoolean(
                  Macro.getValue(optionsString, SHOWGPUINFO.replaceAll("\\s", ""),
                  String.valueOf(prefs_.getBoolean(SHOWGPUINFO, false)))));
      frame.add(checkBoxes_.get(SHOWGPUINFO), "span3, left, wrap");
      intSpinners_.get(GPUDEVICE).setValue(Integer.parseInt(Macro.getValue(
              optionsString, GPUDEVICE.replaceAll("\\s", ""), 
              Integer.toString(prefs_.getInt(GPUDEVICE, 0)))));
      frame.add(new Label(GPUDEVICE));
      frame.add(intSpinners_.get(GPUDEVICE), spinnerWidth_ + ", " + paragraphSpacing_);
      
      JButton okButton = new JButton("OK");
      okButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            if (checkSettings()) {
               storeSettings();
               frame.setVisible(false);
               frame.dispose();

               Runnable executor = new Runnable() {
                  @Override
                  public void run() {
                     execute();
                  }
               };
               (new Thread(executor, "diSPIM CUDA execution")).start();

            }
         }
      });
      frame.add(okButton, "span 4, split 4, tag ok");
      
      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(new ActionListener(){
         @Override
         public void actionPerformed(ActionEvent e) {
            frame.setVisible(false);
            frame.dispose();
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

            final Runnable chooserRunnable = new Runnable() {
               @Override
               public void run() {
                  JFileChooser chooser = new JFileChooser();
                  chooser.setDialogTitle(msg);

                  if (dir) {
                     chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                  } else {
                     chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                  }
                  String openLocation = pathField.getText();
                  if (openLocation == null || openLocation.length() == 0) {
                     openLocation = OpenDialog.getDefaultDirectory();
                  }
                  if (openLocation != null) {
                     File f = new File(openLocation);
                     if (IJ.debugMode) {
                        IJ.log("DirectoryChooser,setSelectedFile: " + f);
                     }
                     chooser.setSelectedFile(f);
                  }
                  chooser.setApproveButtonText("Select");
                  if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                     File file = chooser.getSelectedFile();
                     String result = file.getAbsolutePath();
                     if (dir) {
                        if (!result.endsWith(File.separator)) {
                           result += File.separator;
                        }
                     }
                     OpenDialog.setDefaultDirectory(result);
                     pathField.setText(result);

                  }
               }
            };

            if (SwingUtilities.isEventDispatchThread()) {
               chooserRunnable.run();
            } else{
               try {
                  EventQueue.invokeAndWait(chooserRunnable);
               } catch (InterruptedException ie) {
               } catch (InvocationTargetException ex) {
               }
            }
         }
      });
      
      return button;
   }
   
   /**
    * Creates a row in the given frame with a label, textfield, and button
    * that the user may use to place a file or directory in the textfield
    * @param frame Frame to which the row will be added
    * @param optionsString OptionsString as passed by an ImageJ macro
    * @param identifier Key to this field, will be used as label
    * @param directory directory when true, file otherwise
    */
   private void addFDChoice(
           final JFrame frame, 
           final String optionsString, 
           final String identifier,
           final boolean directory) {        
      frame.add(new Label(identifier));
      JTextField tmp = (JTextField)jTextFields_.get(identifier);
      frame.add(tmp, "span 3, split 2, width 250, growx");
      tmp.setText(Macro.getValue(optionsString, identifier.replaceAll("\\s",""), 
              prefs_.get(identifier, "")));
      frame.add(chooserButton(identifier, tmp, directory), "wrap");   
   }
 
   /**
    * Checks that all required settings are present
    * Also deduces the Image A and B fields from the contents of the SPIM A and B
    * directories.  
    * Shows an ImageJ error message when something is not right
    * @return true when all settings are fine, false if anything is not right.
    */
   private boolean checkSettings() {
      
      cudaExe_ = new File (cudaDir_.getPath() + File.separator + 
              (String) cudaExecutables_.getSelectedItem());
      if (cudaExe_ == null || !cudaExe_.canExecute()) {
         ij.IJ.showMessage("Can not find or execute " + cudaExe_.getName());
         return false;
      }
      final String[] files = {SPIMADIRECTORY, SPIMBDIRECTORY,  
         OUTPUTDIRECTORY, PSFA, PSFB};
      for (String file : files ) {
         String value = ((JTextField) jTextFields_.get(file)).getText();
         File f = new File(value);
         if (!f.exists()) {
            ij.IJ.showMessage("Can not read " + file);
            return false;         
         }
      }
      try {
         imgAToken_ = getImagePrefix(((JTextField) jTextFields_.get(SPIMADIRECTORY)).getText());
         prefs_.put(IMAGEANAME, imgAToken_);
         imgBToken_ = getImagePrefix(((JTextField) jTextFields_.get(SPIMBDIRECTORY)).getText());
         prefs_.put(IMAGEBNAME, imgBToken_);
      } catch (FileNotFoundException fnfe) {
         return false;
      }
      
   
      return true;
   }
   
   /**
    * Expects files in a directory to be named token_xx.tif
    * Returns the part before the last underscore
    * Checks that all tif files in a directory follow the same convention
    * does not check for correct numbering
    * If anything is incorrect, throws a FileNotFoundException after 
    * displaying an ImageJ message
    * @param dir directory to be examined
    * @return token
    * @throws FileNotFoundException 
    */
   private String getImagePrefix (final String dir) throws FileNotFoundException {
      final File spimImageDir = new File(dir);
      File[] listFiles = spimImageDir.listFiles(new FilenameFilter() {
         @Override
         public boolean accept(File dir, String name) {
            return (name.endsWith(".tif") || (name.endsWith(".tiff"))) &&
                   !name.startsWith(".") ;
         }
      });
      if (listFiles == null || listFiles.length < 1) {
         ij.IJ.showMessage("No Files found in " + dir );
         throw new FileNotFoundException();
      }
      String firstName = listFiles[0].getName();
      int indexOfLastUnderscore = firstName.lastIndexOf("_") + 1;
      if (indexOfLastUnderscore < 1) {
         ij.IJ.showMessage ("Tif files in " + dir + " are not named correctly");
         throw new FileNotFoundException();
      }
      final String token = firstName.substring(0, indexOfLastUnderscore);
      for (File lf : listFiles) {
         if (!token.equals(lf.getName().substring(0, indexOfLastUnderscore))) {
            ij.IJ.showMessage ("Not all tif files in " + dir + " start with " + token );
            throw new FileNotFoundException();
         }
      }
      
      return token;
   }
   
   /**
    * Write settings to usernode preferences, and the ImageJ macro recorder
    */
   private void storeSettings () {
      if (recorderOn_) {
         Recorder.setCommand("diSPIM Fusion");
      }
      
      prefs_.put(CUDAEXECUTABLE, cudaExe_.getName());
      if (recorderOn_) {
         String key = CUDAEXECUTABLE.replaceAll("\\s", "");
         String value = cudaExe_.getName().replace("\\", "\\\\");
         Recorder.recordOption(key, value);
      }
      
      prefs_.put(REGISTRATIONOPTIONS, (String) registrationOptions_.getSelectedItem());
      if (recorderOn_) {
         String key = REGISTRATIONOPTIONS.replaceAll("\\s", "");
         String value = ((String) registrationOptions_.getSelectedItem()).replace("\\", "\\\\");
         Recorder.recordOption(key, value);
      }
      
      prefs_.put(IMAGEBROTATION, (String) imageBRotation_.getSelectedItem());
      if (recorderOn_) {
         String key = IMAGEBROTATION.replaceAll("\\s", "");
         String value = ((String) registrationOptions_.getSelectedItem()).replace("\\", "\\\\");
         Recorder.recordOption(key, value);
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
   
   private void execute() {
      List<String> command = new ArrayList<String>();
      command.add(cudaExe_.getAbsolutePath());
      command.add(prefs_.get(SPIMADIRECTORY, " "));
      command.add(prefs_.get(SPIMBDIRECTORY, " "));
      command.add(prefs_.get(IMAGEANAME, " "));
      command.add(prefs_.get(IMAGEBNAME, " "));
      command.add(prefs_.get(OUTPUTDIRECTORY, " "));
      command.add(String.valueOf(prefs_.getInt(START, 0)));
      command.add(String.valueOf(prefs_.getInt(END, 1)));
      command.add(String.valueOf(prefs_.getInt(INTERVAL, 1)));
      command.add(String.valueOf(prefs_.getDouble(TEST, 1)));
      command.add(String.valueOf(prefs_.getDouble(XPIXELSIZEA, 0.165)));
      command.add(String.valueOf(prefs_.getDouble(YPIXELSIZEA, 0.165)));
      command.add(String.valueOf(prefs_.getDouble(ZPIXELSIZEA, 1.0)));
      command.add(String.valueOf(prefs_.getDouble(XPIXELSIZEB, 0.165)));
      command.add(String.valueOf(prefs_.getDouble(YPIXELSIZEB, 0.165)));
      command.add(String.valueOf(prefs_.getDouble(ZPIXELSIZEB, 1.0)));
      command.add(String.valueOf(Arrays.asList(regOptionsStr_).indexOf(
              prefs_.get(REGISTRATIONOPTIONS, "All images dependently")) + 1));
      int imbRotation = Arrays.asList(imageBRotationsStr_).indexOf(
              prefs_.get(IMAGEBROTATION, "No Rotation"));
      if (imbRotation == 2) { imbRotation = -1;}
      command.add(String.valueOf(imbRotation));
      boolean reg2D = prefs_.getBoolean(DO2DREGISTRATION, false);
      boolean useInputMatrix = prefs_.getBoolean(CUSTOMIZEMATRIX, false);
      if (useInputMatrix) {
         reg2D = false;
         // TODO: path to transformation matrix
      }
      command.add(reg2D ? "1" : "0");
      command.add(useInputMatrix ? "1" : "0");
      command.add("Balabalabala"); // path to transformation matrix
      command.add(prefs_.getBoolean(SAVEREGISTEREDIMAGES, false) ? "1" : "0");
      command.add(df_.format( prefs_.getDouble(CONVERGENCETHRESHOLD, 0.0001)));
      command.add (String.valueOf(prefs_.getInt(DECONVOLUTIONITERATIONS, 10)));
      command.add(String.valueOf(prefs_.getInt(OUTPUTBITS, 16)));
      command.add(prefs_.get(PSFA, " "));
      command.add(prefs_.get(PSFB, " "));
      // GPU stuff
      command.add(prefs_.getBoolean(SHOWGPUINFO, true) ? "1" : "0");
      command.add(String.valueOf(prefs_.getInt(GPUDEVICE, 0)));
      
      for (String token : command) {
         ij.IJ.log(token);
      }
      int nrArgs = command.size() - 1;
      ij.IJ.log ("There are: " + nrArgs + " arguments");
              
      ProcessBuilder cmd = new ProcessBuilder(command);
      cmd.directory(cudaExe_.getParentFile());
      try {
         // actually start execution
         Process process = cmd.start(); 
         
         // show output in IJ.log window
         BufferedReader reader = 
                new BufferedReader(new InputStreamReader(process.getInputStream()));
         String line = reader.readLine();
         while ( line != null) {
            ij.IJ.log(line);
            line = reader.readLine();
         }
         
         int result = process.waitFor();
         if (result != 0) {
            // return error to user:
            ij.IJ.log("diSPIM Cuda execution failed with error code: " + result);
         }
      } catch (IOException ex) {}
      catch (InterruptedException ex) {}
   }
   
}