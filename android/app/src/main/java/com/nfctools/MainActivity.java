package com.nfctools;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(ReadCardByNFCPlugin.class);
        super.onCreate(savedInstanceState);
    }

}
