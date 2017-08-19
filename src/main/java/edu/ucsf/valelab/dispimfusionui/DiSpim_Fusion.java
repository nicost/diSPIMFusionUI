
package edu.ucsf.valelab.dispimfusionui;

import java.io.File;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;



/**
 *
 * @author nico
 */
@Plugin(type = Command.class, menuPath = "Plugins>diSpimFusion") 
public class DiSpim_Fusion implements Command {

   @Parameter (style = FileWidget.DIRECTORY_STYLE, label = "SPIMA Directory")
   private File spimADirectory_;
   
   @Parameter (style = FileWidget.DIRECTORY_STYLE, label = "SPIMB Directory")
   private File spimBDirectory_;
   
   public void run() {
   }
         




   
}
