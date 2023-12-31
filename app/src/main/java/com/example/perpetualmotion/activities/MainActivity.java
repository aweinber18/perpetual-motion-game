package com.example.perpetualmotion.activities;

import static com.example.perpetualmotion.classes.Utils.showInfoDialog;
import static com.example.perpetualmotion.classes.Utils.showYesNoDialog;
import static com.mintedtech.perpetual_motion.pm_game.PMGame.getJSONof;
import static com.mintedtech.perpetual_motion.pm_game.PMGame.restoreGameFromJSON;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.perpetualmotion.interfaces.AdapterOnItemClickListener;
import com.example.perpetualmotion.R;
import com.example.perpetualmotion.databinding.ActivityMainBinding;
import com.example.perpetualmotion.databinding.MainIncludeActivityBottomBarAndFabBinding;
import com.google.android.material.snackbar.Snackbar;
import com.mintedtech.perpetual_motion.pm_game.Card;
import com.mintedtech.perpetual_motion.pm_game.PMGame;
import com.example.perpetualmotion.classes.CardPilesAdapter;
public class MainActivity extends AppCompatActivity {

    // Game (current game) object
    private PMGame mCurrentGame;

    // Adapter (current board) object
    private CardPilesAdapter mAdapter;

    // Status Bar and SnackBar View references
    private TextView mTv_cardsRemaining, mTv_cardsInDeck;
    private View mSbContainer;
    private Snackbar mSnackbar;

    private boolean mIsNightMode;               // Sent to Adapter for when it sets the suit colors


    // UI Strings
    private String mWINNER_MSG, mNON_WINNER_MSG;

    // Keys used for save/restore during rotation and for Preferences if auto-save (above) is turned on
    private final String mKeyCheckedPiles = "CHECKED_PILES";
    private final String mKeyGame = "GAME";

    private ActivityMainBinding binding;
    private MainIncludeActivityBottomBarAndFabBinding bottomBarAndFabBinding;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(mKeyGame, getJSONof(mCurrentGame));
        outState.putBooleanArray(mKeyCheckedPiles, mAdapter.getCheckedPiles());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView();
        setSupportActionBar(binding.includeActivityToolbar.toolbar);
        bottomBarAndFabBinding.fab.setOnClickListener(view -> handleFABClick());
        setIsNightMode();

        setFieldReferencesToViewsAndSnackBar();
        setupBoard();
        doInitialStartGame(savedInstanceState);
    }

    private void setContentView() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        bottomBarAndFabBinding = MainIncludeActivityBottomBarAndFabBinding.bind(binding.getRoot());
        setContentView(binding.getRoot());
    }

    private void handleFABClick() {
        showInfoDialog(this, "Information", mCurrentGame.getRules());
    }

    private void setFieldReferencesToViewsAndSnackBar() {
        mSbContainer = findViewById(R.id.activity_main);
        mTv_cardsRemaining = findViewById(R.id.tv_cards_remaining_to_discard);
        mTv_cardsInDeck = findViewById(R.id.tv_cards_in_deck);
        mSnackbar = Snackbar.make(mSbContainer, R.string.welcome_new_game, Snackbar.LENGTH_SHORT);
    }

    private void setIsNightMode() {
        mIsNightMode = (getApplicationContext().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupBoard() {
        // create the adapter which will drive the RecyclerView (Model portion of MVC)
        mAdapter = new CardPilesAdapter(mIsNightMode, getString(R.string.cards_in_stack));

        // Set the listener object to respond to clicks in the RecyclerView
        // clicks are forwarded to the listener by the ViewHolder via the+ Adapter
        mAdapter.setOnItemClickListener(getNewOnItemClickListener());

        // mAdapter.setAnimationsEnabled(mPrefShowAnimations);

        // get a reference to the RecyclerView - not a field because it's not needed elsewhere
        RecyclerView rvPiles = findViewById(R.id.rv_piles);

        // Please note the use of an xml integer here: portrait==2x2, landscape/square==1x4; neat!
        final int RV_COLUMN_COUNT = getResources().getInteger(R.integer.rv_columns);

        // optimization setting - since there are always exactly four piles
        rvPiles.setHasFixedSize(true);

        // Create a new LayoutManager object to be used in the RecyclerView
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager
                (this, RV_COLUMN_COUNT);

        // apply the Layout Manager object just created to the RecyclerView
        rvPiles.setLayoutManager(layoutManager);

        // apply the adapter object to the RecyclerView
        rvPiles.setAdapter(mAdapter);
    }

    private void doInitialStartGame(Bundle savedInstanceState) {
        // If this is NOT the first run, meaning, we're recreating as a result of a device rotation
        // then restore the board (meaning both cards and user's checks) as from before the rotation
        // Otherwise (if this is a fresh start of the Activity and NOT after a rotation),
        // if auto-save is enabled then restore the game from sharedPrefs
        // otherwise (not post-rotation and auto-save off or no game in prefs): start a new game

        if (savedInstanceState != null) {
            restoreSavedGameAndBoardFromBundle(savedInstanceState);
        /*} else if (mPrefUseAutoSave && isValidGameInPrefs()) {
            restoreSavedGameAndBoardFromPrefs();*/
        } else {
            startNewGame();
        }
    }

    private void startNewGame() {
        startGameAndSetBoard(new PMGame(), null, R.string.welcome_new_game);
    }

    private void restoreSavedGameAndBoardFromBundle(Bundle savedInstanceState) {
        startGameAndSetBoard(restoreGameFromJSON(savedInstanceState.getString(mKeyGame)),
                savedInstanceState.getBooleanArray(mKeyCheckedPiles), 0);
    }

    private void startGameAndSetBoard(PMGame game, boolean[] checks, int msgID) {
        // create/restore the game
        mCurrentGame = game != null ? game : new PMGame();

        // update the board
        doUpdatesAfterGameStartOrTakingTurn();

        // overwrite checks if not null
        if (checks != null)
            mAdapter.overwriteChecksFrom(checks);

        // Show New Game message if non-zero
        if (msgID != 0) {
            mSnackbar = Snackbar.make(mSbContainer, msgID, Snackbar.LENGTH_SHORT);
            mSnackbar.show();
        }
    }

    private void doUpdatesAfterGameStartOrTakingTurn() {
        updateStatusBar();
        updateRecyclerViewAdapter();
        checkForGameOver();
    }

    private void updateStatusBar() {
        // Update the Status Bar with the number of cards left (from Java) via our current game obj
        mTv_cardsRemaining.setText(getString(R.string.cards_to_discard).concat
                (String.valueOf(mCurrentGame.getNumCardsLeftToDiscardFromDeckAndStacks())));

        mTv_cardsInDeck.setText(getString(R.string.in_deck).concat(
                String.valueOf(mCurrentGame.getNumberOfCardsLeftInDeck())));
    }

    private void updateRecyclerViewAdapter() {
        // get the data for the new board from our game object (Java) which tracks the four stacks
        Card[] currentTops = mCurrentGame.getCurrentStacksTopIncludingNulls();

        // temporary card used when updating the board below
        Card currentCard, currentAdapterCard;

        // Update the board one pile/card at a time, as needed
        for (int i = 0; i < currentTops.length; i++) {
            currentCard = currentTops[i];
            currentAdapterCard = mAdapter.getCardAt(i);

            if ((currentAdapterCard == null || !currentAdapterCard.equals(currentCard))) {
                // Have Adapter set each card to the matching top card of each stack
                mAdapter.updatePile(i, currentCard,
                        mCurrentGame.getNumberOfCardsInStackAtPosition(i));
            }

            // Clear the checks that the user might have just set
            mAdapter.clearCheck(i);
        }
    }

    /**
     * If the game is over, this method outputs a dialog box with the correct message (win/not)
     */
    private void checkForGameOver() {
        // If the game is over, let the user know what happened and then start a new game
        if (mCurrentGame.isGameOver()) {
            showGameOverDialog(getString(R.string.game_over),
                    mCurrentGame.isWinner() ? mWINNER_MSG : mNON_WINNER_MSG);
        }
    }


    /**
     * This anon implementation of our Listener interface handles adapter events
     * The object created here is passed to the adapter via the adapter's setter method
     * This object's onItemClick() is called from the Adapter when the user clicks on the board.
     * This leaves the Adapter to handle only the RV's data, and MainActivity to MVC Control...
     *
     * @return an object that responds to clicks inside a RecyclerView whose ViewHolder implements this interface
     */
    private AdapterOnItemClickListener getNewOnItemClickListener() {
        return (position, view) -> handleOnItemClick(position);
    }

    private void handleOnItemClick(int position) {
        try {
            if (mCurrentGame.getNumberOfCardsInStackAtPosition(position) > 0) {
                if (mCurrentGame.isGameOver()) {
                    showSB_AlreadyGameOver();
                } else {
                    dismissSnackBarIfShown();
                    mAdapter.toggleCheck(position);
                }
            }   // otherwise, if this stack is empty (and no card shown), then ignore the click
        } catch (Exception e) {
            Log.d("STACK", "Toggle Crashed: " + e.getMessage());
            // No known reason for this to cause a crash, but just in case...
        }
    }

    private void showSB_AlreadyGameOver() {

    }

    private void dismissSnackBarIfShown() {
        if (mSnackbar.isShown())
            mSnackbar.dismiss();
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}

        return super.onOptionsItemSelected(item);
    }


    /**
     * Shows an Android (nicer) equivalent to JOptionPane
     *
     * @param strTitle Title of the Dialog box
     * @param strMsg   Message (body) of the Dialog box
     */
    private void showGameOverDialog(String strTitle, String strMsg) {
        final DialogInterface.OnClickListener newGameListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startNewGame();
                    }
                };

        showYesNoDialog(this, strTitle, strMsg, newGameListener, null);
    }
}