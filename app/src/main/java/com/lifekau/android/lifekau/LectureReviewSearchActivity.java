package com.lifekau.android.lifekau;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LectureReviewSearchActivity extends AppCompatActivity {

    private AutoCompleteTextView mAutoCompleteSearchBar;

    public static Intent newIntent(Context context){
        Intent intent = new Intent(context, LectureReviewSearchActivity.class);
        return intent;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecture_review_search);

        mAutoCompleteSearchBar = (AutoCompleteTextView)findViewById(R.id.lecture_review_search_bar);

        List<String> list = new ArrayList<>(Arrays.asList("가 나", "가나", "가나다"));
        int layoutItemId = android.R.layout.simple_dropdown_item_1line;
        LectureSearchAdapter adapter = new LectureSearchAdapter(this, layoutItemId, list);
        mAutoCompleteSearchBar.setAdapter(adapter);
    }
}