package com.gaiagps.iburn.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.gj.message.GjMessage;
import com.gaiagps.iburn.gj.message.GjMessageMode;
import com.gaiagps.iburn.gj.message.GjMessageText;

/**
 * Created by liorsaar on 4/19/15
 */
public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";

    private Spinner messageTypeSpinner;
    private Spinner messageModeSpinner;
    private EditText messageEditText;
    private TextView messageConsole;
    private Button messageSendButton;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTheme(R.style.Theme_GJ);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        messageTypeSpinner = (Spinner) view.findViewById(R.id.GjMessageTypeSpinner);
        messageTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                messageTypeOnItemSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        messageModeSpinner = (Spinner) view.findViewById(R.id.GjBufferModeSpinner);

        messageEditText = (EditText) view.findViewById(R.id.GjMessageEditText);
        messageEditText.setEnabled(false);

        messageSendButton = (Button) view.findViewById(R.id.GjMessageSendButton);
        messageSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageSendOnClick();
            }
        });

        messageConsole = (TextView) view.findViewById(R.id.GjMessageConsole);
        messageConsole.setMovementMethod(new ScrollingMovementMethod());
        return view;
    }

    ///////////////////////////////////////////////
    // SEND
    ///////////////////////////////////////////////

    private void messageTypeOnItemSelected(int position) {
        GjMessage.Type type = GjMessage.Type.values()[position];
        messageEditText.setEnabled(false);
        messageModeSpinner.setEnabled(false);
        switch (type) {
            case Text:
                messageEditText.setEnabled(true);
                break;
            case Mode:
                messageModeSpinner.setEnabled(true);
                break;
            case ReportGps:
            case RequestGps:
            case StatusRequest:
            case Lighting:
                break;
        }
    }

    private void messageSendOnClick() {
        int typePosition = messageTypeSpinner.getSelectedItemPosition();
        GjMessage.Type type = GjMessage.Type.values()[typePosition];
        switch (type) {
            case Mode:
                int modePosition = messageModeSpinner.getSelectedItemPosition();
                GjMessage.Mode mode = GjMessage.Mode.valueOf((byte)modePosition);
                messageSend(new GjMessageMode(mode));
                break;
            case ReportGps:
            case RequestGps:
            case StatusRequest:
                break;
            case Text:
                String text = messageEditText.getText().toString().trim();
                messageSend(new GjMessageText(text));
                break;
            case Lighting:
                break;
        }
    }

    private void messageSend(GjMessage message) {
        messageConsole.append(">>> " + message.toString() + "\n");
        messageConsole.append(">>> " + message.toHexString() + "\n");

        messageConsole.post(new Runnable() {
            @Override
            public void run() {
                final int scrollAmount = messageConsole.getLayout().getLineTop(messageConsole.getLineCount()) - messageConsole.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0)
                    messageConsole.scrollTo(0, scrollAmount);
                else
                    messageConsole.scrollTo(0, 0);
            }
        });
    }

}