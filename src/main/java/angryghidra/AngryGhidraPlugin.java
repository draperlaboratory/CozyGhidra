package angryghidra;

import ghidra.app.ExamplesPluginPackage;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.framework.plugintool.PluginInfo;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.model.listing.Program;

@PluginInfo(
    status = PluginStatus.STABLE,
    packageName = ExamplesPluginPackage.NAME,
    category = PluginCategoryNames.ANALYSIS,
    shortDescription = "Use angr in Ghidra",
    description = "One-click symbolic execution using angr in Ghidra"
)

public class AngryGhidraPlugin extends ProgramPlugin {
    private PluginTool mTool;
    private AngryGhidraProvider provider;
    private AngryGhidraPopupMenu popup;
    private UserAddressStorage addressStorage;
    private LocalColorizingService colorService;
    private Program mProgram;

    public AngryGhidraPlugin(PluginTool tool) {
        super(tool);
        mTool = tool;
        addressStorage = new UserAddressStorage();
        provider = new AngryGhidraProvider(this, getName(), mProgram);
        popup = new AngryGhidraPopupMenu(this);
    }

    @Override
    protected void programActivated(Program program) {
        mProgram = program;
        provider.setProgram(program);
        provideColorService();
    }

    @Override
    protected void programOpened(Program program) {
        provider.programOpened(program);
    }

    @Override
    protected void programClosed(Program program) {
        provider.programClosed(program);
    }

    public void provideColorService() {
        colorService = new LocalColorizingService(mTool);
        popup.setColorService(colorService);
        provider.setColorService(colorService);
    }

    public AngryGhidraProvider getProvider() {
        return provider;
    }

    public UserAddressStorage getAddressStorage() {
        return addressStorage;
    }
}
