package ru.phsystems.irisx;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 11.09.12
 * Time: 19:11
 * License: GPLv3
 *
 * Small applet that can play WAV-stream from HTTP
 */


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class Play extends Applet implements ActionListener, Runnable{

    Button play;
    Thread th;
    String url = null;
    boolean playing = false;

    public void init(){

        play = new Button("|>");
        add(play);
        play.addActionListener(this);
    }

    public void run ()
    {
        //Just to be nice, lower this thread's priority
        //so it can't interfere with other processing going on.
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        //Remember the starting time.
        long startTime = System.currentTimeMillis();
    }

    public void actionPerformed(ActionEvent ae){
        Button source = (Button)ae.getSource();

        if (source.getLabel() == "|>"){

            url = getParameter("url");

            if(!playing)
            {
                playing = true;
                play.setLabel("[ ]");
                th = new Thread(new Audio(url));
                th.start();
            }

        }
        else if(source.getLabel() == "[ ]"){

            System.out.println("STOP!");
            th.interrupt();
            play.setLabel("|>");
            playing = false;
        }
    }

    public void destroy()
    {
        th.interrupt();
        th = null;
    }

    private class Audio extends Thread implements Runnable
    {
        int ExternalBufferSize = 64000;
        int InternalBufferSize = 64000;

        SourceDataLine line = null;
        String sTmp;
        Socket m_sktSound;
        DataInputStream m_soundInput;
        DataOutputStream m_soundOutput;
        int nRead = 0;
        int retry = 0;
        String addr = null;

        byte[] rData = new byte[this.ExternalBufferSize];
        byte[] m_GetSoundString;

        public Audio(String addrUrl)
        {
            addr = addrUrl;
        }

        public void run()
        {
            this.sTmp = new String("GET "+addr+" HTTP/1.0\r\nUser-Agent: user");
            this.sTmp = this.sTmp.concat("\r\n\r\n");

            System.out.println("PLAY! "+this.sTmp);

            try
            {



                Thread.currentThread(); Thread.sleep(100L);
                this.m_GetSoundString = this.sTmp.getBytes("8859_1");
                this.m_sktSound = new Socket(getCodeBase().getHost(), getCodeBase().getPort());

                this.m_sktSound.setKeepAlive(true);
                this.m_soundInput = new DataInputStream(this.m_sktSound.getInputStream());
                this.m_soundOutput = new DataOutputStream(this.m_sktSound.getOutputStream());
                this.m_soundOutput.write(this.m_GetSoundString);

                this.nRead = 0;
                int i = 0;
                int m;
                do { this.nRead += this.m_soundInput.read(this.rData, this.nRead, this.rData.length - this.nRead);
                    for (m = 0; m < this.nRead - 7; m++)
                    {
                        if ((this.rData[m] == 13) && (this.rData[(m + 1)] == 10) && (this.rData[(m + 2)] == 13) && (this.rData[(m + 3)] == 10) && (this.rData[(m + 4)] == 82) && (this.rData[(m + 5)] == 73) && (this.rData[(m + 6)] == 70) && (this.rData[(m + 7)] == 70))
                        {
                            i = 1;
                            m += 4;
                            break;
                        }
                    }
                    Thread.currentThread(); Thread.sleep(3L); }
                while ((this.nRead != -1) && (i == 0));

                long l = this.rData[(m + 24)] + (this.rData[(m + 25)] << 8) + (this.rData[(m + 26)] << 16) + (this.rData[(m + 27)] << 24);

                int j = this.rData[(m + 22)] + (this.rData[(m + 23)] << 8);
                int k = this.rData[(m + 34)] + (this.rData[(m + 35)] << 8);

                m += 44;

                AudioFormat localAudioFormat = new AudioFormat((float)l, k, j, true, false);

                DataLine.Info localInfo = new DataLine.Info(SourceDataLine.class, localAudioFormat, this.InternalBufferSize);

                this.line = ((SourceDataLine) AudioSystem.getLine(localInfo));
                this.line.open(localAudioFormat, this.InternalBufferSize);
                this.line.start();
                int n = this.line.getBufferSize();

                this.line.write(this.rData, m, this.nRead - m);
                this.nRead = 0;
                int i1 = j * 256;

                this.retry = 0;
                while (this.nRead != -1)
                {
                    Thread.currentThread(); Thread.sleep(8L);
                    this.nRead = this.m_soundInput.read(this.rData, 0, this.rData.length);
                    if ((this.nRead > 0) &&
                            ((n - this.line.available()) / i1 < 50))
                    {
                        if (this.line.available() >= this.nRead)
                            this.line.write(this.rData, 0, this.nRead);
                        else {
                            this.line.flush();
                        }

                    }

                }

            }
            catch (Exception localException2)
            {
                System.err.println(localException2);
            }

            try
            {
                this.line.flush();
                this.line.stop();
                this.line.close();

                this.m_soundInput.close();
                this.m_soundOutput.close();
                this.m_sktSound.close();
            }
            catch (Exception localException3)
            {
                System.err.println(localException3);
            }
        }
    }
}



