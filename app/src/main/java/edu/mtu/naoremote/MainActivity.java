package edu.mtu.naoremote;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.proxies.ALAnimatedSpeech;
import com.aldebaran.qi.helper.proxies.ALMotion;
import com.aldebaran.qi.helper.proxies.ALRobotPosture;
import com.aldebaran.qi.helper.proxies.ALTextToSpeech;

public class MainActivity extends AppCompatActivity
{
    private Session session;
    private ALTextToSpeech tts;
    private ALMotion motion;
    private ALRobotPosture posture;
    private ALAnimatedSpeech animatedSpeech;

    private Button say;
    private Spinner postureSelector;
    private CheckBox autoAnimate;
    private EditText textToSay;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String robotUrl = "141.219.121.74:9559";
        try
        {
            session = new Session();
            session.connect(robotUrl).get();
            tts = new ALTextToSpeech(session);
            motion = new ALMotion(session);
            posture = new ALRobotPosture(session);
            animatedSpeech = new ALAnimatedSpeech(session);
            animatedSpeech.setBodyLanguageModeFromStr("contextual");


            autoAnimate = (CheckBox) findViewById(R.id.enableAutoGestures);
            textToSay = (EditText) findViewById(R.id.textToSay);

            say = (Button) findViewById(R.id.say);
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
                    } catch (Exception e)
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
}
