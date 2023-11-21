package com.example.perpetualmotion.interfaces;

import android.view.View;

/**
 * Used to send data out of the Adapter to the host Activity
 */
public interface AdapterOnItemClickListener
{
    void onItemClick (int position, View viewClicked);
}