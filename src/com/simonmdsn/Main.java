package com.simonmdsn;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        try {
            List<Process> pacmd = ProcessBuilder.startPipeline(List.of(new ProcessBuilder("pacmd", "list-sinks").inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE), new ProcessBuilder("grep", "-e", "index", "-e", "alsa.card_name =")));
            try (Scanner s = new Scanner(pacmd.get(pacmd.size() - 1).getInputStream())) {
                List<String> result = s.useDelimiter("\\R").tokens().toList();
                System.out.println("Available sinks");
                for (String s1 : result) {
                    System.out.println(s1);
                }
                List<AudioOutput> outputs = new ArrayList<>();
                for (int i = 0; i < result.size(); i++) {
                    AudioOutput audioOutput = new AudioOutput(result.get(i).contains("*"), result.get(i).split(": ")[1], result.get(++i).split("alsa.card_name = ")[1]);
                    outputs.add(audioOutput);
                }
                Optional<AudioOutput> active = outputs.stream().filter(AudioOutput::activated).findFirst();
                active.ifPresent(audioOutput -> {
                    System.out.printf("sink with name %s and index %s is active\n", audioOutput.name(), audioOutput.index());
                    AudioOutput newSink = outputs.get((outputs.indexOf(audioOutput) + 1) % outputs.size());
                    System.out.printf("switching to sink with name %s and index %s\n", newSink.name(), newSink.index());
                    ProcessBuilder pacmd1 = new ProcessBuilder("pacmd", "set-default-sink", newSink.index());
                    try {
                        pacmd1.start();
                        JFrame frame = new JFrame();
                        frame.setSize(300, 150);
                        Rectangle bounds = frame.getGraphicsConfiguration().getBounds();
                        frame.setLocation(bounds.width - 400, 100);
                        frame.setUndecorated(true);
                        frame.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
                        frame.setLayout(new GridBagLayout());
                        JTextArea jtextArea = new JTextArea(newSink.name().substring(1,newSink.name().length()-1));
                        jtextArea.setEditable(false);
                        jtextArea.setVisible(true);
                        jtextArea.setBackground(new Color(0,0,0,0));
                        frame.add(jtextArea);
                        frame.setVisible(true);
                        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                            SwingUtilities.invokeLater(frame::dispose);
                            System.exit(0);
                        },1000, TimeUnit.MILLISECONDS);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

