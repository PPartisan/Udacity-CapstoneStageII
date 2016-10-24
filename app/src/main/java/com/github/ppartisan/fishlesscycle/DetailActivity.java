package com.github.ppartisan.fishlesscycle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public final class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {

            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, DetailFragment.newInstance())
                    .commit();

        }

    }

}