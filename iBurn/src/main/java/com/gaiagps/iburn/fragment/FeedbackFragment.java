package com.gaiagps.iburn.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.gaiagps.iburn.R;

public class FeedbackFragment extends Fragment {

    EditText feedbackView;

    public static FeedbackFragment newInstance() {
        FeedbackFragment fragment = new FeedbackFragment();
        return fragment;
    }

    public FeedbackFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_feedback, container, false);
        feedbackView = (EditText) root.findViewById(R.id.feedback);

        root.findViewById(R.id.feedbackButton).setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto","support+42fb65d77b094805a8b42ac4a6e6a571@feedback.hockeyapp.net", null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "iBurn-Android feedback");
            emailIntent.putExtra(Intent.EXTRA_TEXT, feedbackView.getText());
            startActivity(Intent.createChooser(emailIntent, "Email Feedback"));
            feedbackView.setText("");
        });

        return root;
    }


}
