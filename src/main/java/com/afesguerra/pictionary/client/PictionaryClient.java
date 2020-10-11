package com.afesguerra.pictionary.client;

import com.afesguerra.pictionary.client.gui.GameStartFrame;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramSocket;
import java.net.SocketException;

@Log4j2
public class PictionaryClient {
    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(PictionaryClient::createAndShowGUI);
    }

    private static void createAndShowGUI()  {
        try {
            final DatagramSocket sck = new DatagramSocket();
            final GameStartFrame gameStartFrame = new GameStartFrame(sck);
            gameStartFrame.setVisible(true);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
}
