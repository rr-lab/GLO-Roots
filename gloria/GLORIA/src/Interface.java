/**
* @author Guillaume Lobet | Université de Liège
* @date: 2014-06-16
*
* Create the interface for the GLO-RIA plugin
*
**/


import ij.IJ;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;


public class Interface extends JFrame implements ItemListener, ActionListener, ChangeListener{

//------------------------------------
// GENERAL
	JTabbedPane tp;
	private static Interface instance = null;


//------------------------------------
//	CARTOGRAPHER PANEL
	JButton cartoAnalysisButton, cartoNextButton, cartoFolderButton, cartoCSVButton; 
	JTextField cartoImageFolder, cartoCSVFile;
	JTextField cartoScalePix, cartoScaleCm, cartoNTimeSerie;
	JCheckBox cartoUseRSML, cartoUseAllSegments, cartoOnlySeg, cartoConnectReporters, cartoLowContrast, cartoSoil;	
	JComboBox cartoConnection;
	JEditorPane infoPane;		

	public SegmentListTree segmentListTree;
	public ReporterTableModel repTableModel;
	public JTable repTable;
	RootSystem rs;
	RootCartographer glora;

//------------------------------------
// ROOT PANEL
	JButton rootAnalysisButton, rootFolderButton, rootCSVButton;
	JTextField rootImageFolder, rootCSVFile;
	JTextField rootScalePix, rootScaleCm, rootStartingDate, rootMinSize, nEFD, nTimeSerie, nBinDir, nHBin, nWBin;
	JCheckBox blackRoots, rootGlobal, rootLocal, rootEFD, timeSerie, manualCorrection, rootDirectional, rootLowContrast, rootWide;
	JComboBox directionalityMethod;
	RootAnalysis ran;
	JPanel rootParamGlobalPanel, rootParamLocalPanel;

	private static final long serialVersionUID = -6312379859247272449L;


	/**
	 * Constructor
	 */
	public Interface(){
		instance = this;
		build();
	}



	/**
	 * Build the interface
	 */
	private void build(){
		this.setTitle("GLO-Root Image Analysis - GLORIA -");
		this.setSize(800,750);
		this.setLocationRelativeTo(null);
		this.setResizable(true) ;
		this.getContentPane().add(getPanel());
		this.setVisible(true);
	}

	/**
	 * Get the current instance of the interface. Used to update the info panel in the
	 * cartographer section
	 * @return
	 */
	static public Interface getInstance() {return instance;}


	/**
	 * Create the general panel
	 * @return
	 */
	private JPanel getPanel(){

	      tp = new JTabbedPane();

	      tp.addTab("GLO-RIA", getAboutTab());
	      tp.addTab("RootSystem", getRootTab());
	      tp.addTab("RootReporter", getCartographerTab());
	      // Final container
	      JPanel container = new JPanel(new BorderLayout()) ;
	      container.add(tp);

	      return container;
	}

	/**
	 * Create the "About" tab for the interface
	 * @return
	 */
	private JScrollPane getAboutTab(){


		ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/gloria_logo.png")));
		JLabel logo = new JLabel("",icon, JLabel.CENTER);
		logo.setPreferredSize(new Dimension(300, 250));

		// About

		JTextPane aboutPane = new JTextPane();
		aboutPane.setEditable(false);

		aboutPane.setText(displayAboutText());
		SimpleAttributeSet bSet = new SimpleAttributeSet();
		StyleConstants.setAlignment(bSet, StyleConstants.ALIGN_CENTER);
		StyledDocument doc = aboutPane.getStyledDocument();
		doc.setParagraphAttributes(0, doc.getLength(), bSet, false);

		JScrollPane aboutView = new JScrollPane(aboutPane);
		aboutView.setBorder(BorderFactory.createLineBorder(Color.gray));

		JPanel aboutBox = new JPanel(new BorderLayout());
		aboutBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		aboutBox.add(aboutView, BorderLayout.NORTH);

		// Disclaimer

		JTextPane disclaimerPane = new JTextPane();
		disclaimerPane.setEditable(false);

		disclaimerPane.setText(displayDisclaimerText());
		SimpleAttributeSet bSet1 = new SimpleAttributeSet();
		StyleConstants.setAlignment(bSet1, StyleConstants.ALIGN_CENTER);
		StyledDocument doc1 = disclaimerPane.getStyledDocument();
		doc1.setParagraphAttributes(0, doc1.getLength(), bSet1, false);

		JScrollPane disclaimerView = new JScrollPane(disclaimerPane);
		disclaimerView.setBorder(BorderFactory.createLineBorder(Color.gray));

		JPanel disclaimerBox = new JPanel(new BorderLayout());
		disclaimerBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		disclaimerBox.add(disclaimerView, BorderLayout.NORTH);

		JPanel p1 = new JPanel(new BorderLayout());
		p1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		p1.add(logo, BorderLayout.NORTH);
		p1.add(aboutBox, BorderLayout.CENTER);
		p1.add(disclaimerBox, BorderLayout.SOUTH);

	      return new JScrollPane(p1);
	}


	/**
	 * Create the "Global analysis" tab for the interface
	 * @return
	 */
	private JScrollPane getRootTab(){

		// Image panel
	      rootFolderButton = new JButton("Choose folder");
	      rootFolderButton.setActionCommand("IMAGE_FOLDER_ROOT");
	      rootFolderButton.addActionListener(this);

	      rootImageFolder = new JTextField("[Choose a folder containing the images]",35);
//	      rootImageFolder = new JTextField("/Users/guillaumelobet/Desktop/test/structure/high_quality/end_points/ecotypes/",35);
//	      rootImageFolder = new JTextField("/Users/guillaumelobet/Desktop/test/structure/high_quality/time_series/9_R10/",35);

	      JLabel title1 = new JLabel("Images source");
	      Font f1 = title1.getFont();
	      title1.setFont(f1.deriveFont(f1.getStyle() ^ Font.BOLD));

	      JPanel subpanel1 = new JPanel();
	      subpanel1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      GridBagConstraints gbc1 = new GridBagConstraints();
	      gbc1.anchor = GridBagConstraints.WEST;
	      GridBagLayout gbl1 = new GridBagLayout();
	      subpanel1.setLayout(gbl1);
	      gbc1.gridx = 0;
	      gbc1.gridy = 0;
	      subpanel1.add(rootImageFolder, gbc1);
	      gbc1.gridx = 1;
	      subpanel1.add(rootFolderButton, gbc1);

	      JPanel panel1 = new JPanel(new BorderLayout());
	      panel1.setBorder(BorderFactory.createLineBorder(Color.gray));
	      panel1.add(subpanel1, BorderLayout.WEST);

	      JPanel imageBox = new JPanel(new BorderLayout());
	      imageBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      imageBox.add(title1, BorderLayout.NORTH);
	      imageBox.add(panel1, BorderLayout.SOUTH);

		// Parameter panel

	      rootScalePix = new JTextField("138.6", 5);
	      rootScaleCm = new JTextField("1", 5);

	      rootMinSize = new JTextField("20", 5);

	      blackRoots = new JCheckBox("Black roots", false);

	      JPanel subpanel2 = new JPanel();
	      subpanel2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      GridBagConstraints gbc2 = new GridBagConstraints();
	      gbc2.anchor = GridBagConstraints.WEST;
	      GridBagLayout gbl2 = new GridBagLayout();
	      subpanel2.setLayout(gbl2);

	      JPanel panel2 = new JPanel(new BorderLayout());
	      panel2.setBorder(BorderFactory.createLineBorder(Color.gray));
	      panel2.add(subpanel2, BorderLayout.WEST);

	      gbc2.gridy = 0;
	      gbc2.gridx = 0;
	      subpanel2.add(new JLabel("Image scale || "), gbc2);
	      gbc2.gridx = 1;
	      subpanel2.add(rootScaleCm, gbc2);
	      gbc2.gridx = 2;
	      subpanel2.add(new JLabel(" Cm / "), gbc2);
	      gbc2.gridx = 3;
	      subpanel2.add(rootScalePix, gbc2);
	      gbc2.gridx = 4;
	      subpanel2.add(new JLabel(" Pixels"), gbc2);


	      gbc2.gridy = 1;
	      gbc2.gridx = 0;
	      subpanel2.add(new JLabel("Min root size || "), gbc2);
	      gbc2.gridx = 1;
	      subpanel2.add(rootMinSize, gbc2);

	      gbc2.gridy = 2;
	      gbc2.gridx = 0;
	      subpanel2.add(blackRoots, gbc2);

	      JLabel title2 = new JLabel("Analysis parameters");
	      Font f2 = title2.getFont();
	      title2.setFont(f2.deriveFont(f2.getStyle() ^ Font.BOLD));

	      JPanel paramBox = new JPanel(new BorderLayout());
	      paramBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      paramBox.add(title2, BorderLayout.NORTH);
	      paramBox.add(panel2, BorderLayout.SOUTH);

		// Option panel

	      rootGlobal = new JCheckBox("Global Analysis", true);

	      manualCorrection = new JCheckBox("Manual correction", false);
	      rootLowContrast = new JCheckBox("Low contrast", false);
	      rootWide = new JCheckBox("Wide rhizotron", false);

	      rootLocal = new JCheckBox("Local Analysis", true);
	      timeSerie = new JCheckBox("Time-series", false);

	      rootDirectional = new JCheckBox("Directional Analysis || ", false);
	      String[] nameType = {"Fourrier", "Local"};
	      directionalityMethod = new JComboBox(nameType);
	      directionalityMethod.setSelectedIndex(0);
	      directionalityMethod.addItemListener(this);
	      nBinDir= new JTextField("90", 3);
	      nHBin= new JTextField("1", 3);
	      nWBin= new JTextField("1", 3);

	      rootEFD = new JCheckBox("EFD Analysis || ", false);
	      rootEFD.addItemListener(this);
	      nEFD = new JTextField("5", 4);

	      JPanel subpanel3 = new JPanel();
	      subpanel3.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      GridBagConstraints gbc3 = new GridBagConstraints();
	      gbc3.anchor = GridBagConstraints.WEST;
	      GridBagLayout gbl3 = new GridBagLayout();
	      subpanel3.setLayout(gbl3);

	      JPanel panel3 = new JPanel(new BorderLayout());
	      panel3.setBorder(BorderFactory.createLineBorder(Color.gray));
	      panel3.add(subpanel3, BorderLayout.WEST);

	      gbc3.gridy = 2;
	      gbc3.gridx = 0;
	      subpanel3.add(rootGlobal, gbc3);

	      gbc3.gridy = 3;
	      gbc3.gridx = 0;
	      subpanel3.add(rootLocal, gbc3);
	      gbc3.gridx = 1;
	      subpanel3.add(timeSerie, gbc3);
	      
	      gbc3.gridy = 4;
	      gbc3.gridx = 0;
	      subpanel3.add(rootEFD, gbc3);
	      gbc3.gridx = 1;
	      subpanel3.add(new JLabel(" Harmonics : "), gbc3);
	      gbc3.gridx = 2;
	      subpanel3.add(nEFD, gbc3);

	      gbc3.gridy = 7;
	      gbc3.gridx = 0;
	      subpanel3.add(rootDirectional, gbc3);
	      gbc3.gridx = 1;
	      subpanel3.add(new JLabel(" Method : "), gbc3);
	      gbc3.gridx = 2;
	      subpanel3.add(directionalityMethod, gbc3);
	      gbc3.gridx = 3;
	      subpanel3.add(new JLabel(" Theta Bins : "), gbc3);
	      gbc3.gridx = 4;
	      subpanel3.add(nBinDir, gbc3);
	      gbc3.gridx = 5;
	      subpanel3.add(new JLabel(" X Bins : "), gbc3);
	      gbc3.gridx = 6;
	      subpanel3.add(nWBin, gbc3);
	      gbc3.gridx = 7;
	      subpanel3.add(new JLabel(" Y Bins : "), gbc3);
	      gbc3.gridx = 8;
	      subpanel3.add(nHBin, gbc3);

	      gbc3.gridy = 8;
	      gbc3.gridx = 0;
	      subpanel3.add(manualCorrection, gbc3);
	      
	      gbc3.gridy = 9;
	      gbc3.gridx = 0;
	      subpanel3.add(rootLowContrast, gbc3);
	      
	      gbc3.gridy = 10;
	      gbc3.gridx = 0;
	      subpanel3.add(rootWide, gbc3);	      

	      JLabel title3 = new JLabel("Analysis options");
	      title3.setFont(f2.deriveFont(f2.getStyle() ^ Font.BOLD));

	      JPanel optionBox = new JPanel(new BorderLayout());
	      optionBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      optionBox.add(title3, BorderLayout.NORTH);
	      optionBox.add(panel3, BorderLayout.SOUTH);

		// Output panel


	      rootCSVFile = new JTextField("[Choose CSV file to save the data]", 35);
//	      rootCSVFile = new JTextField("/Users/guillaumelobet/Desktop/test.csv", 35);
	      rootCSVFile.setEnabled(true);


	      rootCSVButton = new JButton("Choose folder");
	      rootCSVButton.setActionCommand("CSV_FOLDER_ROOT");
	      rootCSVButton.addActionListener(this);

	      JPanel subpanel4 = new JPanel();
	      subpanel4.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      GridBagConstraints gbc4 = new GridBagConstraints();
	      gbc4.anchor = GridBagConstraints.WEST;
	      GridBagLayout gbl4 = new GridBagLayout();
	      subpanel4.setLayout(gbl4);

	      JPanel panel4 = new JPanel(new BorderLayout());
	      panel4.setBorder(BorderFactory.createLineBorder(Color.gray));
	      panel4.add(subpanel4, BorderLayout.WEST);

	      gbc4.gridy = 0;
	      gbc4.gridx = 0;
	      subpanel4.add(rootCSVFile, gbc4);
	      gbc4.gridx = 1;
	      subpanel4.add(rootCSVButton, gbc4);

	      JLabel subtitle4 = new JLabel("Output parameters");
	      Font f3 = subtitle4.getFont();
	      subtitle4.setFont(f3.deriveFont(f3.getStyle() ^ Font.BOLD));

	      JPanel title4 = new JPanel(new BorderLayout());
	      title4.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      title4.add(subtitle4, BorderLayout.WEST);

	      JPanel outputBox = new JPanel(new BorderLayout());
	      outputBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      outputBox.add(title4, BorderLayout.NORTH);
	      outputBox.add(panel4, BorderLayout.SOUTH);

	// About panel

	      JTextPane aboutRootPane = new JTextPane();
	      aboutRootPane.setEditable(false);

	      aboutRootPane.setText(displayAboutRootText());
	      SimpleAttributeSet bSet = new SimpleAttributeSet();
	      StyleConstants.setAlignment(bSet, StyleConstants.ALIGN_CENTER);
	      StyledDocument doc = aboutRootPane.getStyledDocument();
	      doc.setParagraphAttributes(0, doc.getLength(), bSet, false);

	      JScrollPane aboutLeafView = new JScrollPane(aboutRootPane);
	      aboutLeafView.setBorder(BorderFactory.createLineBorder(Color.gray));

	      JPanel aboutBox = new JPanel(new BorderLayout());
	      aboutBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      aboutBox.add(aboutLeafView, BorderLayout.NORTH);


     // Button panel

	      rootAnalysisButton = new JButton("Run analysis");
	      rootAnalysisButton.setActionCommand("RUN_ANALYSIS_root");
	      rootAnalysisButton.addActionListener(this);
	      rootAnalysisButton.setEnabled(true);

	      JPanel buttons = new JPanel(new BorderLayout());
	      buttons.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
	      buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
	      buttons.add(rootAnalysisButton);

	      JPanel buttonPanel = new JPanel(new BorderLayout());
	      buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      buttonPanel.add(buttons, BorderLayout.EAST);

	      // All panels

	      JPanel allBoxes = new JPanel(new BorderLayout());
	      allBoxes.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      allBoxes.setLayout(new BoxLayout(allBoxes, BoxLayout.Y_AXIS));
	      allBoxes.add(imageBox);
	      allBoxes.add(optionBox);
	      allBoxes.add(paramBox);
	      allBoxes.add(outputBox);
	      allBoxes.add(aboutBox);

	      JPanel panel = new JPanel(new BorderLayout());
	      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      panel.add(allBoxes, BorderLayout.NORTH);
	      panel.add(buttonPanel, BorderLayout.SOUTH);

	      return new JScrollPane(panel);
	}


  /**
    * Create the cartographer tab
    * @return
    */
   private JPanel getCartographerTab(){

	   segmentListTree = new SegmentListTree();

	   JLabel title3 = new JLabel("Segment list");
	   Font f2 = title3.getFont();
	   title3.setFont(f2.deriveFont(f2.getStyle() ^ Font.BOLD));

	   JPanel segmentBox = new JPanel(new BorderLayout());
	   segmentBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	   segmentBox.add(title3, BorderLayout.NORTH);
	   segmentBox.add(segmentListTree, BorderLayout.CENTER);
//		   segmentBox.setLayout(new BoxLayout(segmentBox, BoxLayout.Y_AXIS));
//		   segmentBox.add(title3);
//		   segmentBox.add(segmentListTree);

	   infoPane = new JEditorPane();
	   infoPane.setEditable(false);
	   infoPane.setPreferredSize(new Dimension(300, 150));
	   infoPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	   JScrollPane infoView = new JScrollPane(infoPane);
	   infoPane.setText("Please select a root");

	   JLabel infoTitle= new JLabel("Informations");
	   infoTitle.setFont(f2.deriveFont(f2.getStyle() ^ Font.BOLD));

	   JPanel infoBox = new JPanel(new BorderLayout());
	   infoBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	   infoBox.add(infoTitle, BorderLayout.NORTH);
	   infoBox.add(infoView, BorderLayout.CENTER);

	   JSplitPane ppo2 = new  JSplitPane(JSplitPane.VERTICAL_SPLIT);
	   ppo2.setDividerLocation(410);
	   ppo2.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
	   ppo2.setBottomComponent(infoBox);
	   ppo2.setTopComponent(getApexOptionPanel());

//	   // Marks list panel
//	   apexTableModel = new ApexTableModel();
//	   apexTable = new JTable(apexTableModel);
//	   apexTable.setAutoCreateColumnsFromModel(true);
//	   apexTable.setColumnSelectionAllowed(false);
//	   apexTable.setRowSelectionAllowed(true);
//	   apexTable.setAutoCreateRowSorter(true);
//	   apexTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//	   apexTable.getSelectionModel().addListSelectionListener(
//			   new ListSelectionListener() {
//				   public void valueChanged(ListSelectionEvent event) {
//					   try{
//						   Apex ap = (Apex) apexTableModel.getSelectedApex(apexTable.getSelectedRow());
//						   if(ap != null){
//							   rs.selectApex(ap, true);
//							   ap.setSelected(true);
//							   rs.repaint();
//							   infoPane.setText(rs.displayApexInfo());
//						   }
//					   }
//					   catch(ArrayIndexOutOfBoundsException e){}
//				   }
//			   }
//		);	   
//	   JScrollPane apexPane = new JScrollPane(apexTable);
	   
	   // Marks list panel
	   repTableModel = new ReporterTableModel();
	   repTable = new JTable(repTableModel);
	   repTable.setAutoCreateColumnsFromModel(true);
	   repTable.setColumnSelectionAllowed(false);
	   repTable.setRowSelectionAllowed(true);
	   repTable.setAutoCreateRowSorter(true);
	   repTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	   repTable.getSelectionModel().addListSelectionListener(
			   new ListSelectionListener() {
				   public void valueChanged(ListSelectionEvent event) {
					   try{
						   Reporter rep = (Reporter) repTableModel.getSelectedReporter(repTable.getSelectedRow());
						   if(rep != null){
							   rs.selectReporter(rep, true);
							   rep.setSelected(true);
							   rs.repaint();
							   infoPane.setText(rs.displayReporterInfo());
						   }
					   }
					   catch(ArrayIndexOutOfBoundsException e){}
				   }
			   }
		);	   
	   JScrollPane apexPane = new JScrollPane(repTable);
	   
	   JLabel title4 = new JLabel("Reporter list");
	   title4.setFont(f2.deriveFont(f2.getStyle() ^ Font.BOLD)); 
			
	   JPanel apexBox = new JPanel(new BorderLayout());
	   apexBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	   apexBox.add(title4, BorderLayout.NORTH);
	   apexBox.add(apexPane, BorderLayout.CENTER);
//		   apexBox.setLayout(new BoxLayout(apexBox, BoxLayout.Y_AXIS));
//		   apexBox.add(title4);
//		   apexBox.add(apexPane);


	   JSplitPane splitPaneList = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	   splitPaneList.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	   splitPaneList.setDividerLocation(300);
	   splitPaneList.setTopComponent(segmentBox);
	   splitPaneList.setBottomComponent(apexBox);

	   JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	   splitPane.setLeftComponent(splitPaneList);
	   splitPane.setRightComponent(ppo2);


	   Dimension minimumSize = new Dimension(100, 50);
	   infoView.setMinimumSize(minimumSize);
	   splitPane.setDividerLocation(300);
	   splitPane.setPreferredSize(new Dimension(500, 300));

	   JButton ref = new JButton("Refresh");
	   ref.setActionCommand("ROOT_REFRESH");
	   ref.addActionListener(this);

      
      cartoAnalysisButton = new JButton("Run analysis");
      cartoAnalysisButton.setActionCommand("RUN_ANALYSIS_APEX");
      cartoAnalysisButton.addActionListener(this);
      
      cartoNextButton = new JButton("Next Image");
      cartoNextButton.setActionCommand("RUN_NEXT_APEX");
      cartoNextButton.addActionListener(this);
      cartoNextButton.setEnabled(false);
      
      
      JPanel buttonsRight = new JPanel(new BorderLayout());
      buttonsRight.setLayout(new BoxLayout(buttonsRight, BoxLayout.X_AXIS));
      buttonsRight.add(cartoNextButton);
      buttonsRight.add(cartoAnalysisButton);
      

      JPanel buttonsLEFT = new JPanel();
      buttonsLEFT.setLayout(new BoxLayout(buttonsLEFT,BoxLayout.X_AXIS));
      buttonsLEFT.add(ref);

      JPanel buttonPanel = new JPanel(new BorderLayout());
      buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      buttonPanel.add(buttonsRight, BorderLayout.EAST);


      JPanel p4 = new JPanel(new BorderLayout());
      p4.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
      p4.add(buttonsLEFT, BorderLayout.WEST);
      p4.add(buttonsRight, BorderLayout.EAST);

      JPanel p1 = new JPanel(new BorderLayout());
      p1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      p1.add(splitPane, BorderLayout.CENTER);
      p1.add(p4, BorderLayout.SOUTH);

      return p1;
   }


	/**
	 * Create the option panel for the cartographer tab.
	 * @return
	 */
	private JScrollPane getApexOptionPanel(){

		// Image panel
	      cartoFolderButton = new JButton("Choose folder");
	      cartoFolderButton.setActionCommand("IMAGE_FOLDER_APEX");
	      cartoFolderButton.addActionListener(this);
//	      cartoImageFolder = new JTextField("[Choose a folder]",20);
	      cartoImageFolder = new JTextField("/Users/guillaumelobet/Desktop/test-gloroot/dual_color/zat12_salt/",20);

	      JPanel subpanel1 = new JPanel();
	      subpanel1.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      GridBagConstraints gbc1 = new GridBagConstraints();
	      gbc1.anchor = GridBagConstraints.WEST;
	      GridBagLayout gbl1 = new GridBagLayout();
	      subpanel1.setLayout(gbl1);
	      gbc1.gridx = 0;
	      gbc1.gridy = 0;
	      subpanel1.add(cartoImageFolder, gbc1);	
	      gbc1.gridx = 1;
	      subpanel1.add(cartoFolderButton, gbc1);
	      
	      JPanel panel1 = new JPanel(new BorderLayout());
	      panel1.setBorder(BorderFactory.createLineBorder(Color.gray));
	      panel1.add(subpanel1, BorderLayout.WEST);

	      JLabel title1 = new JLabel("Images source");
	      Font f1 = title1.getFont();
	      title1.setFont(f1.deriveFont(f1.getStyle() ^ Font.BOLD));

	      JPanel imageBox = new JPanel(new BorderLayout());
	      imageBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      imageBox.add(title1, BorderLayout.NORTH);
	      imageBox.add(panel1, BorderLayout.SOUTH);

		// Parameter panel		
	      cartoScalePix = new JTextField("2020", 5);	      
	      cartoScaleCm = new JTextField("23.5", 5);
	      cartoNTimeSerie = new JTextField("5", 5);
	      cartoUseRSML = new JCheckBox("Use existing datafile (if any)", false);
	      cartoUseAllSegments = new JCheckBox("Export unattached segments", true);
	      cartoOnlySeg = new JCheckBox("Structure images only", false);
	      cartoSoil = new JCheckBox("Folder contains soil images", false);
	      cartoConnectReporters = new JCheckBox("Connect Reporters to:", true);
	      cartoLowContrast = new JCheckBox("Low contrast image:", false);
	      
	      String[] nameType = {"Previous image", "Current image"};
	      cartoConnection = new JComboBox(nameType);

	      JPanel subpanel2 = new JPanel();
	      subpanel2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      GridBagConstraints gbc2 = new GridBagConstraints();
	      gbc2.anchor = GridBagConstraints.WEST;
	      GridBagLayout gbl2 = new GridBagLayout();
	      subpanel2.setLayout(gbl2);

	      JPanel panel2 = new JPanel(new BorderLayout());
	      panel2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      panel2.add(subpanel2, BorderLayout.WEST);

	      gbc2.gridy = 0;
	      gbc2.gridx = 0;
	      subpanel2.add(new JLabel(" Cm : "), gbc2);
	      gbc2.gridx = 1;
	      subpanel2.add(new JLabel(" Pixels : "), gbc2);
	      gbc2.gridy = 1;
	      gbc2.gridx = 0;
	      subpanel2.add(cartoScaleCm, gbc2);
	      gbc2.gridx = 1;
	      subpanel2.add(cartoScalePix, gbc2);
	      gbc2.gridx = 2;

	      subpanel2.add(cartoUseRSML, gbc2);
	      gbc2.gridy = 2;
	      gbc2.gridx = 2;
	      subpanel2.add(cartoUseAllSegments, gbc2);
	      gbc2.gridy = 3;
	      gbc2.gridx = 2;
	      subpanel2.add(cartoOnlySeg, gbc2);
	      gbc2.gridy = 4;
	      gbc2.gridx = 2;
	      subpanel2.add(cartoSoil, gbc2);
	      gbc2.gridy = 5;
	      gbc2.gridx = 0;
	      subpanel2.add(new JLabel("Time points"), gbc2);
	      gbc2.gridx = 1;
	      subpanel2.add(cartoNTimeSerie, gbc2);

	      
	      
	      JPanel subpanel3 = new JPanel();
	      subpanel3.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      GridBagConstraints gbc3 = new GridBagConstraints();
	      gbc3.anchor = GridBagConstraints.WEST;
	      GridBagLayout gbl3 = new GridBagLayout();
	      subpanel3.setLayout(gbl3);
	      
	      JPanel panel3 = new JPanel(new BorderLayout());
	      panel3.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      panel3.add(subpanel3, BorderLayout.WEST);
	      
	      gbc3.gridy = 0;
	      gbc3.gridx = 0;
	      subpanel3.add(cartoConnectReporters, gbc3);
	      gbc3.gridx = 1;
	      subpanel3.add(cartoConnection, gbc3);
	      gbc3.gridy = 1;
	      gbc3.gridx = 0;
	      subpanel3.add(cartoLowContrast, gbc3);
	      
	      JPanel paramSubbox = new JPanel(new BorderLayout());
	      paramSubbox.setBorder(BorderFactory.createLineBorder(Color.gray));
	      paramSubbox.add(panel2, BorderLayout.NORTH);
	      paramSubbox.add(panel3, BorderLayout.SOUTH);
	      
	      
	      JLabel title2 = new JLabel("Analysis options");
	      title2.setFont(f1.deriveFont(f1.getStyle() ^ Font.BOLD));

	      JPanel paramBox = new JPanel(new BorderLayout());
	      paramBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      paramBox.add(title2, BorderLayout.NORTH);
	      paramBox.add(paramSubbox, BorderLayout.SOUTH);


		// Output panel

//	      cartoCSVFile = new JTextField("[Choose file]", 20);
	      cartoCSVFile = new JTextField("/Users/guillaumelobet/Desktop/test.csv/",20);	     
	      
	      cartoCSVButton = new JButton("Choose folder");
	      cartoCSVButton.setActionCommand("CSV_FOLDER_APEX");
	      cartoCSVButton.addActionListener(this);
	      
	      JPanel subpanel4 = new JPanel();
	      subpanel4.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      GridBagConstraints gbc4 = new GridBagConstraints();
	      gbc4.anchor = GridBagConstraints.WEST;
	      GridBagLayout gbl4 = new GridBagLayout();
	      subpanel4.setLayout(gbl4);

	      JPanel panel4 = new JPanel(new BorderLayout());
	      panel4.setBorder(BorderFactory.createLineBorder(Color.gray));
	      panel4.add(subpanel4, BorderLayout.WEST);

	      gbc4.gridy = 0;
	      gbc4.gridx = 0; 
	      subpanel4.add(cartoCSVFile, gbc4); 
	      gbc4.gridx = 1;
	      subpanel4.add(cartoCSVButton, gbc4); 	      

	      JLabel title3 = new JLabel("Output file");
	      title3.setFont(f1.deriveFont(f1.getStyle() ^ Font.BOLD));

	      JPanel outputBox = new JPanel(new BorderLayout());
	      outputBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	      outputBox.add(title3, BorderLayout.NORTH);
	      outputBox.add(panel4, BorderLayout.SOUTH);


     // Button panel


	      // All panels

	      JPanel allBoxes = new JPanel(new BorderLayout());
	      allBoxes.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	      allBoxes.setLayout(new BoxLayout(allBoxes, BoxLayout.Y_AXIS));
	      allBoxes.add(imageBox);
	      allBoxes.add(paramBox);
	      allBoxes.add(outputBox);

//	      JPanel panel = new JPanel(new BorderLayout());
//	      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
//	      panel.add(allBoxes, BorderLayout.NORTH);

	      return new JScrollPane(allBoxes);
	}





   /**
    * Create a Tree list to display all the roots traced in the image
    * @author guillaumelobet
    */
   public class SegmentListTree extends JPanel implements TreeSelectionListener {

	   /**
	    *
	    */
	   private static final long serialVersionUID = 1L;
		public JTree tree;
	    private Object pSeg = null;
	    boolean attach = false;
	    TreePath lastSelectedPath = null;

	    /**
	     *
	     */
	    public SegmentListTree() {

	        super(new GridLayout(1,0));
	        tree = new JTree();
	        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	        tree.addTreeSelectionListener(this);
	        tree.setEditable(false);
	        tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	        tree.setDragEnabled(false);

	        // New icons for the tree
            tree.setCellRenderer(new MyRenderer());

	        JScrollPane treeView = new JScrollPane(tree);
	        add(treeView);
	    }

	    private class MyRenderer extends DefaultTreeCellRenderer {

			private static final long serialVersionUID = 1L;
			ImageIcon primIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/primary.gif")));
	        ImageIcon primOpenIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/primary_open.gif")));
	        ImageIcon markIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/mark.gif")));
	        ImageIcon rootIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/images/root_icon.gif")));

	        public MyRenderer() {}

	        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
	                            boolean leaf, int row, boolean hasFocus) {

	            super.getTreeCellRendererComponent( tree, value, sel, expanded, leaf, row, hasFocus);

	            if (isChild(value, row) == 0 && !expanded) setIcon(primIcon);
	            else if (isChild(value, row) == 0 && expanded) setIcon(primOpenIcon);
	            else if (isChild(value, row) == 1 && !expanded) setIcon(markIcon);
	            else if (isChild(value, row) == 50) setIcon(rootIcon);
	            return this;
	        }

	        protected int isChild(Object value, int row) {
	        	try{
		        	if(row == 0) return 50;
		        	else {
		        		if(value instanceof Segment) return 0;
		        		else return 1;
		        	}
	        	}
	            catch(Exception ex){return 40;}
	        }
	    }

	    public void valueChanged(TreeSelectionEvent e) {

            try{rs.selectObject(pSeg, false);}
            catch(Exception ex){}

            if(tree.getSelectionCount() > 0){
	            if(tree.isRowSelected(0)){
	            	infoPane.setText(rs.displayInfo());
	            }
	            else{
			    	TreePath t = tree.getSelectionPath();
			    	lastSelectedPath = t;
			   		pSeg = t.getLastPathComponent();
			   		rs.selectObject(pSeg, true);
		    		infoPane.setText(rs.displayObjectInfo(pSeg));
	            }
            }
            else infoPane.setText("Please select an object");

            try{rs.repaint();}
            catch(NullPointerException npe){;}
	    }

	    public void refreshNodes() {
	    	segmentListTree.tree.removeAll();
	    	segmentListTree.tree.setModel(null);
	    	segmentListTree.tree.setModel(rs);
	    	segmentListTree.tree.setSelectionPath(lastSelectedPath);
	    }
	}

   
   /**
    * Table showing the different marks of a root
    * @author guillaume
    *
    */
   class ReporterTableModel extends AbstractTableModel implements TableModelListener{
		private static final long serialVersionUID = 1L;
		ArrayList<Reporter> repList = new ArrayList<Reporter>();

	      public ReporterTableModel() {
	      }		      
	      

	      public void modelChanged(RootSystem rs) {
	         repList.clear();
	         if(rs != null){
	        	 int j = 0;
		         for(int i = 0 ; i < rs.repList.size(); i++){
		        	 repList.add(j, rs.repList.get(i));
		        	 j++;
		         }
	         }
	         fireTableStructureChanged();
	         }

	      public Object getValueAt(int row, int col) {
	         Reporter m = (Reporter) repList.get(row);
	         return m.toString();
	      }
	      
	      public Reporter getSelectedReporter(int row){
	    	  if(repList.size() > 0) return (Reporter) repList.get(row);

	    	  else return null;
	      }

	      public boolean isCellEditable(int row, int col) {return false;}

	      public void setValueAt(Object aValue, int row, int col) {
	      }
	      
	      public int getRowCount() {return repList.size();}
	         
	      public int getColumnCount() {return 1;}

	      @SuppressWarnings({ "unchecked", "rawtypes" })
		public Class getColumnClass(int col) {
	    	  if (col == 0) return  String.class;
	    	  else return Float.class;
	    	  }

	      public String getColumnName(int col) {
		        	 return "Reporter";
	      }

		public void tableChanged(TableModelEvent arg0) {
	    	  IJ.log("SETVALUE");

		}
	}


   /**
    * Attach the current RootModel to SRWin
    * @param rm
    * @param usePrefs
    */
   	public void setCurrentRootModel(RootSystem rs) {

	   if (rs == this.rs){
          segmentListTree.tree.setModel(rs);
          segmentListTree.tree.expandRow(0);
          repTableModel.modelChanged(rs);          

          rs.log("Current root model updated");
    	  return;
      }

      this.rs = rs;
      repTableModel.modelChanged(rs);          


      }


	/**
	 *
	 */
	public void stateChanged(ChangeEvent e) {

	}
	/**
	 *
	 * @param e
	 */
	public void itemStateChanged(ItemEvent e) {
	      Object item = e.getItem();
	      
	      // EFD analysis needs the global analysis to be run
	      if (item == rootEFD){
	    	  if(rootEFD.isSelected() && !rootGlobal.isSelected()) rootGlobal.setSelected(true);
	    	  rootGlobal.setEnabled(!rootEFD.isSelected());
	      }
	}



	/**
	 *
	 */
	public void actionPerformed(ActionEvent ae) {
		String sep = System.getProperty("file.separator");


		if(ae.getActionCommand() == "ROOT_REFRESH"){
			segmentListTree.refreshNodes();	   
			repTableModel.modelChanged(rs);
		} 
		
//---------------------------------------------------------------------------------------------------		
// CARTOGRAPHER ANALYSIS	
		
		else if (ae.getActionCommand() == "CSV_FOLDER_APEX") { 
	    	  
			JFileChooser fc = new JFileChooser();
			csvFilter csvf = new csvFilter ();
			fc.setFileFilter(csvf);
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int returnVal = fc.showDialog(Interface.this, "Save");

			if (returnVal == JFileChooser.APPROVE_OPTION){
				String fName = fc.getSelectedFile().toString();
				if(!fName.endsWith(".csv")) fName = fName.concat(".csv");
				cartoCSVFile.setText(fName);
			}
			else IJ.log("Open command cancelled.");
		}

		else if (ae.getActionCommand() == "IMAGE_FOLDER_APEX") {

			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = fc.showDialog(Interface.this, "Choose folder");

			if (returnVal == JFileChooser.APPROVE_OPTION){
				String fName = fc.getSelectedFile().toString();
				
				// If the name contains 2 times the same folder at the end, remove one.
				// This is necessary because of a strange behavior of the JFileChooser.
				ArrayList<String> na = Util.getArrayFromString(fName, sep, true);
				if(na.get(na.size()-1).equals(na.get(na.size()-2))){
					na.remove(na.size()-1);
				}
				fName = Util.getStringFromArray(na, sep);
				if(!fName.endsWith(sep)) fName = fName+sep;
				
				cartoImageFolder.setText(fName);			
				cartoCSVFile.setText(fName+"rootreporter-data.csv");
				File[] images = new File(fName).listFiles();
				for(int i = 0; i< images.length; i++) if(images[i].isHidden()) images[i].delete();
				images = new File(fName).listFiles(new FilenameFilter() {
				    public boolean accept(File dir, String name) {
				        return name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".tiff") || 
				        		name.toLowerCase().endsWith(".tif") || name.toLowerCase().endsWith(".jpeg") ||
				        		 name.toLowerCase().endsWith(".png");
				    }
				});
				cartoNTimeSerie.setText(""+images.length/2);				
			}
			else IJ.log("Folder command cancelled.");
		}

		else if(ae.getActionCommand() == "RUN_ANALYSIS_APEX"){
			if (cartoImageFolder.getText().equals("")){
				IJ.log("Please choose an image folder");
				return;
			}
			if(cartoCSVFile.getText().equals("")){
				IJ.log("Please choose a csv folder");
				return;
			}
			cartoNextButton.setEnabled(true);
			cartoAnalysisButton.setEnabled(false);
			glora = new RootCartographer(new File(cartoImageFolder.getText()), 
							cartoCSVFile.getText(), 
		 					Float.valueOf(cartoScalePix.getText()), 
		 					Float.valueOf(cartoScaleCm.getText()),
		 					cartoUseRSML.isSelected(),
		 					cartoUseAllSegments.isSelected(),
		 					cartoOnlySeg.isSelected(),
		 					cartoSoil.isSelected(),
		 					Integer.valueOf(cartoNTimeSerie.getText()),
		 					cartoConnectReporters.isSelected(),
							cartoConnection.getSelectedIndex(),
		 					cartoLowContrast.isSelected(),
		 					instance);

		}
		else if(ae.getActionCommand() == "RUN_NEXT_APEX"){
			if(!glora.analyze()){
				rs.log("---------------------");
				rs.log("All images processed.");
				cartoNextButton.setEnabled(false);
				cartoAnalysisButton.setEnabled(true);
				glora.pwReporter.flush();
				glora.pwSeg.flush();
			}
		}


//---------------------------------------------------------------------------------------------------
// ROOT ANALYSIS
		else if (ae.getActionCommand() == "CSV_FOLDER_ROOT") {

			JFileChooser fc = new JFileChooser();
			csvFilter csvf = new csvFilter ();
			fc.setFileFilter(csvf);
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int returnVal = fc.showDialog(this, "Save");

			if (returnVal == JFileChooser.APPROVE_OPTION){
				String fName = fc.getSelectedFile().toString();
				if(!fName.endsWith(".csv")) fName = fName.concat(".csv");
				rootCSVFile.setText(fName);
			}
			else IJ.log("Open command cancelled.");
		}

		else if (ae.getActionCommand() == "IMAGE_FOLDER_ROOT") {

			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = fc.showDialog(this, "Choose folder");

			if (returnVal == JFileChooser.APPROVE_OPTION){
				String fName = fc.getSelectedFile().toString();

				// If the name contains 2 times the same folder at the end, remove one.
				// This is necessary because of a strange behavior of the JFileChooser.
				ArrayList<String> na = Util.getArrayFromString(fName, sep, true);
				if(na.get(na.size()-1).equals(na.get(na.size()-2))){
					na.remove(na.size()-1);
				}
				fName = Util.getStringFromArray(na, sep);
				if(!fName.endsWith(sep)) fName = fName + sep;
				
				rootImageFolder.setText(fName);
				rootCSVFile.setText(fName+"root-data.csv");
				File f = new File(fName);
				File[] images = f.listFiles();
				for(int i = 0; i< images.length; i++) if(images[i].isHidden()) images[i].delete();
				images = f.listFiles(new FilenameFilter() {
				    public boolean accept(File dir, String name) {
				        return name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".tiff") ||
				        		name.toLowerCase().endsWith(".tif") || name.toLowerCase().endsWith(".jpeg") ||
				        		 name.toLowerCase().endsWith(".png");
				    }
				});

			}
			else IJ.log("Folder command cancelled.");
		}

		else if(ae.getActionCommand() == "RUN_ANALYSIS_root"){
			if (rootImageFolder.getText().equals("")){
				IJ.log("Please choose an image folder");
				return;
			}
			if(rootCSVFile.getText().equals("")){
				IJ.log("Please choose a csv folder");
				return;
			}
			Thread ra = new Thread(new Runnable() {
		 		public void run() {
					new RootAnalysis(new File(rootImageFolder.getText()),
							rootCSVFile.getText(),
							Float.valueOf(rootScalePix.getText()),
							Float.valueOf(rootScaleCm.getText()),
							directionalityMethod.getSelectedIndex(),
							blackRoots.isSelected(),
							Float.valueOf(rootMinSize.getText()),
							rootLocal.isSelected(),
							rootGlobal.isSelected(),
							rootEFD.isSelected(),
							rootDirectional.isSelected(),
							Integer.valueOf(nEFD.getText()),
							Integer.valueOf(nBinDir.getText()),
							manualCorrection.isSelected(),
							Integer.valueOf(nWBin.getText()),
							Integer.valueOf(nHBin.getText()),
							timeSerie.isSelected(),
							rootLowContrast.isSelected(),
							rootWide.isSelected()
							);
				}
			});
			ra.start();
		}

	}

   /**
    * CSV filter
    * @author guillaumelobet
    */
	private class csvFilter extends javax.swing.filechooser.FileFilter{
		public boolean accept (File f) {
			if (f.isDirectory()) {
				return true;
			}

			String extension = getExtension(f);
			if (extension != null) {
				if (extension.equals("csv")) return true;
				else return false;
			}
			return false;
		}

		public String getDescription () {
			return "Comma-separated values file (*.csv)";
		}

		public String getExtension(File f) {
			String ext = null;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if (i > 0 &&  i < s.length() - 1) {
				ext = s.substring(i+1).toLowerCase();
			}
			return ext;
		}
	}


	/**
	 * Display the about text
	 * @return
	 */
	public String displayAboutRootText(){

		String text = "\n Elliptic Fourrier Descriptor analysis is based on a plugin developped by \n"
				+ "Thomas Boudier and Ben Tupper"
				+ "\n--\n"
				+ "Directionality analysis is based on a plugin developped by \n"
				+ " Jean-Yves Tinevez\n";
		return text;
	}

	/**
	 * Display the about text
	 * @return
	 */
	public String displayDisclaimerText(){

		String text = "\nThis plugin is provided 'as is' and 'with all faults'. We makes no representations or warranties \n"
				+ " of any kind concerning the safety, suitability, lack of viruses, inaccuracies, typographical errors, or other\n"
				+ " harmful components of this plugin. There are inherent dangers in the use of any software, and you are solely\n"
				+ " responsible for determining whether this plugin is compatible with your equipment and other software installed\n"
				+ " on your equipment. You are also solely responsible for the protection of your equipment and backup of your data\n"
				+ " and we will not be liable for any damages you may suffer in connection with using, \n"
				+ "modifying, or distributing this plugin.\n";
		return text;
	}

	/**
	 * Display the about text
	 * @return
	 */
	public String displayAboutText(){

		String text = "\n GLO-RIA is a plugin created and maintained by\n-\n"
				+"Guillaume Lobet - Université de Liége\n"
				+ "guillaume.lobet@ulg.ac.be\n"
				+ "@guillaumelobet\n-\n"
				+"Ruben Rellan Alvarez - Dinneny Lab - Carnegie Institute for Science\n"
				+ "rrellan@langebio.cinvestav.mx\n"
				+ "@rrellanalvarez\n\n"
				+ "https://github.com/rr-lab/glo_roots\n\n";
		return text;
	}

}
