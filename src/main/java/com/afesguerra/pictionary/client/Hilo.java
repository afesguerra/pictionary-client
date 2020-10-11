package com.afesguerra.pictionary.client;

import com.afesguerra.pictionary.client.gui.GameFrame;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

@Log4j2
public class Hilo extends Thread {
    private static final String MESSAGE_SEPARATOR = ";";
    private final GameFrame window; // Ventana en la cual se juega
    private final DatagramSocket sck; // Socket por el cual se recibe información

    public Hilo(GameFrame window, DatagramSocket sck) {
        this.window = window;
        this.sck = sck;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                final byte[] a = new byte[10000];
                final DatagramPacket pck = new DatagramPacket(a, a.length);
                sck.receive(pck);
                parseMessage(pck);
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    private void parseMessage(DatagramPacket pck) {
        final String message = new String(pck.getData(), 0, pck.getLength());
        String[] msj = message.split(MESSAGE_SEPARATOR);

        switch (Integer.parseInt(msj[0])) {
            case 0:
                // Alguien ganó
                JOptionPane.showMessageDialog(window, msj[2], "¡¡GANADOR!!",
                        JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
                break;
            case 1:
                // coordenadas
                int x = Integer.parseInt(msj[1]);
                int y = Integer.parseInt(msj[2]);
                window.dibujar(x, y);
                break;
            case 2:
                // palabra+
                window.escribir((msj[1] + ": " + msj[2]));
                break;
            case 3:
                // alguien se desconecto
                JOptionPane.showMessageDialog(window, msj[2], "Error de conexión", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                break;
            case 4:
                final Color color = new Color(Integer.parseInt(msj[1]), Integer.parseInt(msj[2]), Integer.parseInt(msj[3]));
                window.cambiarColor(color);
                break;
            default:
                log.warn("Received unexpected message: {}", message);
                break;
        }
    }
}
