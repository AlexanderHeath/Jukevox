package com.liquidcode.jukevox.adapters;

import android.bluetooth.BluetoothDevice;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import java.util.HashMap;

public class DeviceListAdapter implements ListAdapter {

    // our internal device list
    private HashMap<String, BluetoothDevice> m_deviceMap = null;

    public DeviceListAdapter(HashMap<String, BluetoothDevice> deviceMap) {
        m_deviceMap = deviceMap;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int i) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {

    }

    @Override
    public int getCount() {
        return m_deviceMap.size();
    }

    @Override
    public Object getItem(int i) {
        return m_deviceMap.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        return null;
    }

    @Override
    public int getItemViewType(int i) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return (m_deviceMap.isEmpty());
    }
}
