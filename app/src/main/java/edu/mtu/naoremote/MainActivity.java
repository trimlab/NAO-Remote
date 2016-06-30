package edu.mtu.naoremote;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.aldebaran.qi.Application;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.Tuple10;
import com.aldebaran.qi.Tuple2;
import com.aldebaran.qi.Tuple20;
import com.aldebaran.qi.Tuple3;
import com.aldebaran.qi.Tuple5;
import com.aldebaran.qi.helper.proxies.ALAnimatedSpeech;
import com.aldebaran.qi.helper.proxies.ALAudioPlayer;
import com.aldebaran.qi.helper.proxies.ALMotion;
import com.aldebaran.qi.helper.proxies.ALRobotPosture;
import com.aldebaran.qi.helper.proxies.ALTextToSpeech;
import com.aldebaran.qi.helper.proxies.PackageManager;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity
{
    private Session session;
    private ALTextToSpeech tts;
    private ALMotion motion;
    private ALRobotPosture posture;
    private ALAnimatedSpeech animatedSpeech;
    private ALAudioPlayer audioPlayer;
    private PackageManager packageManager;

    private Button say, playSound, stopSound;
    private Spinner postureSelector, packageSelector;
    private CheckBox autoAnimate;
    private EditText textToSay;

    private static final int AUDIO_FILE_REQUEST_CODE = 4559;

    private String robotUrl = "141.219.121.74:9559";
    private int soundID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View v = getLayoutInflater().inflate(R.layout.dialog_pickip, null, false);
        EditText ipAddress = (EditText) v.findViewById(R.id.ipAddress);
        ipAddress.setText(robotUrl);

        builder.setView(v);
        builder.setCancelable(false);

        builder.setMessage("Enter NAO's IP Address")
                .setTitle("IP Address");

        builder.setPositiveButton("Connect", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Dialog d = (Dialog) dialog;
                EditText ipAddress = (EditText) d.findViewById(R.id.ipAddress);

                robotUrl = ipAddress.getText().toString();
                init();
            }
        });

        builder.show();


        autoAnimate = (CheckBox) findViewById(R.id.enableAutoGestures);
        textToSay = (EditText) findViewById(R.id.textToSay);
        postureSelector = (Spinner) findViewById(R.id.poseSpinner);
        packageSelector = (Spinner) findViewById(R.id.packageSpinner);
        say = (Button) findViewById(R.id.say);
        playSound = (Button) findViewById(R.id.playSample);
        stopSound = (Button) findViewById(R.id.stopSample);
    }

    private void init()
    {

        try
        {
            session = new Session();
            session.connect(robotUrl).get();
            tts = new ALTextToSpeech(session);
            motion = new ALMotion(session);
            posture = new ALRobotPosture(session);
            animatedSpeech = new ALAnimatedSpeech(session);
            packageManager = new PackageManager(session);
            audioPlayer = new ALAudioPlayer(session);

            animatedSpeech.setBodyLanguageModeFromStr("contextual");

            say.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    try
                    {
                        String text = textToSay.getText().toString();
                        if (autoAnimate.isChecked())
                            animatedSpeech.say(text);
                        else
                            tts.say(text);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });

            List<String> postures = posture.getPostureList();
            ArrayAdapter<String> postureAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, (String[]) postures.toArray());
            postureSelector.setAdapter(postureAdapter);

            postureSelector.setSelection(Collections.binarySearch(postures, posture.getPosture()));

            postureSelector.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    try
                    {
                        posture.goToPosture((String) parent.getAdapter().getItem(position), 0.5f);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });

            playSound.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    try
                    {
                        if(soundID != -1 && audioPlayer.isRunning(soundID));
                        {
                            audioPlayer.stop(soundID);
                        }

                        Intent pickFile = new Intent(Intent.ACTION_GET_CONTENT);
                        pickFile.setType("file/*");
                        startActivityForResult(pickFile, AUDIO_FILE_REQUEST_CODE);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });

            stopSound.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    try
                    {
                        if(soundID != -1 && audioPlayer.isRunning(soundID));
                        {
                            audioPlayer.stop(soundID);
                        }
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            });

            List<String> packages = (List<String>) packageManager.getPackages();
            ArrayAdapter<String> packagesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, packages);
            packageSelector.setAdapter(packagesAdapter);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == AUDIO_FILE_REQUEST_CODE)
        {
            String path = data.getData().getPath();

            try
            {
                audioPlayer.playFile(path);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }
}
