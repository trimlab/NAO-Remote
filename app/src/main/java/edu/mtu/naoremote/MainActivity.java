package edu.mtu.naoremote;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.aldebaran.qi.CallError;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.proxies.ALAnimatedSpeech;
import com.aldebaran.qi.helper.proxies.ALAudioPlayer;
import com.aldebaran.qi.helper.proxies.ALAutonomousLife;
import com.aldebaran.qi.helper.proxies.ALAutonomousMoves;
import com.aldebaran.qi.helper.proxies.ALMotion;
import com.aldebaran.qi.helper.proxies.ALRobotPosture;
import com.aldebaran.qi.helper.proxies.ALTextToSpeech;
import com.aldebaran.qi.helper.proxies.PackageManager;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.UserInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private Session session;
    private ALTextToSpeech tts;
    private ALMotion motion;
    private ALRobotPosture posture;
    private ALAnimatedSpeech animatedSpeech;
    private ALAudioPlayer audioPlayer;
    private ALAutonomousLife autonomousLife;
    private ALAutonomousMoves autonomousMoves;
    private PackageManager packageManager;

    private JSch jSch;
    private com.jcraft.jsch.Session sshSession;

    private Button say, playSound, stopSound;
    private Button addGesture, changePitch, changeRate, changeVolume, addPause;
    private Spinner postureSelector;
    private RadioButton noAnimation, contextAnimation, manualAnimation;
    private EditText textToSay;

    private static final int AUDIO_FILE_REQUEST_CODE = 4559;

    private String robotUrl = "141.219.121.74:9559";
    private String robotSSHUsername = "nao";
    private String robotSSHPassword = "";
    private int soundID = -1;

    private SharedPreferences preferences;

    private View.OnClickListener ttsListener = new View.OnClickListener()
    {
        String textToAdd = "";

        @Override
        public void onClick(View v)
        {
            switch(v.getId())
            {
                case R.id.changePitch:
                    textToAdd = "\\\\vct=\\\\";
                    break;
                case R.id.changeRate:
                    textToAdd = "\\\\rspd=\\\\";
                    break;
                case R.id.changeVolume:
                    textToAdd = "\\\\vol=\\\\";
                    break;
                case R.id.addPause:
                    textToAdd = "\\\\pau=\\\\";
                    break;
            }

            textToSay.getText().insert(textToSay.getSelectionStart(), textToAdd);
            textToSay.setSelection(textToSay.getSelectionEnd()-2);

            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(textToSay, 0);
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getPreferences(MODE_PRIVATE);

        //Get views
        noAnimation = (RadioButton) findViewById(R.id.noGestures);
        contextAnimation = (RadioButton) findViewById(R.id.contextGestures);
        manualAnimation = (RadioButton) findViewById(R.id.manualGestures);

        say = (Button) findViewById(R.id.say);
        playSound = (Button) findViewById(R.id.playSample);
        stopSound = (Button) findViewById(R.id.stopSample);
        addGesture = (Button) findViewById(R.id.addGesture);
        changePitch = (Button) findViewById(R.id.changePitch);
        changeRate = (Button) findViewById(R.id.changeRate);
        changeVolume = (Button) findViewById(R.id.changeVolume);
        addPause = (Button) findViewById(R.id.addPause);

        textToSay = (EditText) findViewById(R.id.textToSay);

        changePitch.setOnClickListener(ttsListener);
        changeRate.setOnClickListener(ttsListener);
        changeVolume.setOnClickListener(ttsListener);
        addPause.setOnClickListener(ttsListener);

        //Custom listener for adding gestures
        addGesture.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_gesture, null, false);

                Spinner gestures = (Spinner) dialogView.findViewById(R.id.gestureList);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_list_item_1, new String[]{"affirmative","alright","beg",
                        "beseech","body language","bow","call","clear","enthusiastic","entreat","explain","happy",
                        "hello","hey","hi","I","implore","indicate","me","my","myself","negative","no","not know",
                        "ok","oppose","please","present","rapturous","raring","refute","reject","rousing","show",
                        "supplicate","unacquainted","undetermined","undiscovered","unfamiliar","unknown","warm",
                        "yeah","yes","yoo-hoo","you","your","zestful"});

                gestures.setAdapter(adapter);

                dialog.setView(dialogView);
                dialog.setTitle("Add Gesture");

                dialog.setPositiveButton("Insert", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Spinner gestures = (Spinner) ((Dialog) dialog).findViewById(R.id.gestureList);
                        textToSay.getText().insert(textToSay.getSelectionEnd(), "^startTag(" + gestures.getSelectedItem() + ")");

                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.showSoftInput(textToSay, 0);
                    }
                });
                dialog.show();
            }
        });

        manualAnimation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if(isChecked)
                {
                    addGesture.setVisibility(View.VISIBLE);

                    try
                    {
                        animatedSpeech.setBodyLanguageModeFromStr("contextual");
                    }
                    catch (CallError callError)
                    {
                        callError.printStackTrace();
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    addGesture.setVisibility(View.GONE);
                }
            }
        });

        noAnimation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                try
                {
                    if (isChecked)
                    {
                        animatedSpeech.setBodyLanguageModeFromStr("none");
                    }
                    else
                    {
                        animatedSpeech.setBodyLanguageModeFromStr("contextual");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        /*postureSelector = (Spinner) findViewById(R.id.poseSpinner);
        packageSelector = (Spinner) findViewById(R.id.packageSpinner);*/

        connectionDialog();
    }

    private void connectionDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View v = getLayoutInflater().inflate(R.layout.dialog_pickip, null, false);
        final EditText ipAddress = (EditText) v.findViewById(R.id.ipAddress);
        final EditText sshUname = (EditText) v.findViewById(R.id.sshUsername);
        final EditText sshPass = (EditText) v.findViewById(R.id.sshPassword);
        final CheckBox savePassword = (CheckBox) v.findViewById(R.id.savePassword);
        final CheckBox showPassword =  (CheckBox) v.findViewById(R.id.showPassword);

        showPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {

                if(isChecked)
                {
                    sshPass.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    sshPass.setSelection(sshPass.length());
                }
                else
                {
                    sshPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    sshPass.setSelection(sshPass.length());
                }
            }
        });

        ipAddress.setText(robotUrl);
        sshUname.setText(robotSSHUsername);

        if(preferences.contains("savedPassword"))
        {
            sshPass.setText(preferences.getString("savedPassword", ""));
            savePassword.setChecked(true);
        }

        builder.setView(v);
        //builder.setCancelable(false);

        builder.setTitle("Connect to NAO");

        builder.setPositiveButton("Connect", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {

                robotUrl = ipAddress.getText().toString();
                robotSSHUsername = sshUname.getText().toString();
                robotSSHPassword = sshPass.getText().toString();

                if(savePassword.isChecked())
                    preferences.edit().putString("savedPassword", robotSSHPassword).apply();
                else
                {
                    if(preferences.contains("savedPassword"))
                        preferences.edit().remove("savedPassword").apply();
                }

                init();
            }
        });

        builder.show();
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
            autonomousMoves = new ALAutonomousMoves(session);

            //autonomousMoves.setBackgroundStrategy("none");


            /*jSch = new JSch();
            sshSession = jSch.getSession("nao", String.valueOf(robotUrl), 22);
            UserInfo info = new UserInfo()
            {
                @Override
                public String getPassphrase()
                {
                    return null;
                }

                @Override
                public String getPassword()
                {
                    return robotSSHPassword;
                }

                @Override
                public boolean promptPassword(String message)
                {
                    return true;
                }

                @Override
                public boolean promptPassphrase(String message)
                {
                    return false;
                }

                @Override
                public boolean promptYesNo(String message)
                {
                    return false;
                }

                @Override
                public void showMessage(String message)
                {
                    Log.d("SshMessage", message);
                }
            };

            sshSession.setUserInfo(info);
            sshSession.connect();

            if(sshSession.isConnected())*/

            animatedSpeech.setBodyLanguageModeFromStr("contextual");

            say.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    try
                    {
                        String text = textToSay.getText().toString();
                        if (!noAnimation.isChecked())
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

            /*List<String> postures = posture.getPostureList();
            ArrayAdapter<String> postureAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, (String[]) postures.toArray());
            postureSelector.setAdapter(postureAdapter);

            postureSelector.setSelection(Collections.binarySearch(postures, posture.getPosture()));

            postureSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
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

                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {

                }
            });*/

            playSound.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    try
                    {
                        /*if(soundID != -1 && audioPlayer.isRunning(soundID))
                        {
                            audioPlayer.stop(soundID);
                        }*/

                        Intent pickFile = new Intent(Intent.ACTION_GET_CONTENT);
                        pickFile.setType("audio/*");
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
                        if(soundID != -1 && audioPlayer.isRunning(soundID))
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
            if(resultCode == RESULT_OK)
            {
                try
                {
                    File toSend = new File(data.getData().getPath());
                    String receiveCommand = "scp -p -t " + toSend.getName();
                    Channel channel = sshSession.openChannel("exec");
                    ((ChannelExec) channel).setCommand(receiveCommand);

                    OutputStream out = channel.getOutputStream();
                    InputStream in = channel.getInputStream();

                    channel.connect();

                    //Date last modified
                    out.write( ("T " + toSend.lastModified()/1000 + "0\n").getBytes() );
                    out.flush();

                    //File Size
                    out.write(("C0644 " + toSend.length() + "\n").getBytes());
                    out.flush();

                    //Send File
                    FileInputStream fis = new FileInputStream(toSend);
                    byte[] buffer = new byte[1024];

                    while(true)
                    {
                        int len = fis.read(buffer, 0, buffer.length);

                        if(len <= 0)
                            break;

                        out.write(buffer, 0, len);
                    }

                    fis.close();

                    //Null terminator
                    buffer[0] = 0;
                    out.write(buffer, 0, 1);
                    out.flush();
                    out.close();

                    //Disconnect from exec channel
                    channel.disconnect();



                    //audioPlayer.playFile(path);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }
}
