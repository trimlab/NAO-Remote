package edu.mtu.naoremote;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.ALProxy;
import com.aldebaran.qi.helper.proxies.ALAnimatedSpeech;
import com.aldebaran.qi.helper.proxies.ALMotion;
import com.aldebaran.qi.helper.proxies.ALRobotPosture;
import com.aldebaran.qi.helper.proxies.ALTextToSpeech;

import java.util.Collections;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String robotUrl = "141.219.124.110:9559";
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

            List<String> postures = posture.getPostureList();

            say = (Button) findViewById(R.id.say);
            say.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    try
                    {
                        String text = "Hello! My name is Nao. Nice to meet you!";
                        if(autoAnimate.isChecked())
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

            postureSelector = (Spinner) findViewById(R.id.poseSpinner);

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
                        //posture.applyPosture((String) parent.getAdapter().getItem(position), 1.0f);
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
}
