package angryghidra;

import java.awt.Color;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.app.plugin.core.colorizer.ColorizingService;

public class LocalColorizingService {
    private ColorizingService mCService;

    public LocalColorizingService(PluginTool tool) {
        mCService = tool.getService(ColorizingService.class);
    }

    public void resetColor(Program program, Address address) {
        int TransactionID = program.startTransaction("resetColor");
        mCService.clearBackgroundColor(address, address);
        program.endTransaction(TransactionID, true);
    }

    public void setColor(Program program, Address address, Color color) {
        int TransactionID = program.startTransaction("setColor");
        mCService.setBackgroundColor(address, address, color);
        program.endTransaction(TransactionID, true);
    }

    public void setColor(Address address, Color color) {
        // TODO: Remove this function
        throw new RuntimeException("Reached old setColor");
    }

    public void resetColor(Address address) {
        // TODO: Remove this function
    }
}
