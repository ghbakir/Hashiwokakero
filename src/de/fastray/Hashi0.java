package de.fastray;


import de.fastray.R;
import de.fastray.HashiGame;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

/** Called when the activity is first created. */
public class Hashi0 extends Activity implements OnClickListener {

  @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      // Set up click listeners for all the buttons
      View continueButton = findViewById(R.id.continue_button);
      continueButton.setOnClickListener(this);

      View newButton = findViewById(R.id.new_button);
      newButton.setOnClickListener(this);

      View aboutButton = findViewById(R.id.about_button);
      aboutButton.setOnClickListener(this);

      View exitButton = findViewById(R.id.exit_button);
      exitButton.setOnClickListener(this);

    }

  // click handling
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.exit_button:
	finish();
	break;
      case R.id.new_button:
	NewGame();
	break;
    }
  }


  // Create a new game.
  public void NewGame() {
    // We first ask for the difficulty level.
    new AlertDialog.Builder(this)
      .setTitle(R.string.new_game_title)
      // we provide a char array with the on click listener.
      .setItems(R.array.difficulty,
	  new DialogInterface.OnClickListener() {
	    public void onClick(DialogInterface dialoginterface, int hardness) {
	      Intent intent = new Intent(Hashi0.this, HashiGame.class);
	      intent.putExtra(HashiGame.KEY_DIFFICULTY, hardness);
	      startActivity(intent);
	    }
	  })
    .show();
  }
}
