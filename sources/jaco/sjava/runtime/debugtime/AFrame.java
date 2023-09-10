/**
 *   AFrame.java
 *
 *   @author  Vo Duc Duy (EPFL-DI-LTI)
 *   @version 1.0
 *   @create: 10.06.2001
 *   @modify: 10.07.2001
 *   @modify: 25.01.2002: DUY: add 2 new methods 
 *   - add(GroupButton, JRadioButton,....) and
 *   - add(JRadioButton)
 *   @modify: 29.05.2002 : CP : ATable
 *   Uses a <code>GridBagLayout</code> to place GUI elements on
 *   a window. It provides default values for the fill or weight
 *   parameters.
 *   the J-Elements as well as the A-Elements can be placed using this
 *   frame. It is used by the display
 *   <a href="http://ltiwww.epfl.ch/sJava/version2/Display.html">Generator</a>.
 *
 */

package sjava;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;


public class AFrame extends JFrame {
    public GridBagConstraints c;
    public GridBagLayout gridbag;
    public Container contentPane;
    
    /**
     *   Uses a <code>GridBagLayout</code> to place GUI elements on
     *   a window. It provides default values for the fill or weight
     *   parameters. 
     *   the J-Elements as well as the A-Elements can be placed using this
     *   frame. It is used by the display
     *   <a href="http://ltiwww.epfl.ch/sJava/version2/Display.html">Generator</a>.
     *   @param s string that appears at the top of the created window
     *   @param x,y coordinates of the top left corner on the terminal.
     */

    public AFrame (String s, int x, int y) {
        super(s);
	setLocation(x,y);
	contentPane = getContentPane();
	gridbag = new GridBagLayout();
	c = new GridBagConstraints();
	contentPane.setLayout(gridbag);
	c.fill = c.BOTH;
	// Define closing event
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    });
    }
    /**	
     *  Adds a <code>JButton</code> or a <code>AButton</code> at position <code>x</code>;<code>y</code>
     *  on the window and with a width/height of <code>dx</code>/<code>dy</code>
     *
     *  @param aButton  synchronous button
     *  @param x,y    position on the soft grid
     *  @param dx,dy  number of horizontal/vertical slots occupied by the element
     */

    public void add (JButton aButton, int x, int y, int dx, int dy) {
	c.gridx = x;
	c.gridy = y;
	c.gridwidth = dx;
	c.gridheight = dy;
	c.weightx = 0.5;
        c.fill = c.BOTH;
        gridbag.setConstraints(aButton, c);
        contentPane.add(aButton);
    }	

    /**	
     *  Adds a <code>JCheckBox</code> or a <code>ACheckBox</code> at position <code>x</code>;<code>y</code>
     *  on the window and with a width/height of <code>dx</code>/<code>dy</code>
     *
     *  @param aCheckBox  synchronous check box
     *  @param x,y    position on the soft grid
     *  @param dx,dy  number of horizontal/vertical slots occupied by the element
     */

    public void add (JCheckBox aCheckBox, int x, int y, int dx, int dy) {
	c.gridx = x;
	c.gridy = y;
	c.gridwidth = dx;
	c.gridheight = dy;
	c.weightx = 0.5;
        c.fill = c.BOTH;
        gridbag.setConstraints(aCheckBox, c);
        contentPane.add(aCheckBox);
    }

    /**	
     *  Adds a <code>JComboBox</code> or a <code>AComboBox</code> at position <code>x</code>;<code>y</code>
     *  on the window and with a width/height of <code>dx</code>/<code>dy</code>
     *
     *  @param aCheckBox  synchronous combobox
     *  @param x,y    position on the soft grid
     *  @param dx,dy  number of horizontal/vertical slots occupied by the element
     */

    public void add (JComboBox aChoice, int x, int y, int dx, int dy) {
	c.gridx = x;
	c.gridy = y;
	c.gridwidth = dx;
	c.gridheight = dy;
	c.weightx = 0.5;
        c.fill = c.BOTH;
        gridbag.setConstraints(aChoice, c);
        contentPane.add(aChoice);
    }

    /**	
     *  Adds a <code>JLabel</code> at position <code>x</code>;<code>y</code>
     *  on the window and with a width/height of <code>dx</code>/<code>dy</code>
     *
     *  @param jLabel  label
     *  @param x,y    position on the soft grid
     *  @param dx,dy  number of horizontal/vertical slots occupied by the element
     */

    public void add (JLabel jLab, int x, int y, int dx, int dy) {
	c.gridx = x;
	c.gridy = y;
	c.gridwidth = dx;
	c.gridheight = dy;
        c.weighty = 1.0;
	c.weightx = 1.0;
	gridbag.setConstraints(jLab, c);
	contentPane.add(jLab);
    }

    /**	
     *  Adds a <code>JTextArea</code> at position <code>x</code>;<code>y</code>
     *  on the window and with a width/height of <code>dx</code>/<code>dy</code>
     *
     *  @param tArea  text area
     *  @param x,y    position on the soft grid
     *  @param dx,dy  number of horizontal/vertical slots occupied by the element
     */

    public void add (JTextArea tArea, int x, int y, int dx, int dy) {
	c.gridx = x;
	c.gridy = y;
	c.gridwidth = dx;
	c.gridheight = dy;
        c.fill = c.BOTH;
        c.weighty = 1.0;
	c.weightx = 1.0;
	JScrollPane areaScrollPane = new JScrollPane(tArea);
        gridbag.setConstraints(areaScrollPane, c);
        contentPane.add(areaScrollPane);
    }

    /**	
     *  Adds a <code>JTextField</code> or a <code>ATextField</code> at position <code>x</code>;<code>y</code>
     *  on the window and with a width/height of <code>dx</code>/<code>dy</code>
     *
     *  @param aTextF  text field (synchronous or normal)
     *  @param x,y    position on the soft grid
     *  @param dx,dy  number of horizontal/vertical slots occupied by the element
     */

    public void add (JTextField aTextF, int x, int y, int dx, int dy) {
	c.gridx = x;
	c.gridy = y;
	c.gridwidth = dx;
	c.gridheight = dy;
	c.weightx = 0.5;
        c.fill = c.BOTH;
        gridbag.setConstraints(aTextF, c);
        contentPane.add(aTextF);
    }

    /**	
     *  Adds a <code>JTable</code> or a <code>ATable</code> at position <code>x</code>;<code>y</code>
     *  on the window and with a width/height of <code>dx</code>/<code>dy</code>
     *
     *  @param aTabl  table (synchronous or normal)
     *  @param x,y    position on the soft grid
     *  @param dx,dy  number of horizontal/vertical slots occupied by the element
     */

    public void add (JTable aTabl, int x, int y, int dx, int dy) {
	c.gridx = x;
	c.gridy = y;
	c.gridwidth = dx;
	c.gridheight = dy;
	c.weightx = 1.0;
        c.fill = c.BOTH;
	JScrollPane scrollPane = new JScrollPane(aTabl);
        gridbag.setConstraints(scrollPane, c);
        contentPane.add(scrollPane);
    }
    /**
     *  Adds a <code>JPanel</code> at position <code>x</code>;<code>y</code>
     *  on the window and with a width/height of <code>dx</code>/<code>dy</code>
     *  It can be used to insert elements in a JPanel and then the panel
     *  at a position in the gridbag as shown below:<br>
     *  &nbsp;&nbsp; JPanel panel = new JPanel ();<br>
     *  &nbsp;&nbsp; panel.setLayout (new GridLayout(1,2));  // 1 ligne 2 colonnes<br>
     *  &nbsp;&nbsp; panel.add (aElement);<br>
     *  &nbsp;&nbsp; panel.add (jElement);<br>
     *  &nbsp;&nbsp; frame.add (panel, 1, 2, 2, 1);<br>
     *   
     *  @param panel  panel to insert in the window
     *  @param x,y    position on the soft grid
     *  @param dx,dy  number of horizontal/vertical slots occupied by the element
     */

    public void add (JPanel panel, int x, int y, int dx, int dy) {
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = dx;
        c.gridheight = dy;
        c.weightx = 1.0;
        c.fill = c.BOTH;
        gridbag.setConstraints(panel, c);
        contentPane.add(panel);
    }
    
    /*
     * Adds a Scroll Pane, that should contain an AList
     * 30 Mai 2002, Jordi Corts
     *
     *  @param scrPane  panel to insert in the window
     *  @param x,y    position on the soft grid
     *  @param dx,dy  number of horizontal/vertical slots occupied by the element
     */
    public void add (JScrollPane scrPane, int x, int y, int dx, int dy) {
	c.gridx = x;
	c.gridy = y;
	c.gridwidth = dx;
	c.gridheight = dy;
	c.weightx = 0.5;
        c.fill = c.BOTH;
        gridbag.setConstraints(scrPane, c);
        contentPane.add(scrPane);
    } 
}

