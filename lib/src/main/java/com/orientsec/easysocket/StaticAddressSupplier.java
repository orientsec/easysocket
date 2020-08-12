package com.orientsec.easysocket;

import androidx.annotation.NonNull;

import java.util.List;

public class StaticAddressSupplier implements Supplier<List<Address>> {

    static StaticAddressSupplier build(@NonNull List<Address> addressList) {
        if (addressList.isEmpty()) {
            throw new IllegalArgumentException("Address list is empty.");
        }
        return new StaticAddressSupplier(addressList);
    }

    private List<Address> addressList;

    StaticAddressSupplier(List<Address> addressList) {
        this.addressList = addressList;
    }

    @NonNull
    @Override
    public List<Address> get() {
        return addressList;
    }
}
