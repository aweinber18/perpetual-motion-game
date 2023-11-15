package com.example.perpetualmotion.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.perpetualmotion.R;
import com.example.perpetualmotion.databinding.ActivityMainBinding;
import com.example.perpetualmotion.databinding.MainIncludeActivityBottomBarAndFabBinding;
import com.example.perpetualmotion.databinding.MainIncludeContentBoardBinding;
import com.google.android.material.snackbar.Snackbar;
import com.mintedtech.perpetual_motion.pm_game.PMGame;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainIncludeActivityBottomBarAndFabBinding bottomBarAndFabBinding;
    private Snackbar mSnackbar;
    private View mSnackbarView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        bottomBarAndFabBinding = MainIncludeActivityBottomBarAndFabBinding.bind(binding.getRoot());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.includeActivityToolbar.toolbar);

        mSnackbarView = findViewById(android.R.id.content);
        mSnackbar = Snackbar.make(mSnackbarView, "Welcome!", Snackbar.LENGTH_LONG);
        mSnackbar.setText("Bonjour!").show();
        bottomBarAndFabBinding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (mSnackbar.isShown())
            mSnackbar.dismiss();

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }
}