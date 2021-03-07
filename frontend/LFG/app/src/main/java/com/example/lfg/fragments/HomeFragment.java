package com.example.lfg.fragments;

import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.lfg.R;
import com.example.lfg.adapters.PostsAdapter;
import com.example.lfg.interfaces.ItemClickListener;
import com.example.lfg.models.Post;
import com.example.lfg.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;


public class HomeFragment extends Fragment {
    public static final String TAG = "HomeFragment";
    private RecyclerView rvPosts;
    private PostsAdapter postsAdapter;
    private FirebaseDatabase database;
    SharedPreferences prefs;
    SharedPreferences.Editor edit;
    String scrollID;
    int index;

    List<Post> posts;

    public HomeFragment() {
        // Required empty public constructor
        scrollID = null;
        index = 0;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvPosts = view.findViewById(R.id.rvPosts);
        posts = new ArrayList<>();

        prefs = getActivity().getSharedPreferences("data", MODE_PRIVATE);
        edit = prefs.edit();

        ItemClickListener itemClickListener = new ItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                final FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                Post post = posts.get(position);
                User postUser = post.getAuthor();
                Fragment fragment = new PostFragment(post, postUser);
                fragmentManager.beginTransaction().replace(R.id.flContainer, fragment).addToBackStack("home").commit();

                //TODO: show post details
            }
        };
        postsAdapter = new PostsAdapter(getContext(), posts, itemClickListener);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPosts.setAdapter(postsAdapter);
        database = FirebaseDatabase.getInstance();
        loadData();
    }



    private void loadData(){
        database.goOnline();

        DatabaseReference ref = database.getReference("posts");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                posts.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    User tempUser = new User();
                    Log.i("get", child.getKey());
                    String game = (String) child.child("game").getValue();
                    int size = (int) ((long) child.child("size").getValue());
                    long time = (long) child.child("timestamp").getValue();
                    long timer = (long) child.child("timer").getValue();
                    String logo = (String) child.child("logoName").getValue();
                    String userId = (String) child.child("user").getValue();
                    String id = (String) child.child("id").getValue();
                    Post currPost = new Post(game, size, logo, time + timer);
                    String body = (String) child.child("body").getValue();
                    currPost.setBody(body);
                    currPost.setId(id);
                    currPost.setUser(userId);
                    DataSnapshot commentChild = child.child("comments");
                    if(commentChild != null) {
                        Log.i("comments", "success");
                        List<String> commentIds = new ArrayList<>();
                        for (DataSnapshot currComment : commentChild.getChildren()) {
                            String commentId = (String) currComment.child("id").getValue();
                            Log.i("comments", commentId);
                            commentIds.add(commentId);
                        }
                        currPost.setComments(commentIds);
                    }
                    Date postTimestamp = new Date(time + timer);
                    Log.i("TIME", String.valueOf(time));
                    long currentTimestamp = System.currentTimeMillis();
                    Date currentTime = new Date(currentTimestamp);
                    if (postTimestamp.before(currentTime)) {
                        String postId = child.getKey();
                        ref.child(postId).removeValue();
                        database.getReference("users").child(userId).child("posts").child(postId).removeValue();
                        if(currPost.getComments() != null) {
                            for (String commentId : currPost.getComments()) {
                                database.getReference("comments").child(commentId).removeValue();
                            }
                        }
                        Log.i("expired", "timestamp: " + postTimestamp.toString());
                        Log.i("expired", "current time: " + currentTime.toString());
                        //posts.add(currPost);
                    } else {
                        Log.i("active", game + " timestamp: " + postTimestamp.toString());
                        Log.i("active", game + " current time: " + currentTime.toString());
                        /* for use later in ProfileFragment
                        String username = prefs.getString("username", null);
                        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                         if (username == null)
                            Toast.makeText(getContext(), "username not saved", Toast.LENGTH_SHORT).show();
                        User postUser = new User(userId, username, email);
                        currPost.setAuthor(postUser);
                        currPost.setId(child.getKey());
                        Log.i("POST", child.getKey());*/
                        posts.add(currPost);

                    }


                    //currPost.setLogoName("fortnite_logo.png");

                }

                Collections.sort(posts, new Sortbytime());
                postsAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "The read failed: " + databaseError.getCode());
            }
        });
    }

    class Sortbytime implements Comparator<Post>{

        @Override
        public int compare(Post a, Post b) {
            return (int) (a.getTimeEnd() - b.getTimeEnd());
        }
    }

}