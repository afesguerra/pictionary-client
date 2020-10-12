package com.afesguerra.pictionary.client;

import com.afesguerra.pictionary.client.gui.GameStartFrame;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.DatagramChannel;

@Log4j2
public class PictionaryClient {
    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(PictionaryClient::createAndShowGUI);
    }

    private static void createAndShowGUI()  {
        try {
            final DatagramChannel channel = DatagramChannel.open();
            final GameStartFrame gameStartFrame = new GameStartFrame(channel);
            gameStartFrame.setVisible(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
