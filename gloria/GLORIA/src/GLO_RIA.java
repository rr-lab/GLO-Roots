/**
* @author Guillaume Lobet | Universit� de Li�ge
* @date: 2014-06-16
* 
* 
**/

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import ij.ImageJ;
import ij.plugin.frame.PlugInFrame;


public class GLO_RIA extends PlugInFrame{
	
	static GLO_RIA ra;
	
	private static final long serialVersionUID = -2516812747038073446L;

	
	/**
	 * 
	 */
	public GLO_RIA() {
		super("GLO-RIA");
		new Interface();
	}
	
	/**
	 * Main class of the plugin
	 * @param args
	 */
	public static void main(String args[]) {
	    
		ra = new GLO_RIA();
		
		// What to do when closing the window
		ImageJ ij = new ImageJ();
		ij.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				ra.dispose();
				System.exit(0);
			}
		});
	}
}
