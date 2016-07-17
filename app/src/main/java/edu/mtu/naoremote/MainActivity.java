package edu.mtu.naoremote;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

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
    private Spinner postureSelector, packageSelector;
    private CheckBox autoAnimate;
    private EditText textToSay;

    private static final int AUDIO_FILE_REQUEST_CODE = 4559;

    private String robotUrl = "141.219.121.74:9559";
    private String robotSSHUsername = "nao";
    private String robotSSHPassword = "";
    private int soundID = -1;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getPreferences(MODE_PRIVATE);

        //Get views
        autoAnimate = (CheckBox) findViewById(R.id.enableAutoGestures);
        textToSay = (EditText) findViewById(R.id.textToSay);
        postureSelector = (Spinner) findViewById(R.id.poseSpinner);
        packageSelector = (Spinner) findViewById(R.id.packageSpinner);
        say = (Button) findViewById(R.id.say);
        playSound = (Button) findViewById(R.id.playSample);
        stopSound = (Button) findViewById(R.id.stopSample);

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
        builder.setCancelable(false);

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

            autonomousMoves.setBackgroundStrategy("none");


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
            });

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
