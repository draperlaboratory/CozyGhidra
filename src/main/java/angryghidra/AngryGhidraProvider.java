package angryghidra;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.Map.Entry;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import org.json.JSONArray;
import org.json.JSONObject;
import docking.ComponentProvider;
import docking.widgets.textfield.IntegerTextField;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import resources.ResourceManager;

import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.embed.swing.JFXPanel;
import javafx.application.Platform;

public class AngryGhidraProvider extends ComponentProvider {
    public final String htmlString = "<html>Memory<br/>Hint: to store an integer literal enter \"bvv(val, nbits)\" in the value field, for example \"bvv(0xdeadbeef, 32)\"</html>";
    public final String configuringString = "[+] Configuring options";
    private boolean isHookWindowClosed;
    private boolean isTerminated;
    private boolean isWindows;
    private int guiRegNextId;
    private int guiArgNextId;
    private int guiMemNextId;
    private int guiStoreNextId;
    private int guiMallocNextId;
    private int guiSymbolsNextId;
    private String tmpDir;
    private Program focusedProgram;
    private HashSet<Program> availablePrograms = new HashSet<>();
    private LocalColorizingService mColorService;
    private HookHandler hookHandler;
    private AngrProcessing angrProcessing;
    private UserAddressStorage addressStorage;
    private JPanel mainPanel;
    private JPanel customOptionsPanel;
    private JPanel argumentsPanel;
    private JPanel mainOptionsPanel;
    private JPanel statusPanel;
    private JPanel hookLablesPanel;
    private JPanel writeMemoryPanel;
    private JPanel argSetterPanel;
    private JPanel mallocPanel;
    private JPanel symbolsPanel;
    private JScrollPane scrollSolutionTextArea;
    private JScrollPane scrollAvoidAddrsArea;
    private JComboBox<String> projectACombo;
    private JComboBox<String> projectBCombo;
    private JTextField callFunTFA;
    private JTextField callFunTFB;
    private JTextField valueTF;
    private JTextField registerTF;
    private JTextField mallocNameTF;
    private JTextField symbolsNameTF;
    private JTextField firstArgTF;
    private IntegerTextField vectorAddressTF;
    private IntegerTextField vectorLenTF;
    private JTextField memStoreAddrTF;
    private JTextField memStoreValueTF;
    private IntegerTextField mallocSizeTF;
    private IntegerTextField symbolsSizeTF;
    private JCheckBox chckbxAvoidAddresses;
    private JCheckBox chckbxAutoLoadLibs;
    private JCheckBox chckbxArg;
    private JTextArea avoidTextArea;
    private JTextArea solutionTextArea;
    private JLabel statusLabel;
    private JLabel statusLabelFound;
    private JLabel lbStatus;
    private JLabel lbStoreAddr;
    private JLabel lbStoreVal;
    private JLabel lblWriteToMemory;
    private JLabel lbArgContent;
    private JLabel lbMallocName;
    private JLabel lbMallocSize;
    private JLabel lbSymbolsName;
    private JLabel lbSymbolsSize;
    private JButton btnReset;
    private JButton btnRun;
    private JButton btnStop;
    private JButton btnAddWM;
    private JButton btnAddArg;
    private HashMap <IntegerTextField, IntegerTextField> vectors;
    private HashMap <JTextField, JTextField> memStore;
    private HashMap <JTextField, JTextField> presetRegs;
    private HashMap<JTextField, IntegerTextField> mallocedChunks;
    private HashMap<JTextField, IntegerTextField> createdSymbols;
    private HashMap <String[], String[][]> hooks;
    private ArrayList <JTextField> argsTF;
    private ArrayList <JButton> delRegsBtns;
    private ArrayList <JButton> delMemBtns;
    private ArrayList <JButton> delStoreBtns;
    private ArrayList <JButton> delBtnArgs;
    private ArrayList <JButton> delHookBtns;
    private ArrayList <JLabel> lbHooks;
    private ImageIcon deleteIcon;
    private ImageIcon addIcon;

    public AngryGhidraProvider(AngryGhidraPlugin plugin, String owner, Program program) {
        super(plugin.getTool(), owner, owner);
        addressStorage = plugin.getAddressStorage();
        setIcon(ResourceManager.loadImage("images/ico.png"));
        if (program != null){
            setProgram(program);
        }
        initFields();
        buildPanel();
    }

    public void setColorService(LocalColorizingService colorService) {
        mColorService = colorService;
        angrProcessing = new AngrProcessing(addressStorage, mColorService, this, focusedProgram.getAddressFactory());
    }

    private void initFields() {
        ImageIcon addIconNonScaled = new ImageIcon(getClass().getResource("/images/add.png"));
        ImageIcon deleteIconNonScaled = new ImageIcon(getClass().getResource("/images/delete.png"));
        addIcon = new ImageIcon(addIconNonScaled.getImage().getScaledInstance(21, 21,  java.awt.Image.SCALE_SMOOTH));
        deleteIcon = new ImageIcon(deleteIconNonScaled.getImage().getScaledInstance(21, 21,  java.awt.Image.SCALE_SMOOTH));

        setHookWindowState(true);
        setIsTerminated(false);
        guiArgNextId = 2;
        guiMemNextId = 2;
        guiRegNextId = 2;
        guiStoreNextId = 2;
        guiMallocNextId = 2;
        guiSymbolsNextId = 2;
        delRegsBtns = new ArrayList <JButton>();
        delBtnArgs = new ArrayList <JButton>();
        delMemBtns = new ArrayList <JButton>();
        delStoreBtns = new ArrayList <JButton>();
        delHookBtns = new ArrayList <JButton>();
        argsTF = new ArrayList <JTextField>();
        presetRegs = new HashMap<>();
        mallocedChunks = new HashMap<>();
        createdSymbols = new HashMap<>();
        vectors = new HashMap<>();
        memStore = new HashMap<>();
        hooks = new HashMap <String[], String[][]>();
        lbHooks = new ArrayList <JLabel>();
        isWindows = System.getProperty("os.name").contains("Windows");
        tmpDir = System.getProperty("java.io.tmpdir");
        if (!isWindows) {
            tmpDir += "/";
        }
    }

    // Replace buildPanel with this method to see an example of the use of webview
    private void buildPanelWebview() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(0, 1));
        setVisible(true);

        final JFXPanel fxPanel = new JFXPanel();
        mainPanel.add(fxPanel);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                WebView view = new WebView();
                view.getEngine().load("http://localhost:8080/?pre=/pre&post=/post");
                fxPanel.setScene(new Scene(view));
            }
        });
    }

    private void buildPanel() {
        mainPanel = new JPanel();
        mainPanel.setMinimumSize(new Dimension(210, 510));
        setVisible(true);

        // Some preparations
        ImageIcon startIcon = new ImageIcon(getClass().getResource("/images/flag.png"));
        ImageIcon stopIcon = new ImageIcon(getClass().getResource("/images/stop.png"));
        ImageIcon resetIcon = new ImageIcon(getClass().getResource("/images/reset.png"));
        resetIcon = new ImageIcon(resetIcon.getImage().getScaledInstance(18, 18,  java.awt.Image.SCALE_SMOOTH));
        Font sansSerif12 = new Font("SansSerif", Font.PLAIN, 12);
        Font sansSerif13 = new Font("SansSerif", Font.PLAIN, 13);

        TitledBorder borderMPO = BorderFactory.createTitledBorder("Main project options");
        borderMPO.setTitleFont(sansSerif12);

        TitledBorder borderCSO = BorderFactory.createTitledBorder("Custom symbolic options");
        borderCSO.setTitleFont(sansSerif12);

        TitledBorder borderSA = BorderFactory.createTitledBorder("Function arguments");
        borderSA.setTitleFont(sansSerif12);

        TitledBorder borderMalloc = BorderFactory.createTitledBorder("Malloced chunks");
        borderMalloc.setTitleFont(sansSerif12);

        TitledBorder borderSymbolic = BorderFactory.createTitledBorder("Symbolic variables");
        borderSymbolic.setTitleFont(sansSerif12);

        argSetterPanel = new JPanel();
        writeMemoryPanel = new JPanel();
        hookLablesPanel = new JPanel();
        statusPanel = new JPanel();
        statusPanel.setBorder(null);
        argSetterPanel.setBorder(null);
        writeMemoryPanel.setBorder(null);

        mainOptionsPanel = new JPanel();
        mainOptionsPanel.setForeground(new Color(46, 139, 87));
        mainOptionsPanel.setBorder(borderMPO);

        customOptionsPanel = new JPanel();
        customOptionsPanel.setBorder(borderCSO);

        chckbxArg = new JCheckBox("Arguments");
        chckbxArg.setFont(sansSerif12);

        argumentsPanel = new JPanel();
        argumentsPanel.setForeground(new Color(46, 139, 87));
        argumentsPanel.setBorder(borderSA);

        mallocPanel = new JPanel();
        mallocPanel.setForeground(new Color(46, 139, 87));
        mallocPanel.setBorder(borderMalloc);

        symbolsPanel = new JPanel();
        symbolsPanel.setForeground(new Color(46, 139, 87));
        symbolsPanel.setBorder(borderSymbolic);

        // Malloc panel
        GridBagLayout gbl_mallocPanel = new GridBagLayout();
        gbl_mallocPanel.columnWidths = new int[] {
                0,
                0,
                0,
                0,
                0,
                0
        };
        gbl_mallocPanel.rowHeights = new int[] {
                0,
                0,
                0
        };
        gbl_mallocPanel.columnWeights = new double[] {
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                Double.MIN_VALUE
        };
        gbl_mallocPanel.rowWeights = new double[] {
                0.0,
                0.0,
                Double.MIN_VALUE
        };
        mallocPanel.setLayout(gbl_mallocPanel);

        lbMallocName = new JLabel("Name");
        lbMallocName.setFont(sansSerif12);
        GridBagConstraints gbc_lbMallocName = new GridBagConstraints();
        gbc_lbMallocName.weightx = 1.0;
        gbc_lbMallocName.insets = new Insets(0, 0, 0, 5);
        gbc_lbMallocName.gridx = 1;
        gbc_lbMallocName.gridy = 0;
        mallocPanel.add(lbMallocName, gbc_lbMallocName);

        lbMallocSize = new JLabel("Size (bytes)");
        lbMallocSize.setFont(sansSerif12);
        GridBagConstraints gbc_lbMallocSize = new GridBagConstraints();
        gbc_lbMallocSize.weightx = 1.0;
        gbc_lbMallocSize.insets = new Insets(0, 0, 0, 5);
        gbc_lbMallocSize.gridx = 3;
        gbc_lbMallocSize.gridy = 0;
        mallocPanel.add(lbMallocSize, gbc_lbMallocSize);

        JButton btnMallocAddButton = new JButton("");
        GridBagConstraints gbc_btnMallocAddButton = new GridBagConstraints();
        gbc_btnMallocAddButton.anchor = GridBagConstraints.CENTER;
        gbc_btnMallocAddButton.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnMallocAddButton.insets = new Insets(0, 0, 0, 5);
        gbc_btnMallocAddButton.gridx = 0;
        gbc_btnMallocAddButton.gridy = 1;
        gbc_btnMallocAddButton.weighty = 0.1;
        mallocPanel.add(btnMallocAddButton, gbc_btnMallocAddButton);
        btnMallocAddButton.setBorder(null);
        btnMallocAddButton.setContentAreaFilled(false);
        btnMallocAddButton.setIcon(addIcon);

        btnMallocAddButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextField regTF = new JTextField();
                regTF.setColumns(5);
                GridBagConstraints gbc_regTF = new GridBagConstraints();
                gbc_regTF.fill = GridBagConstraints.HORIZONTAL;
                gbc_regTF.anchor = GridBagConstraints.CENTER;
                gbc_regTF.gridx = 1;
                gbc_regTF.insets = new Insets(0, 0, 0, 5);
                gbc_regTF.gridy = guiMallocNextId;
                gbc_regTF.weightx = 1;
                gbc_regTF.weighty = 0.1;
                mallocPanel.add(regTF, gbc_regTF);

                IntegerTextField valTF = new IntegerTextField();
                valTF.setDecimalMode();
                //valTF.setColumns(5);
                GridBagConstraints gbc_valTF = new GridBagConstraints();
                gbc_valTF.fill = GridBagConstraints.HORIZONTAL;
                gbc_valTF.anchor = GridBagConstraints.CENTER;
                gbc_valTF.insets = new Insets(0, 0, 0, 5);
                gbc_valTF.gridx = 3;
                gbc_valTF.gridy = guiMallocNextId;
                gbc_valTF.weightx = 1;
                gbc_valTF.weighty = 0.1;
                mallocPanel.add(valTF.getComponent(), gbc_valTF);
                mallocedChunks.put(regTF, valTF);

                JButton btnDel = new JButton("");
                btnDel.setBorder(null);
                btnDel.setContentAreaFilled(false);
                btnDel.setIcon(deleteIcon);
                GridBagConstraints gbc_btnDel = new GridBagConstraints();
                gbc_btnDel.insets = new Insets(0, 0, 0, 5);
                gbc_btnDel.fill = GridBagConstraints.HORIZONTAL;
                gbc_btnDel.anchor = GridBagConstraints.CENTER;
                gbc_btnDel.gridx = 0;
                gbc_btnDel.gridy = guiMallocNextId++;
                gbc_btnDel.weighty = 0.1;
                mallocPanel.add(btnDel, gbc_btnDel);
                delRegsBtns.add(btnDel);
                btnDel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        guiMallocNextId--;
                        delRegsBtns.remove(btnDel);
                        mallocedChunks.remove(regTF, valTF);
                        mallocPanel.remove(regTF);
                        mallocPanel.remove(valTF.getComponent());
                        mallocPanel.remove(btnDel);
                        mallocPanel.repaint();
                        mallocPanel.revalidate();
                    }
                });
                mallocPanel.repaint();
                mallocPanel.revalidate();
            }
        });

        mallocNameTF = new JTextField();
        mallocNameTF.setColumns(5);
        GridBagConstraints gbc_mallocNameTF = new GridBagConstraints();
        gbc_mallocNameTF.insets = new Insets(0, 0, 0, 5);
        gbc_mallocNameTF.anchor = GridBagConstraints.CENTER;
        gbc_mallocNameTF.fill = GridBagConstraints.HORIZONTAL;
        gbc_mallocNameTF.gridx = 1;
        gbc_mallocNameTF.gridy = 1;
        gbc_mallocNameTF.weightx = 1;
        gbc_mallocNameTF.weighty = 0.1;
        mallocPanel.add(mallocNameTF, gbc_mallocNameTF);

        mallocSizeTF = new IntegerTextField();
        mallocSizeTF.setDecimalMode();
        GridBagConstraints gbc_MallocSizeTF = new GridBagConstraints();
        gbc_MallocSizeTF.insets = new Insets(0, 0, 0, 5);
        gbc_MallocSizeTF.fill = GridBagConstraints.HORIZONTAL;
        gbc_MallocSizeTF.anchor = GridBagConstraints.CENTER;
        gbc_MallocSizeTF.gridx = 3;
        gbc_MallocSizeTF.gridy = 1;
        gbc_MallocSizeTF.weightx = 1;
        gbc_MallocSizeTF.weighty = 0.1;
        mallocPanel.add(mallocSizeTF.getComponent(), gbc_MallocSizeTF);

        mallocedChunks.put(mallocNameTF, mallocSizeTF);

        // End malloc panel

        // Symbols panel
        GridBagLayout gbl_symbolsPanel = new GridBagLayout();
        gbl_symbolsPanel.columnWidths = new int[] {
                0,
                0,
                0,
                0,
                0,
                0
        };
        gbl_symbolsPanel.rowHeights = new int[] {
                0,
                0,
                0
        };
        gbl_symbolsPanel.columnWeights = new double[] {
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                Double.MIN_VALUE
        };
        gbl_symbolsPanel.rowWeights = new double[] {
                0.0,
                0.0,
                Double.MIN_VALUE
        };
        symbolsPanel.setLayout(gbl_symbolsPanel);

        lbSymbolsName = new JLabel("Name");
        lbSymbolsName.setFont(sansSerif12);
        GridBagConstraints gbc_lbSymbolsName = new GridBagConstraints();
        gbc_lbSymbolsName.weightx = 1.0;
        gbc_lbSymbolsName.insets = new Insets(0, 0, 0, 5);
        gbc_lbSymbolsName.gridx = 1;
        gbc_lbSymbolsName.gridy = 0;
        symbolsPanel.add(lbSymbolsName, gbc_lbSymbolsName);

        lbSymbolsSize = new JLabel("Size (bits)");
        lbSymbolsSize.setFont(sansSerif12);
        GridBagConstraints gbc_lbSymbolsSize = new GridBagConstraints();
        gbc_lbSymbolsSize.weightx = 1.0;
        gbc_lbSymbolsSize.insets = new Insets(0, 0, 0, 5);
        gbc_lbSymbolsSize.gridx = 3;
        gbc_lbSymbolsSize.gridy = 0;
        symbolsPanel.add(lbSymbolsSize, gbc_lbSymbolsSize);

        JButton btnSymbolsAddButton = new JButton("");
        GridBagConstraints gbc_btnSymbolsAddButton = new GridBagConstraints();
        gbc_btnSymbolsAddButton.anchor = GridBagConstraints.CENTER;
        gbc_btnSymbolsAddButton.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnSymbolsAddButton.insets = new Insets(0, 0, 0, 5);
        gbc_btnSymbolsAddButton.gridx = 0;
        gbc_btnSymbolsAddButton.gridy = 1;
        gbc_btnSymbolsAddButton.weighty = 0.1;
        symbolsPanel.add(btnSymbolsAddButton, gbc_btnSymbolsAddButton);
        btnSymbolsAddButton.setBorder(null);
        btnSymbolsAddButton.setContentAreaFilled(false);
        btnSymbolsAddButton.setIcon(addIcon);

        btnSymbolsAddButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextField nameTF = new JTextField();
                nameTF.setColumns(5);
                GridBagConstraints gbc_regTF = new GridBagConstraints();
                gbc_regTF.fill = GridBagConstraints.HORIZONTAL;
                gbc_regTF.anchor = GridBagConstraints.CENTER;
                gbc_regTF.gridx = 1;
                gbc_regTF.insets = new Insets(0, 0, 0, 5);
                gbc_regTF.gridy = guiSymbolsNextId;
                gbc_regTF.weightx = 1;
                gbc_regTF.weighty = 0.1;
                symbolsPanel.add(nameTF, gbc_regTF);

                IntegerTextField valTF = new IntegerTextField();
                valTF.setDecimalMode();
                //valTF.setColumns(5);
                GridBagConstraints gbc_valTF = new GridBagConstraints();
                gbc_valTF.fill = GridBagConstraints.HORIZONTAL;
                gbc_valTF.anchor = GridBagConstraints.CENTER;
                gbc_valTF.insets = new Insets(0, 0, 0, 5);
                gbc_valTF.gridx = 3;
                gbc_valTF.gridy = guiSymbolsNextId;
                gbc_valTF.weightx = 1;
                gbc_valTF.weighty = 0.1;
                symbolsPanel.add(valTF.getComponent(), gbc_valTF);
                createdSymbols.put(nameTF, valTF);

                JButton btnDel = new JButton("");
                btnDel.setBorder(null);
                btnDel.setContentAreaFilled(false);
                btnDel.setIcon(deleteIcon);
                GridBagConstraints gbc_btnDel = new GridBagConstraints();
                gbc_btnDel.insets = new Insets(0, 0, 0, 5);
                gbc_btnDel.fill = GridBagConstraints.HORIZONTAL;
                gbc_btnDel.anchor = GridBagConstraints.CENTER;
                gbc_btnDel.gridx = 0;
                gbc_btnDel.gridy = guiSymbolsNextId++;
                gbc_btnDel.weighty = 0.1;
                symbolsPanel.add(btnDel, gbc_btnDel);
                delRegsBtns.add(btnDel);
                btnDel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        guiSymbolsNextId--;
                        delRegsBtns.remove(btnDel);
                        createdSymbols.remove(nameTF, valTF);
                        symbolsPanel.remove(nameTF);
                        symbolsPanel.remove(valTF.getComponent());
                        symbolsPanel.remove(btnDel);
                        symbolsPanel.repaint();
                        symbolsPanel.revalidate();
                    }
                });
                symbolsPanel.repaint();
                symbolsPanel.revalidate();
            }
        });

        symbolsNameTF = new JTextField();
        symbolsNameTF.setColumns(5);
        GridBagConstraints gbc_symbolsNameTF = new GridBagConstraints();
        gbc_symbolsNameTF.insets = new Insets(0, 0, 0, 5);
        gbc_symbolsNameTF.anchor = GridBagConstraints.CENTER;
        gbc_symbolsNameTF.fill = GridBagConstraints.HORIZONTAL;
        gbc_symbolsNameTF.gridx = 1;
        gbc_symbolsNameTF.gridy = 1;
        gbc_symbolsNameTF.weightx = 1;
        gbc_symbolsNameTF.weighty = 0.1;
        symbolsPanel.add(symbolsNameTF, gbc_symbolsNameTF);

        symbolsSizeTF = new IntegerTextField();
        symbolsSizeTF.setDecimalMode();
        GridBagConstraints gbc_SymbolsSizeTF = new GridBagConstraints();
        gbc_SymbolsSizeTF.insets = new Insets(0, 0, 0, 5);
        gbc_SymbolsSizeTF.fill = GridBagConstraints.HORIZONTAL;
        gbc_SymbolsSizeTF.anchor = GridBagConstraints.CENTER;
        gbc_SymbolsSizeTF.gridx = 3;
        gbc_SymbolsSizeTF.gridy = 1;
        gbc_SymbolsSizeTF.weightx = 1;
        gbc_SymbolsSizeTF.weighty = 0.1;
        symbolsPanel.add(symbolsSizeTF.getComponent(), gbc_SymbolsSizeTF);

        createdSymbols.put(symbolsNameTF, symbolsSizeTF);

        // End symbols panel

        GroupLayout gl_argumentsPanel = new GroupLayout(argumentsPanel);
        gl_argumentsPanel.setHorizontalGroup(
            gl_argumentsPanel.createParallelGroup(Alignment.TRAILING)
            .addGroup(gl_argumentsPanel.createSequentialGroup()
                .addContainerGap()
                .addComponent(chckbxArg, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                .addGap(31)
                .addComponent(argSetterPanel, GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)
                .addContainerGap())
        );
        gl_argumentsPanel.setVerticalGroup(
            gl_argumentsPanel.createParallelGroup(Alignment.LEADING)
            .addGroup(gl_argumentsPanel.createSequentialGroup()
                .addGroup(gl_argumentsPanel.createParallelGroup(Alignment.LEADING)
                    .addGroup(gl_argumentsPanel.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(chckbxArg))
                    .addComponent(argSetterPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addGap(20))
        );
        argumentsPanel.setLayout(gl_argumentsPanel);

        GridBagLayout gbl_argSetterPanel = new GridBagLayout();
        gbl_argSetterPanel.columnWidths = new int[] {
            0,
            0,
            0,
            0,
            0
        };
        gbl_argSetterPanel.rowHeights = new int[] {
            0,
            0
        };
        gbl_argSetterPanel.columnWeights = new double[] {
            0.0,
            0.0,
            0.0,
            0.0,
            Double.MIN_VALUE
        };
        gbl_argSetterPanel.rowWeights = new double[] {
            0.0,
            0.0
        };
        argSetterPanel.setLayout(gbl_argSetterPanel);

        btnAddArg = new JButton("");
        GridBagConstraints gbc_btnAddArg = new GridBagConstraints();
        gbc_btnAddArg.anchor = GridBagConstraints.CENTER;
        gbc_btnAddArg.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnAddArg.insets = new Insets(0, 0, 0, 5);
        gbc_btnAddArg.gridx = 0;
        gbc_btnAddArg.gridy = 1;
        gbc_btnAddArg.weighty = 0.1;
        argSetterPanel.add(btnAddArg, gbc_btnAddArg);
        btnAddArg.setContentAreaFilled(false);
        btnAddArg.setIcon(addIcon);
        btnAddArg.setBorder(null);
        btnAddArg.setVisible(false);

        lbArgContent = new JLabel("Content");
        GridBagConstraints gbc_lbArgLen = new GridBagConstraints();
        gbc_lbArgLen.insets = new Insets(0, 0, 0, 5);
        gbc_lbArgLen.anchor = GridBagConstraints.CENTER;
        gbc_lbArgLen.gridwidth = 3;
        gbc_lbArgLen.gridx = 1;
        gbc_lbArgLen.gridy = 0;
        gbc_lbArgLen.weightx = 1;
        argSetterPanel.add(lbArgContent, gbc_lbArgLen);
        lbArgContent.setFont(sansSerif12);
        lbArgContent.setVisible(false);

        firstArgTF = new JTextField();
        GridBagConstraints gbc_TFArglen = new GridBagConstraints();
        gbc_TFArglen.insets = new Insets(0, 0, 0, 5);
        gbc_TFArglen.fill = GridBagConstraints.HORIZONTAL;
        gbc_TFArglen.anchor = GridBagConstraints.NORTH;
        gbc_TFArglen.gridwidth = 3;
        gbc_TFArglen.gridx = 1;
        gbc_TFArglen.gridy = 1;
        gbc_TFArglen.weightx = 1;
        gbc_TFArglen.weighty = 0.1;
        argSetterPanel.add(firstArgTF, gbc_TFArglen);
        firstArgTF.setVisible(false);
        chckbxArg.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (chckbxArg.isSelected()) {
                        firstArgTF.setVisible(true);
                        lbArgContent.setVisible(true);
                        btnAddArg.setVisible(true);
                        for (JButton btnDel: delBtnArgs) {
                            btnDel.setVisible(true);
                        }
                        for (JTextField argTF: argsTF) {
                            argTF.setVisible(true);
                        }
                    } else {
                        firstArgTF.setVisible(false);
                        lbArgContent.setVisible(false);
                        btnAddArg.setVisible(false);
                        for (JButton btnDel: delBtnArgs) {
                            btnDel.setVisible(false);
                        }
                        for (JTextField argTF: argsTF) {
                            argTF.setVisible(false);
                        }
                    }
                }
            }
        );

        btnAddArg.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextField argTF = new JTextField();
                GridBagConstraints gbc_TFArg = new GridBagConstraints();
                gbc_TFArg.fill = GridBagConstraints.HORIZONTAL;
                gbc_TFArg.anchor = GridBagConstraints.CENTER;
                gbc_TFArg.gridwidth = 3;
                gbc_TFArg.gridx = 1;
                gbc_TFArg.insets = new Insets(0, 0, 0, 5);
                gbc_TFArg.gridy = guiArgNextId;
                gbc_TFArg.weightx = 1;
                gbc_TFArg.weighty = 0.1;
                argSetterPanel.add(argTF, gbc_TFArg);
                argsTF.add(argTF);

                JButton btnDel = new JButton("");
                btnDel.setBorder(null);
                btnDel.setContentAreaFilled(false);
                btnDel.setIcon(deleteIcon);
                GridBagConstraints gbc_btnDel = new GridBagConstraints();
                gbc_btnDel.insets = new Insets(0, 0, 0, 5);
                gbc_btnDel.fill = GridBagConstraints.HORIZONTAL;
                gbc_btnDel.anchor = GridBagConstraints.CENTER;
                gbc_btnDel.gridx = 0;
                gbc_btnDel.gridy = guiArgNextId++;
                gbc_btnDel.weighty = 0.1;
                argSetterPanel.add(btnDel, gbc_btnDel);
                delBtnArgs.add(btnDel);
                btnDel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        guiArgNextId--;
                        argSetterPanel.remove(argTF);
                        argSetterPanel.remove(btnDel);
                        delBtnArgs.remove(btnDel);
                        argsTF.remove(argTF);
                        argSetterPanel.repaint();
                        argSetterPanel.revalidate();
                    }
                });
                argSetterPanel.repaint();
                argSetterPanel.revalidate();
            }
        });
        chckbxAutoLoadLibs = new JCheckBox("Auto load libs");
        chckbxAutoLoadLibs.setFont(sansSerif12);

        JLabel projectALabel = new JLabel("Program A");
        JLabel projectBLabel = new JLabel("Program B");
        String[] defaultComboStrsA = { "" };
        String[] defaultComboStrsB = { "" };
        projectACombo = new JComboBox<String>(defaultComboStrsA);
        projectBCombo = new JComboBox<String>(defaultComboStrsB);
        projectALabel.setFont(sansSerif12);
        projectBLabel.setFont(sansSerif12);

        JLabel lbCallFunA = new JLabel("Call function A");
        lbCallFunA.setFont(sansSerif12);
        JLabel lbCallFunB = new JLabel("Call function B");
        lbCallFunB.setFont(sansSerif12);

        callFunTFA = new JTextField();
        callFunTFA.setMinimumSize(new Dimension(100, 20));
        callFunTFA.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                Address dstAddress = addressStorage.getCallFunAddrA();
                if (dstAddress != null) {
                    mColorService.resetColor(programA(), dstAddress);
                    addressStorage.setCallFunAddressA(null);
                }
            }
        });

        callFunTFB = new JTextField();
        callFunTFB.setMinimumSize(new Dimension(100, 20));
        callFunTFB.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                Address dstAddress = addressStorage.getCallFunAddrB();
                if (dstAddress != null) {
                    mColorService.resetColor(programB(), dstAddress);
                    addressStorage.setCallFunAddressB(null);
                }
            }
        });

        // Unfortunately, it was found that GUI gaps look different on different operating systems
        int blankStateTFGap = 10;
        int findAddressGap = 13;
        int blankStateCBGap = 11;
        int scrollAvoidAddrsAreaGap = 11;
        int avoidAreaGap = 11;
        int bufferGap = 8;
        int horizontalCallFunGap = 22;
        if (isWindows) {
            blankStateTFGap = 11;
            findAddressGap = 10;
            blankStateCBGap = 6;
            scrollAvoidAddrsAreaGap = 13;
            avoidAreaGap = 8;
            bufferGap = 0;
            horizontalCallFunGap = 21;
        }

        GroupLayout gl_mainOptionsPanel = new GroupLayout(mainOptionsPanel);
        gl_mainOptionsPanel.setHorizontalGroup(
            gl_mainOptionsPanel.createParallelGroup(Alignment.TRAILING)
                .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                        .addGap(11)
                        .addGroup(gl_mainOptionsPanel.createParallelGroup(Alignment.LEADING)
                            .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                                .addComponent(chckbxAutoLoadLibs, GroupLayout.PREFERRED_SIZE, 134, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(73, Short.MAX_VALUE))
                            .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                                .addGroup(gl_mainOptionsPanel.createParallelGroup(Alignment.LEADING)
                                    .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                                        .addComponent(projectALabel, GroupLayout.PREFERRED_SIZE, 134, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED, 18, Short.MAX_VALUE))
                                    .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                                        .addComponent(projectBLabel, GroupLayout.PREFERRED_SIZE, 134, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED, 18, Short.MAX_VALUE))
                                    .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                                        .addComponent(lbCallFunA, GroupLayout.PREFERRED_SIZE, 134, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED, 18, Short.MAX_VALUE))
                                    .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                                        .addComponent(lbCallFunB, GroupLayout.PREFERRED_SIZE, 134, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.RELATED, 18, Short.MAX_VALUE)))
                                .addGroup(gl_mainOptionsPanel.createParallelGroup(Alignment.LEADING)
                                    .addComponent(projectACombo, GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                                    .addComponent(projectBCombo, GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                                    .addComponent(callFunTFA, GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                                    .addComponent(callFunTFB, GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                                    /*.addComponent(scrollAvoidAddrsArea, GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)*/)
                                .addGap(15))))
        );

        gl_mainOptionsPanel.setVerticalGroup(
            gl_mainOptionsPanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                    .addGap(6)
                    .addComponent(chckbxAutoLoadLibs)
                    .addGap(bufferGap)
                    .addGroup(gl_mainOptionsPanel.createParallelGroup(Alignment.BASELINE)
                        .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                            .addGap(blankStateCBGap)
                            .addComponent(projectALabel))
                        .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                            .addGap(blankStateTFGap)
                            .addComponent(projectACombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                    .addGroup(gl_mainOptionsPanel.createParallelGroup(Alignment.BASELINE)
                        .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                            .addGap(blankStateCBGap)
                            .addComponent(projectBLabel))
                        .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                            .addGap(blankStateTFGap)
                            .addComponent(projectBCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                    .addGroup(gl_mainOptionsPanel.createParallelGroup(Alignment.BASELINE)
                        .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                                .addGap(findAddressGap)
                                .addComponent(lbCallFunA))
                        .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                                .addGap(10)
                                .addComponent(callFunTFA, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                    .addGroup(gl_mainOptionsPanel.createParallelGroup(Alignment.BASELINE)
                        .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                            .addGap(findAddressGap)
                            .addComponent(lbCallFunB))
                        .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                            .addGap(10)
                            .addComponent(callFunTFB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))

                        /*
                        .addGroup(gl_mainOptionsPanel.createParallelGroup(Alignment.BASELINE)
                            .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                                .addGap(avoidAreaGap)
                                .addComponent(chckbxAvoidAddresses))
                            .addGroup(gl_mainOptionsPanel.createSequentialGroup()
                                .addGap(scrollAvoidAddrsAreaGap)
                                .addComponent(scrollAvoidAddrsArea, GroupLayout.PREFERRED_SIZE, 45, Short.MAX_VALUE)))*/
                    .addContainerGap())
        );
        gl_mainOptionsPanel.setAutoCreateContainerGaps(false);
        gl_mainOptionsPanel.setAutoCreateGaps(false);
        gl_mainOptionsPanel.setHonorsVisibility(false);
        mainOptionsPanel.setLayout(gl_mainOptionsPanel);

        GridBagLayout gbl_writeMemoryPanel = new GridBagLayout();
        gbl_writeMemoryPanel.columnWidths = new int[] {
            0,
            0,
            0,
            0,
            0,
            0
        };
        gbl_writeMemoryPanel.rowHeights = new int[] {
            0,
            0,
            0
        };
        gbl_writeMemoryPanel.columnWeights = new double[] {
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            Double.MIN_VALUE
        };
        gbl_writeMemoryPanel.rowWeights = new double[] {
            0.0,
            0.0,
            Double.MIN_VALUE
        };
        writeMemoryPanel.setLayout(gbl_writeMemoryPanel);

        lblWriteToMemory = new JLabel(htmlString);
        lblWriteToMemory.setHorizontalAlignment(SwingConstants.CENTER);
        lblWriteToMemory.setFont(sansSerif12);

        lbStoreAddr = new JLabel("Address");
        lbStoreAddr.setFont(sansSerif12);
        GridBagConstraints gbc_lbStoreAddr = new GridBagConstraints();
        gbc_lbStoreAddr.weightx = 1.0;
        gbc_lbStoreAddr.insets = new Insets(0, 0, 0, 5);
        gbc_lbStoreAddr.gridx = 1;
        gbc_lbStoreAddr.gridy = 0;
        writeMemoryPanel.add(lbStoreAddr, gbc_lbStoreAddr);

        lbStoreVal = new JLabel("Value");
        lbStoreVal.setFont(sansSerif12);
        GridBagConstraints gbc_lbStoreVal = new GridBagConstraints();
        gbc_lbStoreVal.weightx = 1.0;
        gbc_lbStoreVal.insets = new Insets(0, 0, 0, 5);
        gbc_lbStoreVal.gridx = 3;
        gbc_lbStoreVal.gridy = 0;
        writeMemoryPanel.add(lbStoreVal, gbc_lbStoreVal);

        memStoreAddrTF = new JTextField();
        GridBagConstraints gbc_memStoreAddrTF = new GridBagConstraints();
        gbc_memStoreAddrTF.anchor = GridBagConstraints.CENTER;
        gbc_memStoreAddrTF.fill = GridBagConstraints.HORIZONTAL;
        gbc_memStoreAddrTF.insets = new Insets(0, 0, 0, 5);
        gbc_memStoreAddrTF.gridx = 1;
        gbc_memStoreAddrTF.gridy = 1;
        gbc_memStoreAddrTF.weightx = 1;
        gbc_memStoreAddrTF.weighty = 0.1;
        writeMemoryPanel.add(memStoreAddrTF, gbc_memStoreAddrTF);

        memStoreValueTF = new JTextField();
        GridBagConstraints gbc_memStoreValueTF = new GridBagConstraints();
        gbc_memStoreValueTF.insets = new Insets(0, 0, 0, 5);
        gbc_memStoreValueTF.fill = GridBagConstraints.HORIZONTAL;
        gbc_memStoreValueTF.anchor = GridBagConstraints.CENTER;
        gbc_memStoreValueTF.gridx = 3;
        gbc_memStoreValueTF.gridy = 1;
        gbc_memStoreValueTF.weightx = 1;
        gbc_memStoreValueTF.weighty = 0.1;
        writeMemoryPanel.add(memStoreValueTF, gbc_memStoreValueTF);

        btnAddWM = new JButton("");
        btnAddWM.setContentAreaFilled(false);
        btnAddWM.setBorder(null);
        btnAddWM.setIcon(addIcon);
        GridBagConstraints gbc_btnAddWM = new GridBagConstraints();
        gbc_btnAddWM.weighty = 0.1;
        gbc_btnAddWM.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnAddWM.anchor = GridBagConstraints.CENTER;
        gbc_btnAddWM.insets = new Insets(0, 0, 0, 5);
        gbc_btnAddWM.gridx = 0;
        gbc_btnAddWM.gridy = 1;
        gbc_btnAddWM.weighty = 0.1;
        writeMemoryPanel.add(btnAddWM, gbc_btnAddWM);
        btnAddWM.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextField addrTF = new JTextField();
                GridBagConstraints gbc_addrTF = new GridBagConstraints();
                gbc_addrTF.fill = GridBagConstraints.HORIZONTAL;
                gbc_addrTF.anchor = GridBagConstraints.CENTER;
                gbc_addrTF.gridx = 1;
                gbc_addrTF.insets = new Insets(0, 0, 0, 5);
                gbc_addrTF.gridy = guiStoreNextId;
                gbc_addrTF.weightx = 1;
                gbc_addrTF.weighty = 0.1;
                writeMemoryPanel.add(addrTF, gbc_addrTF);

                JTextField valTF = new JTextField();
                GridBagConstraints gbc_valTF = new GridBagConstraints();
                gbc_valTF.fill = GridBagConstraints.HORIZONTAL;
                gbc_valTF.anchor = GridBagConstraints.CENTER;
                gbc_valTF.insets = new Insets(0, 0, 0, 5);
                gbc_valTF.gridx = 3;
                gbc_valTF.gridy = guiStoreNextId;
                gbc_valTF.weightx = 1;
                gbc_valTF.weighty = 0.1;
                writeMemoryPanel.add(valTF, gbc_valTF);
                memStore.put(addrTF, valTF);

                JButton btnDel = new JButton("");
                btnDel.setBorder(null);
                btnDel.setContentAreaFilled(false);
                btnDel.setIcon(deleteIcon);
                GridBagConstraints gbc_btnDel = new GridBagConstraints();
                gbc_btnDel.fill = GridBagConstraints.HORIZONTAL;
                gbc_btnDel.anchor = GridBagConstraints.CENTER;
                gbc_btnDel.insets = new Insets(0, 0, 0, 5);
                gbc_btnDel.gridx = 0;
                gbc_btnDel.gridy = guiStoreNextId++;
                gbc_btnDel.weighty = 0.1;
                writeMemoryPanel.add(btnDel, gbc_btnDel);
                delStoreBtns.add(btnDel);
                btnDel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        guiStoreNextId--;
                        writeMemoryPanel.remove(addrTF);
                        writeMemoryPanel.remove(valTF);
                        writeMemoryPanel.remove(btnDel);
                        delStoreBtns.remove(btnDel);
                        memStore.remove(addrTF, valTF);
                        writeMemoryPanel.repaint();
                        writeMemoryPanel.revalidate();
                    }
                });
                writeMemoryPanel.repaint();
                writeMemoryPanel.revalidate();
            }
        });

        GroupLayout gl_customOptionsPanel = new GroupLayout(customOptionsPanel);
        gl_customOptionsPanel.setHorizontalGroup(
            gl_customOptionsPanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_customOptionsPanel.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(gl_customOptionsPanel.createParallelGroup(Alignment.LEADING)
                        .addGroup(gl_customOptionsPanel.createSequentialGroup()
                            .addComponent(lblWriteToMemory, GroupLayout.PREFERRED_SIZE, 327, Short.MAX_VALUE)
                            .addPreferredGap(ComponentPlacement.RELATED, 237, GroupLayout.PREFERRED_SIZE))
                        .addComponent(writeMemoryPanel, GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE))
                    .addGap(25))
        );
        gl_customOptionsPanel.setVerticalGroup(
            gl_customOptionsPanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_customOptionsPanel.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblWriteToMemory)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(writeMemoryPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addGap(54))
        );
        customOptionsPanel.setLayout(gl_customOptionsPanel);

        GridBagLayout gbl_vectorsPanel = new GridBagLayout();
        gbl_vectorsPanel.columnWidths = new int[] {
            0,
            0,
            0,
            0,
            0,
            0
        };
        gbl_vectorsPanel.rowHeights = new int[] {
            0,
            0
        };
        gbl_vectorsPanel.columnWeights = new double[] {
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            Double.MIN_VALUE
        };
        gbl_vectorsPanel.rowWeights = new double[] {
            0.0,
            0.0
        };

        lbStatus = new JLabel("Status:");
        lbStatus.setForeground(Color.BLUE);
        lbStatus.setFont(sansSerif13);

        statusLabel = new JLabel(configuringString);
        statusLabel.setFont(sansSerif13);

        statusLabelFound = new JLabel("");
        statusLabelFound.setFont(sansSerif13);

        btnRun = new JButton("Run");
        btnRun.setIcon(startIcon);
        btnRun.setFont(sansSerif12);

        solutionTextArea = new JTextArea();
        solutionTextArea.setFont(sansSerif12);
        scrollSolutionTextArea = new JScrollPane(solutionTextArea);
        solutionTextArea.setEditable(false);
        scrollSolutionTextArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollSolutionTextArea.setBorder(new LineBorder(Color.blue, 1));
        scrollSolutionTextArea.setVisible(false);

        btnStop = new JButton("Stop");
        btnStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (new File(tmpDir + "angr_options.json").exists()) {
                    setIsTerminated(true);
                    statusLabel.setText("[+] Stopping...");
                    statusLabelFound.setText("");
                    scrollSolutionTextArea.setVisible(false);
                }
            }
        });
        btnStop.setFont(sansSerif12);
        btnStop.setIcon(stopIcon);

        btnReset = new JButton("Reset");
        btnReset.setIcon(resetIcon);
        btnReset.setFont(sansSerif12);
        btnReset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetState();
            }
        });

        GroupLayout gl_statusPanel = new GroupLayout(statusPanel);
        gl_statusPanel.setHorizontalGroup(
            gl_statusPanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_statusPanel.createSequentialGroup()
                    .addGap(10)
                    .addComponent(statusLabelFound, GroupLayout.DEFAULT_SIZE, 127, Short.MAX_VALUE)
                    .addGap(71)
                    .addComponent(scrollSolutionTextArea, GroupLayout.DEFAULT_SIZE, 378, Short.MAX_VALUE)
                    .addGap(10))
                .addGroup(gl_statusPanel.createSequentialGroup()
                    .addGroup(gl_statusPanel.createParallelGroup(Alignment.TRAILING)
                        .addGroup(gl_statusPanel.createSequentialGroup()
                            .addGap(77)
                            .addComponent(btnRun, GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)
                            .addGap(77)
                            .addComponent(btnStop, GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)
                            .addGap(77)
                            .addComponent(btnReset, GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)
                            .addGap(1))
                        .addGroup(gl_statusPanel.createSequentialGroup()
                            .addGap(10)
                            .addComponent(statusLabel, GroupLayout.DEFAULT_SIZE, 495, Short.MAX_VALUE)))
                    .addGap(91))
                .addGroup(gl_statusPanel.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lbStatus, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(538, Short.MAX_VALUE))
        );
        gl_statusPanel.setVerticalGroup(
            gl_statusPanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_statusPanel.createSequentialGroup()
                    .addGap(10)
                    .addGroup(gl_statusPanel.createParallelGroup(Alignment.BASELINE)
                        .addComponent(btnRun, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnStop, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnReset, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(lbStatus, GroupLayout.PREFERRED_SIZE, 13, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(statusLabel, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE)
                    .addGroup(gl_statusPanel.createParallelGroup(Alignment.LEADING)
                        .addGroup(gl_statusPanel.createSequentialGroup()
                            .addGap(5)
                            .addComponent(statusLabelFound, GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE))
                        .addGroup(gl_statusPanel.createSequentialGroup()
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(scrollSolutionTextArea, GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        statusPanel.setLayout(gl_statusPanel);

        JPanel hookPanel = new JPanel();
        TitledBorder borderHP = BorderFactory.createTitledBorder("Hook options");
        borderHP.setTitleFont(sansSerif12);
        hookPanel.setBorder(borderHP);

        GroupLayout gl_mainPanel = new GroupLayout(mainPanel);
        gl_mainPanel.setHorizontalGroup(
            gl_mainPanel.createParallelGroup(Alignment.TRAILING)
            .addGroup(gl_mainPanel.createSequentialGroup()
                .addGroup(gl_mainPanel.createParallelGroup(Alignment.LEADING)
                    .addGroup(gl_mainPanel.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(statusPanel, GroupLayout.DEFAULT_SIZE, 550, Short.MAX_VALUE))
                    .addGroup(gl_mainPanel.createSequentialGroup()
                        .addGroup(gl_mainPanel.createParallelGroup(Alignment.LEADING)
                            .addGroup(gl_mainPanel.createSequentialGroup()
                                .addGap(10)
                                .addComponent(mainOptionsPanel, GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE))
                            .addGroup(gl_mainPanel.createSequentialGroup()
                                .addGap(10)
                                .addComponent(mallocPanel, GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE))
                            .addGroup(gl_mainPanel.createSequentialGroup()
                                .addGap(10)
                                .addComponent(symbolsPanel, GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE))
                            .addGroup(gl_mainPanel.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(argumentsPanel, GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE))
                            .addGroup(gl_mainPanel.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(hookPanel, GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)))
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(customOptionsPanel, GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)))
                .addGap(13))
        );
        gl_mainPanel.setVerticalGroup(
            gl_mainPanel.createParallelGroup(Alignment.LEADING)
            .addGroup(gl_mainPanel.createSequentialGroup()
                .addGroup(gl_mainPanel.createParallelGroup(Alignment.LEADING)
                    .addGroup(gl_mainPanel.createSequentialGroup()
                        .addGap(10)
                        .addComponent(mainOptionsPanel, GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE)
                        .addGap(2)
                        .addComponent(mallocPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
                        .addGap(2)
                        .addComponent(symbolsPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
                        .addGap(2)
                        .addComponent(argumentsPanel, GroupLayout.DEFAULT_SIZE, 81, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(hookPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE))
                    .addGroup(gl_mainPanel.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(customOptionsPanel, GroupLayout.DEFAULT_SIZE, 357, Short.MAX_VALUE)))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(statusPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGap(5))
        );
        mainPanel.setLayout(gl_mainPanel);

        JButton btnAddHook = new JButton("Add Hook");
        btnAddHook.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (getHookWindowState()) {
                    hookHandler = new HookHandler(AngryGhidraProvider.this);
                    hookHandler.main();
                    setHookWindowState(false);
                } else {
                    hookHandler.toFront();
                }
            }
        });
        btnAddHook.setFont(new Font("SansSerif", Font.PLAIN, 11));

        GroupLayout gl_hookPanel = new GroupLayout(hookPanel);
        gl_hookPanel.setHorizontalGroup(
            gl_hookPanel.createParallelGroup(Alignment.LEADING)
                .addGroup(gl_hookPanel.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(btnAddHook, GroupLayout.PREFERRED_SIZE, 105, Short.MAX_VALUE)
                    .addGap(43)
                    .addComponent(hookLablesPanel, GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)
                    .addContainerGap())
        );
        gl_hookPanel.setVerticalGroup(
            gl_hookPanel.createParallelGroup(Alignment.TRAILING)
                .addGroup(gl_hookPanel.createSequentialGroup()
                    .addGroup(gl_hookPanel.createParallelGroup(Alignment.TRAILING)
                        .addGroup(Alignment.LEADING, gl_hookPanel.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(btnAddHook))
                        .addGroup(gl_hookPanel.createSequentialGroup()
                            .addGap(10)
                            .addComponent(hookLablesPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                    .addGap(34))
        );
        hookPanel.setLayout(gl_hookPanel);

        GridBagLayout gbl_hookLablesPanel = new GridBagLayout();
        gbl_hookLablesPanel.columnWidths = new int[] {0};
        gbl_hookLablesPanel.rowHeights = new int[] {0};
        gbl_hookLablesPanel.columnWeights = new double[] {Double.MIN_VALUE};
        gbl_hookLablesPanel.rowWeights = new double[] {Double.MIN_VALUE};
        hookLablesPanel.setLayout(gbl_hookLablesPanel);
        btnRun.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusLabel.setText(configuringString);
                statusLabelFound.setText("");
                setIsTerminated(false);
                angrProcessing.clearTraceList(false);
                JSONObject angr_options = new JSONObject();
                Boolean auto_load_libs = false;
                if (chckbxAutoLoadLibs.isSelected()) {
                    auto_load_libs = true;
                }
                angr_options.put("auto_load_libs", auto_load_libs);
                /*
                if (chckbxBlankState.isSelected()) {
                    if (!blankStateTF.getText().matches("0x[0-9A-Fa-f]+")) {
                        statusLabel.setForeground(Color.red);
                        statusLabel.setText("[–] Error: enter the correct blank state address value in hex format!");
                        return;
                    }
                    String blank_state = blankStateTF.getText();
                    angr_options.put("blank_state", blank_state);
                }
                */
                if (!callFunTFA.getText().matches("0x[0-9A-Fa-f]+")) {
                    statusLabel.setForeground(Color.red);
                    statusLabel.setText("[–] Error: enter the correct destination address in hex format!");
                    return;
                }
                if (!callFunTFB.getText().matches("0x[0-9A-Fa-f]+")) {
                    statusLabel.setForeground(Color.red);
                    statusLabel.setText("[–] Error: enter the correct destination address in hex format!");
                    return;
                }
                String find_addr = callFunTFA.getText();
                angr_options.put("find_address", find_addr);
                if (chckbxAvoidAddresses.isSelected()) {
                    if (!avoidTextArea.getText().replaceAll("\\s+", "").matches("[0x0-9a-fA-F, /,]+")) {
                        statusLabel.setForeground(Color.red);
                        statusLabel.setText("[–] Error: enter the correct avoid addresses in hex format separated by comma!");
                        return;
                    }
                    String avoid = avoidTextArea.getText().replaceAll("\\s+", "");
                    angr_options.put("avoid_address", avoid);
                }
                if (chckbxArg.isSelected()) {
                    if (!firstArgTF.getText().isEmpty()) {
                        JSONObject argDetails = new JSONObject();
                        int id = 1;
                        argDetails.put(String.valueOf(id++), firstArgTF.getText());
                        for (JTextField itf : argsTF) {
                            String value = itf.getText();
                            if (!value.isEmpty()) {
                                argDetails.put(String.valueOf(id), value);
                            }
                            id++;
                        }
                        angr_options.put("arguments", argDetails);
                    }
                }
                if (!vectorAddressTF.getText().isEmpty() &&
                        !vectorLenTF.getText().isEmpty()) {
                    JSONObject vectorDetails = new JSONObject();
                    vectorDetails.put(vectorAddressTF.getText(), vectorLenTF.getText());
                    for (Entry<IntegerTextField, IntegerTextField> entry : vectors.entrySet()) {
                        String addr = entry.getKey().getText();
                        String len = entry.getValue().getText();
                        if (!addr.isEmpty() && !len.isEmpty()) {
                            vectorDetails.put(addr, len);
                        }
                    }
                    angr_options.put("vectors", vectorDetails);
                }
                if (!memStoreAddrTF.getText().isEmpty() && !memStoreValueTF.getText().isEmpty()) {
                    JSONObject storeDetails = new JSONObject();
                    storeDetails.put(memStoreAddrTF.getText(), memStoreValueTF.getText());
                    for (Entry<JTextField, JTextField> entry : memStore.entrySet()) {
                        String addr = entry.getKey().getText();
                        String val = entry.getValue().getText();
                        if (!addr.isEmpty() && !val.isEmpty()) {
                            storeDetails.put(addr, val);
                        }
                    }
                    angr_options.put("mem_store", storeDetails);
                }
                String reg1 = registerTF.getText();
                String val1 = valueTF.getText();
                if (symbolicVectorInputCheck(reg1, val1)) {
                    JSONObject regDetails = new JSONObject();
                    regDetails.put(reg1, val1);
                    for (Entry <JTextField, JTextField> entry : presetRegs.entrySet()) {
                        String reg = entry.getKey().getText();
                        String val = entry.getValue().getText();
                        if (symbolicVectorInputCheck(reg, val)) {
                            regDetails.put(reg, val);
                        }
                    }
                    angr_options.put("regs_vals", regDetails);
                }
                if (!hooks.isEmpty()) {
                    JSONArray hookList = new JSONArray();
                    for (Entry <String[], String[][]> entry: hooks.entrySet()) {
                        JSONObject hookDetails = new JSONObject();
                        String[] hookOptions = entry.getKey();
                        String hookAddress = hookOptions[0];
                        hookDetails.put("length", hookOptions[1]);
                        String[][] regs = entry.getValue();
                        for (int i = 0; i <regs[0].length; i++) {
                            if (regs[0][i] != null && regs[1][i] != null) {
                                hookDetails.put(regs[0][i], regs[1][i]);
                            }
                        }
                        JSONObject newHook = new JSONObject();
                        newHook.put(hookAddress, hookDetails);
                        hookList.put(newHook);
                    }
                    angr_options.put("hooks", hookList);
                }
                String binary_path = focusedProgram.getExecutablePath();
                if (isWindows) {
                    binary_path = binary_path.replaceFirst("/", "");
                    binary_path = binary_path.replace("/", "\\");
                }
                angr_options.put("binary_file", binary_path);
                angr_options.put("base_address", "0x" + Long.toHexString(focusedProgram.getMinAddress().getOffset()));
                if (focusedProgram.getExecutableFormat().contains("Raw Binary")) {
                    String language = focusedProgram.getLanguage().toString();
                    String arch = language.substring(0, language.indexOf("/"));
                    angr_options.put("raw_binary_arch", arch);
                }
                statusLabel.setForeground(Color.black);
                mainPanel.revalidate();
                File angrOptionsFile = new File(tmpDir + "angr_options.json");
                if (angrOptionsFile.exists()) {
                    angrOptionsFile.delete();
                }
                try {
                    FileWriter file = new FileWriter(tmpDir + "angr_options.json");
                    file.write(angr_options.toString());
                    file.flush();
                    file.close();
                } catch (Exception ex) {}
                angrProcessing.preparetoRun(angrOptionsFile);
            }
        });
    }

    private void resetState() {
        setIsTerminated(false);
        statusLabel.setText(configuringString);
        statusLabel.setForeground(Color.black);
        statusLabelFound.setText("");
        solutionTextArea.setText("");
        scrollSolutionTextArea.setVisible(false);
        chckbxAutoLoadLibs.setSelected(false);
        angrProcessing.setSolutionExternal(null);
        angrProcessing.clearTraceList(true);

        // Reset call addresses
        callFunTFA.setText("");
        Address dstAddressA = addressStorage.getCallFunAddrA();
        if (dstAddressA != null) {
            mColorService.resetColor(programA(), dstAddressA);
            addressStorage.setCallFunAddressA(null);
        }

        callFunTFB.setText("");
        Address dstAddressB = addressStorage.getCallFunAddrB();
        if (dstAddressB != null) {
            mColorService.resetColor(programB(), dstAddressB);
            addressStorage.setCallFunAddressB(null);
        }

        // Reset find address
        callFunTFB.setText("");

        mainOptionsPanel.revalidate();

        // Reset malloc panel
        guiMallocNextId = 2;
        // TODO: Rest of reset

        // Reset arguments mainPanel
        guiArgNextId = 2;
        lbArgContent.setVisible(false);
        btnAddArg.setVisible(false);
        for (JButton btnDel: delBtnArgs) {
            argSetterPanel.remove(btnDel);
        }
        for (JTextField argTF : argsTF) {
            argSetterPanel.remove(argTF);
        }
        delBtnArgs.clear();
        argsTF.clear();
        firstArgTF.setText("");
        firstArgTF.setVisible(false);
        chckbxArg.setSelected(false);
        argSetterPanel.repaint();
        argSetterPanel.revalidate();

        // Reset mem set contents
        guiStoreNextId = 2;
        for (Entry<JTextField, JTextField> entry : memStore.entrySet()) {
            JTextField addrTF = entry.getKey();
            JTextField valTF = entry.getValue();
            writeMemoryPanel.remove(addrTF);
            writeMemoryPanel.remove(valTF);
        }
        for (JButton button : delStoreBtns) {
            writeMemoryPanel.remove(button);
        }
        memStoreAddrTF.setText("");
        memStoreValueTF.setText("");
        memStore.clear();
        delStoreBtns.clear();
        writeMemoryPanel.repaint();
        writeMemoryPanel.revalidate();

        registerTF.setText("");
        valueTF.setText("");
        delRegsBtns.clear();
        presetRegs.clear();

        // Reset all hooks
        if (hookHandler != null) {
            hookHandler.requestClearHooks();
        }
        hooks.clear();
        for (JButton button : delHookBtns) {
            hookLablesPanel.remove(button);
        }
        for (JLabel label : lbHooks) {
            hookLablesPanel.remove(label);
        }
        lbHooks.clear();
        delHookBtns.clear();
        hookLablesPanel.repaint();
        hookLablesPanel.revalidate();
    }

    public boolean symbolicVectorInputCheck(String reg, String value) {
        return !reg.isEmpty() && !value.isEmpty() && (value.matches("0x[0-9A-Fa-f]+") ||
                value.matches("[0-9]+") || value.contains("sv"));
    }

    public JTextField getCallFunA() {
        return callFunTFA;
    }

    public JTextField getCallFunB() {
        return callFunTFB;
    }

    public JTextArea getTextArea() {
        return avoidTextArea;
    }

    public JCheckBox getCBAvoidAddresses() {
        return chckbxAvoidAddresses;
    }

    public JPanel getWriteMemoryPanel() {
        return writeMemoryPanel;
    }

    public JPanel getHookLablesPanel() {
        return hookLablesPanel;
    }

    public int getGuiStoreCounter() {
        return guiStoreNextId;
    }

    public void setGuiStoreCounter(int value) {
        guiStoreNextId = value;
    }

    public JTextField getStoreAddressTF() {
        return memStoreAddrTF;
    }

    public JTextField getStoreValueTF() {
        return memStoreValueTF;
    }

    public void putIntoMemStore(JTextField tf1, JTextField tf2) {
        memStore.put(tf1, tf2);
    }

    public void removeFromMemStore(IntegerTextField tf1, IntegerTextField tf2) {
        memStore.remove(tf1, tf2);
    }

    public void putIntoDelStoreBtns(JButton button) {
        delStoreBtns.add(button);
    }

    public void removeFromDelStoreBtns(JButton button) {
        delStoreBtns.remove(button);
    }

    public void putIntoDelHookBtns(JButton button) {
        delHookBtns.add(button);
    }

    public void removeFromDelHookBtns(JButton button) {
        delHookBtns.remove(button);
    }

    public ImageIcon getDeleteIcon() {
        return deleteIcon;
    }

    public ImageIcon getAddIcon() {
        return addIcon;
    }

    public void putIntoHooks(String[] options, String[][] regs) {
        hooks.put(options, regs);
    }

    public void removeFromHooks(String[] options, String[][] regs) {
        hooks.remove(options, regs);
    }

    public void putIntoLbHooks(JLabel label) {
        lbHooks.add(label);
    }

    public void removeFromLbHooks(JLabel label) {
        lbHooks.remove(label);
    }

    public Boolean getIsTerminated() {
        return isTerminated;
    }

    public void setIsTerminated(Boolean value) {
        isTerminated = value;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JLabel getStatusLabelFound() {
        return statusLabelFound;
    }

    public JScrollPane getScrollSolutionTextArea() {
        return scrollSolutionTextArea;
    }

    public JTextArea getSolutionTextArea() {
        return solutionTextArea;
    }

    public void setProgram(Program program) {
        focusedProgram = program;
        if (program != null) {
            availablePrograms.add(program);
            refreshComboBoxes();
        }
    }

    public void programOpened(Program program) {
        if (program != null) {
            availablePrograms.add(program);
            refreshComboBoxes();
        }
    }

    public void programClosed(Program program) {
        if (program != null) {
            availablePrograms.remove(program);
            refreshComboBoxes();
        }
    }

    private void refreshComboBoxes() {
        refreshComboBox(projectACombo);
        refreshComboBox(projectBCombo);
        mainOptionsPanel.repaint();
        mainOptionsPanel.revalidate();
    }

    public Program getFocusedProgram() {
        return focusedProgram;
    }

    private void refreshComboBox(JComboBox<String> box) {
        String selectedItem = (String) box.getSelectedItem();
        box.removeAllItems();
        box.addItem("");
        boolean containsPrevSelected = false;
        if (selectedItem.equals("")) {
            containsPrevSelected = true;
        }
        for (Program p : availablePrograms) {
            box.addItem(p.getName());
            if (p.getName().equals(selectedItem)) {
                containsPrevSelected = true;
            }
        }
        if (containsPrevSelected) {
            box.setSelectedItem(selectedItem);
        }
    }

    public Program programA() {
        return getProgram(projectACombo);
    }

    public Program programB() {
        return getProgram(projectBCombo);
    }

    private Program getProgram(JComboBox<String> comboBox) {
        String selectedItem = (String) comboBox.getSelectedItem();
        for (Program p : availablePrograms) {
            if (p.getName().equals(selectedItem)) {
                return p;
            }
        }
        return null;
    }

    public void setHookWindowState(boolean value) {
        isHookWindowClosed = value;
    }

    public boolean getHookWindowState() {
        return isHookWindowClosed;
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }
}
