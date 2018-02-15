package com.lifekau.android.lifekau.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.lifekau.android.lifekau.PxDpConverter;
import com.lifekau.android.lifekau.R;
import com.lifekau.android.lifekau.adapter.CommentRecyclerAdapter;
import com.lifekau.android.lifekau.manager.LoginManager;
import com.lifekau.android.lifekau.model.Comment;
import com.lifekau.android.lifekau.model.Post;
import com.lifekau.android.lifekau.viewholder.PostViewHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity implements OnClickListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String EXTRA_HAS_CLICKED_COMMENT = "extra_has_clicked_comment";
    private static String EXTRA_POST_KEY = "extra_post_key";
    private final int NUM_OF_COMMENT_PER_PAGE = 5;
    private int mTotalItemCount = 0;
    private int mLastVisibleItemPosition;
    private boolean mIsLoading;
    private LinearLayout mPostContainer;
    private View mBottomMarginView;
    private TextView mTextView;
    private PostViewHolder mPostViewHolder;
    private String mPostKey;
    private DatabaseReference mPostRef;
    private DatabaseReference mCommentsRef;
    private RecyclerView mRecyclerView;
    private CommentRecyclerAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private ImageView mCommentSubmitImageView;
    private EditText mCommentEditText;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private NestedScrollView mNestedScrollView;
    private ImageView mMoreCommentsImageView;
    private TextView mMoreCommentsTextView;
    private ProgressBar mMoreCommentsProgressBar;
    private LinearLayout mMoreCommentsLinearLayout;
    private boolean mJustWroteComment;


    public static Intent newIntent(Context context, String postKey, boolean hasClickedComment) {
        Intent intent = new Intent(context, PostDetailActivity.class);
        intent.putExtra(EXTRA_POST_KEY, postKey);
        intent.putExtra(EXTRA_HAS_CLICKED_COMMENT, hasClickedComment);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mPostKey = getIntent().getStringExtra(EXTRA_POST_KEY);

        initializeViews();

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        mPostRef = database
                .child(getString(R.string.firebase_database_posts))
                .child(mPostKey);
        mCommentsRef = database
                .child(getString(R.string.firebase_database_post_comments))
                .child(mPostKey);

        mBottomMarginView.setVisibility(View.GONE);
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setItemAnimator(null);
        mNestedScrollView.setSmoothScrollingEnabled(true);
        mMoreCommentsImageView.setOnClickListener(this);
        mMoreCommentsTextView.setOnClickListener(this);
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mCommentSubmitImageView.setOnClickListener(this);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressViewOffset(true, PxDpConverter.convertDpToPx(72), PxDpConverter.convertDpToPx(100));
        updateUI();
    }

    private void getComments() {
        Query query = mCommentsRef.orderByKey();
        if (mAdapter.getItemCount() != 0) query = query.endAt(mAdapter.getLastKey());
        query = query.limitToLast(NUM_OF_COMMENT_PER_PAGE + 1);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Comment> newComments = new ArrayList<>();
                List<String> newCommentKeys = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    newComments.add(snapshot.getValue(Comment.class));
                    newCommentKeys.add(snapshot.getKey());
                }
                if (newCommentKeys.size() <= NUM_OF_COMMENT_PER_PAGE) {
                    mMoreCommentsLinearLayout.setVisibility(View.GONE);
                } else {
                    mMoreCommentsLinearLayout.setVisibility(View.VISIBLE);

                    newComments = newComments.subList(newComments.size() - NUM_OF_COMMENT_PER_PAGE, newComments.size());
                    newCommentKeys = newCommentKeys.subList(newCommentKeys.size() - NUM_OF_COMMENT_PER_PAGE, newCommentKeys.size());
                }

                if (mAdapter.getItemCount() != 0) {
                    newComments.remove(newComments.size() - 1);
                    newCommentKeys.remove(newCommentKeys.size() - 1);
                }
                Collections.reverse(newComments);
                Collections.reverse(newCommentKeys);

                mAdapter.addAll(newComments, newCommentKeys);
                mIsLoading = false;

                mMoreCommentsProgressBar.setVisibility(View.GONE);
                mMoreCommentsImageView.setVisibility(View.VISIBLE);
                mMoreCommentsTextView.setText(R.string.see_previous_comments);

                if (mJustWroteComment) {
                    mJustWroteComment = false;
                    mNestedScrollView.fullScroll(View.FOCUS_DOWN);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("fuck", "PostDetailActivity:onCancelled");
                mJustWroteComment = false;
            }
        });
    }

    private void updatePost() {
        mPostRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("fuck", "PostDetailActivity:onDataChange");
                Post post = dataSnapshot.getValue(Post.class);
                mPostViewHolder.bind(post, mPostKey);
                mPostViewHolder.mCommentButtonContainer.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mCommentEditText.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(mCommentEditText, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
                mPostViewHolder.mTextView.setOnClickListener(null);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("fuck", "PostDetailActivity:onCancelled");
            }
        });
    }

    private void updateComments() {
        mAdapter = new CommentRecyclerAdapter(mPostKey, this);
        mRecyclerView.setAdapter(mAdapter);
        mIsLoading = true;
        getComments();
    }

    private void updateUI() {
//        mProgressBar.setVisibility(View.VISIBLE);
//        mRecyclerView.setVisibility(View.GONE);
        updateComments();
        updatePost();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void initializeViews() {
        mMoreCommentsLinearLayout = findViewById(R.id.more_comments_linear_layout);
        mMoreCommentsProgressBar = findViewById(R.id.more_comments_progress_bar);
        mMoreCommentsTextView = findViewById(R.id.more_comments_text_view);
        mMoreCommentsImageView = findViewById(R.id.more_comments_refresh_image_view);
        mNestedScrollView = findViewById(R.id.post_detail_nested_scroll_view);
        mCommentSubmitImageView = findViewById(R.id.post_detail_comment_send_image_view);
        mCommentEditText = findViewById(R.id.post_detail_comment_edit_text);
        mRecyclerView = findViewById(R.id.post_detail_recycler_view);
        mPostContainer = findViewById(R.id.post_detail_post_container);
        mPostViewHolder = new PostViewHolder(mPostContainer, this);
        mBottomMarginView = mPostContainer.findViewById(R.id.list_item_post_bottom_margin_view);
        mSwipeRefreshLayout = findViewById(R.id.post_detail_swipe_refresh_layout);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.post_detail_comment_send_image_view:
                writeComment(new Comment(LoginManager.get(this).getStudentId(), mCommentEditText.getText().toString()));
                break;
            case R.id.more_comments_refresh_image_view:
                pressedMoreComments();
                break;
            case R.id.more_comments_text_view:
                pressedMoreComments();
                break;
        }
    }

    private void pressedMoreComments() {
        mMoreCommentsImageView.setVisibility(View.GONE);
        mMoreCommentsProgressBar.setVisibility(View.VISIBLE);
        mMoreCommentsTextView.setText(R.string.reading_more_comments);
        getComments();
    }

    private void writeComment(Comment comment) {
        mCommentsRef.push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                mJustWroteComment = true;
                updateComments();
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mCommentEditText.getWindowToken(), 0);

                mCommentEditText.setText("");
                Toast.makeText(PostDetailActivity.this,
                        getString(R.string.successfully_comment_registered),
                        Toast.LENGTH_SHORT).
                        show();
            }
        });
    }

    @Override
    public void onRefresh() {
        if (mIsLoading) return;
        updateUI();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 500);
    }
//    private void getMoreComments() {
//        mIsLoading = true;
//        getComments();
//    }
}
