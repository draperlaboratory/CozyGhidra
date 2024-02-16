package angryghidra;

import java.util.ArrayList;
import java.util.List;
import ghidra.program.model.address.Address;

public class UserAddressStorage {
    private Address currentCallFunAddrA;
    private Address currentCallFunAddrB;
    private Address currentBlankStateAddr;
    private List <Address> currentAvoidAddresses;

    public UserAddressStorage() {
        currentCallFunAddrA = null;
        currentCallFunAddrB = null;
        currentBlankStateAddr = null;
        currentAvoidAddresses = new ArrayList <Address>();
    }

    public Address getCallFunAddrA() {
        return currentCallFunAddrA;
    }

    public Address getCallFunAddrB() {
        return currentCallFunAddrB;
    }

    public Address getCurrentCallFunAddrB() { return currentCallFunAddrB; }

    public Address getBlankStateAddress() {
        return currentBlankStateAddr;
    }

    public List <Address> getAvoidAddresses() {
        return currentAvoidAddresses;
    }

    public Address getAvoidAddress(int index) {
        return currentAvoidAddresses.get(index);
    }

    public void setCallFunAddressA(Address address) {
        currentCallFunAddrA = address;
    }

    public void setCallFunAddressB(Address address) {
        currentCallFunAddrB = address;
    }

    public void setBlankStateAddress(Address address) {
        currentBlankStateAddr = address;
    }

    public void setAvoidAddresses(List <Address> addresses) {
        currentAvoidAddresses = addresses;
    }

    public void addAvoidAddress(Address address) {
        currentAvoidAddresses.add(address);
    }

    public void removeAvoidAddress(Address address) {
        currentAvoidAddresses.remove(address);
    }

    public void removeAvoidAddress(int index) {
        currentAvoidAddresses.remove(index);
    }

    public void clearAvoidAddresses() {
        currentAvoidAddresses.clear();
    }
}
